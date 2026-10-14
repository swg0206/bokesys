package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.po.Tag;
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
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 标签管理，对应 API-19（Fun_12 标签管理）。
 *
 * <p>列表展示标签名称与其被引用的文章数量；删除标签会连带清理关联关系。
 */
@Controller
@RequestMapping("/admin/tags")
public class TagController extends AdminBaseController {

    private final TagService tagService;

    public TagController(ConfigService configService,
                         CategoryService categoryService,
                         TagService tagService,
                         LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
        this.tagService = tagService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tags", tagService.listAllWithCount());
        model.addAttribute("activeMenu", "tag");
        model.addAttribute("pageTitle", "标签管理");
        return "admin/tag-list";
    }

    @PostMapping
    @ResponseBody
    public Result<Void> create(Tag tag) {
        tagService.create(tag);
        return Result.ok("标签已添加", null);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Result<Void> update(@PathVariable("id") Long id, Tag tag) {
        tag.setId(id);
        tagService.update(tag);
        return Result.ok("标签已更新", null);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<Void> delete(@PathVariable("id") Long id) {
        tagService.delete(id);
        return Result.ok("标签已删除", null);
    }
}
