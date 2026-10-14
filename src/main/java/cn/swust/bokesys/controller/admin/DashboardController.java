package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.DashboardService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.DashboardVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 后台仪表盘，对应 API-11 {@code GET /admin/dashboard}（Fun_08 仪表盘管理）。
 *
 * <p>对应《概要设计》6.2.3：四张统计卡片（文章总数 / 评论总数 / 分类数 / 标签数）、
 * 待审核评论提醒条、文章发布趋势图与最近发布的文章列表。
 */
@Controller
public class DashboardController extends AdminBaseController {

    private final DashboardService dashboardService;

    public DashboardController(ConfigService configService,
                               CategoryService categoryService,
                               TagService tagService,
                               LinkService linkService,
                               DashboardService dashboardService) {
        super(configService, categoryService, tagService, linkService);
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        DashboardVO stats = dashboardService.stats();
        model.addAttribute("stats", stats);
        // 供侧边导航「评论管理」右侧的待审核角标使用
        model.addAttribute("pendingCommentCount", stats.getPendingCommentTotal());
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("pageTitle", "仪表盘");
        return "admin/dashboard";
    }
}
