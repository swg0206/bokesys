package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统管理，对应 API-21（Fun_14 系统管理）。
 *
 * <p>站点名称、站点描述、备案信息等参数持久化在 t_config 表，
 * 保存后立即生效、无需重启（ConfigService 内部有缓存，保存时自动失效）。
 *
 * <p>这里显式列出可配置的键，而不是把请求参数整体倒进数据库 ——
 * 后者会让任何客户端都能凭空写入任意配置项。
 */
@Controller
@RequestMapping("/admin/config")
public class ConfigController extends AdminBaseController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService,
                            CategoryService categoryService,
                            TagService tagService,
                            LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
        this.configService = configService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("configs", configService.listAll());
        model.addAttribute("site", configService.loadAllAsMap());
        model.addAttribute("activeMenu", "config");
        model.addAttribute("pageTitle", "系统管理");
        return "admin/config";
    }

    @PutMapping
    @ResponseBody
    public Result<Void> save(@RequestParam(value = "siteName", required = false) String siteName,
                             @RequestParam(value = "siteDescription", required = false) String siteDescription,
                             @RequestParam(value = "siteKeywords", required = false) String siteKeywords,
                             @RequestParam(value = "icpNo", required = false) String icpNo,
                             @RequestParam(value = "pageSize", required = false) String pageSize,
                             @RequestParam(value = "authorName", required = false) String authorName) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("site_name", siteName);
        values.put("site_description", siteDescription);
        values.put("site_keywords", siteKeywords);
        values.put("icp_no", icpNo);
        values.put("page_size", normalizePageSize(pageSize));
        values.put("author_name", authorName);

        // 只保存真正传了值的项，避免把未提交的配置清空
        values.entrySet().removeIf(e -> e.getValue() == null);
        configService.save(values);
        return Result.ok("配置已保存", null);
    }

    /** 每页文章数需要落一个合法整数，否则前台分页会算出异常页数 */
    private String normalizePageSize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1) {
                value = 1;
            }
            if (value > 50) {
                value = 50;
            }
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
