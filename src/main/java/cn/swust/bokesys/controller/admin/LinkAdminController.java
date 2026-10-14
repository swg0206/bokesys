package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.po.Link;
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
 * 友情链接管理，对应 API-20（Fun_13 友情链接管理）。
 *
 * <p>修改后前台侧边栏的友链区块会随之变化（前台每次渲染都从库里读）。
 */
@Controller
@RequestMapping("/admin/links")
public class LinkAdminController extends AdminBaseController {

    private final LinkService linkService;

    public LinkAdminController(ConfigService configService,
                               CategoryService categoryService,
                               TagService tagService,
                               LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
        this.linkService = linkService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("links", linkService.listAll());
        model.addAttribute("activeMenu", "link");
        model.addAttribute("pageTitle", "友情链接");
        return "admin/link-list";
    }

    @PostMapping
    @ResponseBody
    public Result<Void> create(Link link) {
        linkService.create(link);
        return Result.ok("友链已添加", null);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Result<Void> update(@PathVariable("id") Long id, Link link) {
        link.setId(id);
        linkService.update(link);
        return Result.ok("友链已更新", null);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<Void> delete(@PathVariable("id") Long id) {
        linkService.delete(id);
        return Result.ok("友链已删除", null);
    }
}
