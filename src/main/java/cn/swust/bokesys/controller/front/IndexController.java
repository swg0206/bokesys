package cn.swust.bokesys.controller.front;

import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.service.ArticleService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.ArticleVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 前台首页，对应接口 API-01（Fun_01 文章浏览）。
 *
 * <p>对应《概要设计》6.1：按发布时间倒序展示已发布文章，支持分页浏览。
 */
@Controller
public class IndexController extends BaseController {

    /** 配置项中读取前台每页文章数 */
    private static final String CFG_PAGE_SIZE = "page_size";
    private static final int DEFAULT_PAGE_SIZE = 6;

    private final ArticleService articleService;

    public IndexController(ConfigService configService,
                           CategoryService categoryService,
                           TagService tagService,
                           LinkService linkService,
                           ArticleService articleService) {
        super(configService, categoryService, tagService, linkService);
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                        Model model) {
        int pageSize = Integer.parseInt(
                configService.get(CFG_PAGE_SIZE, String.valueOf(DEFAULT_PAGE_SIZE)));

        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setPageNo(pageNo(pageNo));
        query.setPageSize(pageSize);

        PageResult<ArticleVO> page = articleService.pageFront(query);

        model.addAttribute("page", page);
        // 分页链接前缀（不含 pageNo），由页面原样拼给分页片段
        model.addAttribute("pageBase", "/?");
        model.addAttribute("activeNav", "home");
        model.addAttribute("pageTitle", null);
        fillFrontCommonModel(model);
        return "front/index";
    }
}
