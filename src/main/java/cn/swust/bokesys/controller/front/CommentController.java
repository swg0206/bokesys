package cn.swust.bokesys.controller.front;

import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.dto.CommentDTO;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.CommentService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.CommentVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论接口，对应 API-06 {@code GET /api/comments} 与 API-07 {@code POST /api/comments}
 * （Fun_05 发表评论）。
 *
 * <p>两个接口都返回 {@code Result} JSON，供文章详情页用 fetch 做局部刷新
 * （对应《概要设计》3.1 表现层「通过 Ajax / fetch 发起异步请求」）。
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController extends BaseController {

    private final CommentService commentService;

    public CommentController(ConfigService configService,
                             CategoryService categoryService,
                             TagService tagService,
                             LinkService linkService,
                             CommentService commentService) {
        super(configService, categoryService, tagService, linkService);
        this.commentService = commentService;
    }

    /**
     * 查询某篇文章下的评论楼。
     *
     * <p>只返回已通过审核的评论 —— 待审核的评论即便已经被提交，
     * 在这里也是查不到的。
     */
    @GetMapping
    public Result<List<CommentVO>> list(@RequestParam("articleId") Long articleId) {
        return Result.ok(commentService.listApprovedTree(articleId));
    }

    /**
     * 提交评论。
     *
     * <p>落库状态一律为「待审核」，因此返回的提示是「等待审核」而不是「已发布」。
     */
    @PostMapping
    public Result<Void> submit(@Valid CommentDTO dto, HttpSession session) {
        User user = currentUser(session);
        commentService.submit(dto, user);
        return Result.ok("评论提交成功，等待管理员审核通过后展示", null);
    }
}
