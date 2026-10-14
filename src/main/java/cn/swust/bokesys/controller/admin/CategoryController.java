package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.po.Category;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 分类管理，对应 API-17（Fun_10 分类管理）。
 *
 * <p>列表页展示分类名称、描述、排序号与该分类下的文章数量；
 * 删除前由 CategoryService 校验占用，占用时返回可读的提示。
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryController extends AdminBaseController {

    private final CategoryService categoryService;

    public CategoryController(ConfigService configService,
                              CategoryService categoryService,
                              TagService tagService,
                              LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.listAllWithCount());
        model.addAttribute("activeMenu", "category");
        model.addAttribute("pageTitle", "分类管理");
        return "admin/category-list";
    }

    @PostMapping
    @ResponseBody
    public Result<Void> create(Category category) {
        categoryService.create(category);
        return Result.ok("分类已添加", null);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Result<Void> update(@PathVariable("id") Long id, Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.ok("分类已更新", null);
    }

    /** 删除：分类下还有文章时，Service 会抛出业务异常，由全局处理器转成提示 */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<Void> delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
        return Result.ok("分类已删除", null);
    }
}
