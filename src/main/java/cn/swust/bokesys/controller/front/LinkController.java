package cn.swust.bokesys.controller.front;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.po.Link;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台友情链接接口，对应 API-08 {@code GET /api/links}（Fun_06）。
 *
 * <p>只返回 status=1 的链接，按 sort_order 升序。
 */
@RestController
@RequestMapping("/api/links")
public class LinkController extends BaseController {

    private final LinkService linkService;

    public LinkController(ConfigService configService,
                          CategoryService categoryService,
                          TagService tagService,
                          LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
        this.linkService = linkService;
    }

    @GetMapping
    public Result<List<Link>> list() {
        return Result.ok(linkService.listVisible());
    }
}
