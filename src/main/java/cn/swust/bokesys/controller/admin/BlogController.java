package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.dto.ArticleDTO;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.service.ArticleService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.ArticleVO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 博客管理，对应《概要设计》3.2 与接口 API-12 ~ API-16（Fun_09 博客管理）。
 *
 * <p>约定：GET 返回 HTML 页面（列表页与编辑器页服务端渲染），
 * 增删改返回 {@code Result} JSON —— 与文档接口表中这一组的返回值一致，
 * 前端用 fetch 提交后局部刷新，对应 6.2.4 描述的交互。
 *
 * <p>PUT / DELETE 同时接受 POST：HTML 表单本身只能发 GET/POST，
 * 表单场景靠隐藏域 {@code _method} 配合 Spring 的 HiddenHttpMethodFilter 转换
 * （已在 application.yml 中开启 spring.mvc.hiddenmethod.filter.enabled）。
 * 同时映射 POST 是为了让不带该隐藏域的请求也能工作，避免 405。
 */
@Controller
@RequestMapping("/admin/articles")
public class BlogController extends AdminBaseController {

    private final ArticleService articleService;

    public BlogController(ConfigService configService,
                          CategoryService categoryService,
                          TagService tagService,
                          LinkService linkService,
                          ArticleService articleService) {
        super(configService, categoryService, tagService, linkService);
        this.articleService = articleService;
    }

    // ── 列表页 ──────────────────────────────────────────────────────────

    /** 后台文章分页列表，支持按标题、分类、状态筛选 */
    @GetMapping
    public String list(@RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "pageNo", required = false) Integer pageNo,
                       Model model) {
        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setTitle(title);
        query.setCategoryId(categoryId);
        query.setStatus(status);
        query.setPageNo(pageNo(pageNo));
        query.setPageSize(10);

        PageResult<ArticleVO> page = articleService.pageAdmin(query);

        model.addAttribute("page", page);
        model.addAttribute("pageBase", pageBase("/admin/articles",
                "title", title, "categoryId", categoryId, "status", status));
        model.addAttribute("categories", categoryService.listAllWithCount());
        model.addAttribute("title", title);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("activeMenu", "article");
        model.addAttribute("pageTitle", "博客管理");
        return "admin/article-list";
    }

    // ── 编辑器页 ────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("article", null);
        prepareEditorModel(model, "发布文章");
        return "admin/article-edit";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable("id") Long id, Model model) {
        ArticleVO article = articleService.getVoById(id);
        model.addAttribute("article", article);
        prepareEditorModel(model, "编辑文章");
        return "admin/article-edit";
    }

    // ── 写操作（返回 JSON）───────────────────────────────────────────────

    /** API-13：保存并发布 */
    @PostMapping
    @ResponseBody
    public Result<Long> create(@Valid ArticleDTO dto) {
        dto.setStatus(ArticleStatus.PUBLISHED.getCode());
        Long id = articleService.create(dto);
        return Result.ok("发布成功", id);
    }

    /** API-16：保存为草稿 */
    @PostMapping("/draft")
    @ResponseBody
    public Result<Long> saveDraft(@Valid ArticleDTO dto) {
        dto.setStatus(ArticleStatus.DRAFT.getCode());
        Long id = articleService.create(dto);
        return Result.ok("草稿已保存", id);
    }

    /**
     * API-14：更新文章。
     *
     * <p>表单场景下浏览器只能发 POST，由隐藏域 {@code _method=PUT} 经
     * HiddenHttpMethodFilter 转成真正的 PUT 后再路由到这里；
     * fetch 场景则可以直接发 PUT。
     */
    @PutMapping("/{id}")
    @ResponseBody
    public Result<Void> update(@PathVariable("id") Long id, @Valid ArticleDTO dto,
                               @RequestParam(value = "action", required = false) String action) {
        dto.setId(id);
        // 编辑器里的「存草稿」按钮复用同一个地址，通过 action 区分最终状态
        if ("draft".equalsIgnoreCase(action)) {
            dto.setStatus(ArticleStatus.DRAFT.getCode());
        } else if (dto.getStatus() == null) {
            dto.setStatus(ArticleStatus.PUBLISHED.getCode());
        }
        articleService.update(dto);
        return Result.ok(ArticleStatus.DRAFT.matches(dto.getStatus())
                ? "草稿已保存" : "更新成功", null);
    }

    /** API-15：删除文章（前端二次确认后调用） */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<Void> delete(@PathVariable("id") Long id) {
        articleService.delete(id);
        return Result.ok("删除成功", null);
    }

    /** 批量删除：请求体为文章 ID 数组 */
    @PostMapping("/batch-delete")
    @ResponseBody
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        articleService.deleteBatch(ids);
        return Result.ok("已删除 " + ids.size() + " 篇文章", null);
    }

    // ── 内部辅助 ────────────────────────────────────────────────────────

    private void prepareEditorModel(Model model, String title) {
        model.addAttribute("categories", categoryService.listAllWithCount());
        model.addAttribute("allTags", tagService.listAllWithCount());
        model.addAttribute("activeMenu", "article");
        model.addAttribute("pageTitle", title);
    }
}
