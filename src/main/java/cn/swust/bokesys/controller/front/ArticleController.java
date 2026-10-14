package cn.swust.bokesys.controller.front;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.po.Category;
import cn.swust.bokesys.po.Tag;
import cn.swust.bokesys.service.ArticleService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.CommentService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.ArticleVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台文章相关页面，对应接口：
 * API-02 {@code /article/{id}}（文章详情，阅读量自增）、
 * API-03 {@code /search}（关键词搜索）、
 * API-04 {@code /category/{categoryId}}（分类导航）、
 * API-05 {@code /tag/{tagId}}（标签筛选），
 * 外加导航栏需要但接口清单未列的 {@code /archives}（归档）。
 */
@Controller
public class ArticleController extends BaseController {

    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final String CFG_PAGE_SIZE = "page_size";
    /** 归档页一次取回的已发布文章上限 */
    private static final int ARCHIVE_LIMIT = 100;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy 年 M 月");

    private final ArticleService articleService;
    private final CommentService commentService;

    public ArticleController(ConfigService configService,
                             CategoryService categoryService,
                             TagService tagService,
                             LinkService linkService,
                             ArticleService articleService,
                             CommentService commentService) {
        super(configService, categoryService, tagService, linkService);
        this.articleService = articleService;
        this.commentService = commentService;
    }

    /** 文章详情：阅读量自增，并带上评论楼 */
    @GetMapping("/article/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        ArticleVO article = articleService.viewDetail(id);
        List<cn.swust.bokesys.vo.CommentVO> comments = commentService.listApprovedTree(id);

        model.addAttribute("article", article);
        model.addAttribute("comments", comments);
        model.addAttribute("commentTotal", countComments(comments));
        model.addAttribute("prevArticle", articleService.getPrev(id));
        model.addAttribute("nextArticle", articleService.getNext(id));
        model.addAttribute("activeNav", "home");
        model.addAttribute("pageTitle", article.getTitle());
        fillFrontCommonModel(model);
        return "front/article";
    }

    /** 关键词搜索：对标题与正文做模糊匹配 */
    @GetMapping("/search")
    public String search(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "pageNo", required = false) Integer pageNo,
                         Model model) {
        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setKeyword(keyword);
        query.setPageNo(pageNo(pageNo));
        query.setPageSize(pageSize());

        PageResult<ArticleVO> page = articleService.pageFront(query);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("pageBase", searchPageBase(keyword));
        model.addAttribute("activeNav", "home");
        model.addAttribute("pageTitle", "搜索：" + (keyword == null ? "" : keyword));
        fillFrontCommonModel(model);
        return "front/search";
    }

    /** 分类导航 */
    @GetMapping("/category/{categoryId}")
    public String byCategory(@PathVariable("categoryId") Long categoryId,
                             @RequestParam(value = "pageNo", required = false) Integer pageNo,
                             Model model) {
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在或已被删除");
        }
        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setCategoryId(categoryId);
        query.setPageNo(pageNo(pageNo));
        query.setPageSize(pageSize());

        model.addAttribute("page", articleService.pageFront(query));
        model.addAttribute("category", category);
        model.addAttribute("pageBase", "/category/" + categoryId + "?");
        model.addAttribute("activeNav", "category");
        model.addAttribute("pageTitle", "分类：" + category.getName());
        fillFrontCommonModel(model);
        return "front/category";
    }

    /** 标签筛选 */
    @GetMapping("/tag/{tagId}")
    public String byTag(@PathVariable("tagId") Long tagId,
                        @RequestParam(value = "pageNo", required = false) Integer pageNo,
                        Model model) {
        Tag tag = tagService.getById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在或已被删除");
        }
        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setTagId(tagId);
        query.setPageNo(pageNo(pageNo));
        query.setPageSize(pageSize());

        model.addAttribute("page", articleService.pageFront(query));
        model.addAttribute("tag", tag);
        model.addAttribute("pageBase", "/tag/" + tagId + "?");
        model.addAttribute("activeNav", "tag");
        model.addAttribute("pageTitle", "标签：" + tag.getName());
        fillFrontCommonModel(model);
        return "front/tag";
    }

    /**
     * 归档：按年月分组展示已发布文章。
     *
     * <p>导航栏「归档」入口需要它，但《概要设计》的接口清单里没有单列，
     * 因此复用「按状态分页」的现成能力，在服务端完成分组。
     */
    @GetMapping("/archives")
    public String archives(Model model) {
        List<ArticleVO> all = articleService
                .pageByStatus(ArticleStatus.PUBLISHED.getCode(), 1, ARCHIVE_LIMIT)
                .getList();

        Map<String, List<ArticleVO>> grouped = new LinkedHashMap<>();
        for (ArticleVO article : all) {
            if (article.getCreateTime() == null) {
                continue;
            }
            String key = YearMonth.from(article.getCreateTime()).format(MONTH_FORMAT);
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(article);
        }

        model.addAttribute("archiveGroups", grouped);
        model.addAttribute("total", all.size());
        model.addAttribute("activeNav", "archive");
        model.addAttribute("pageTitle", "归档");
        fillFrontCommonModel(model);
        return "front/archives";
    }

    private int pageSize() {
        return Integer.parseInt(configService.get(CFG_PAGE_SIZE, String.valueOf(DEFAULT_PAGE_SIZE)));
    }

    /**
     * 搜索结果页的分页链接前缀。
     *
     * <p>关键词必须做 URL 编码，否则含空格或 & 的搜索词会把分页链接拼坏。
     */
    private String searchPageBase(String keyword) {
        String encoded = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
        return "/search?keyword=" + encoded + "&";
    }

    /** 递归统计评论楼里的总条数（根节点 + 各层回复） */
    private int countComments(List<cn.swust.bokesys.vo.CommentVO> tree) {
        int total = 0;
        for (cn.swust.bokesys.vo.CommentVO node : tree) {
            total += 1 + countComments(node.getChildren());
        }
        return total;
    }
}
