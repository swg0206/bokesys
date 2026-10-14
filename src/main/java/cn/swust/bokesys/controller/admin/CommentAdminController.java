package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.constant.CommentStatus;
import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.CommentService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.vo.CommentVO;
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
 * 评论管理，对应 API-18（Fun_11 评论管理）。
 *
 * <p>核心动作是「审核通过」：通过后评论才会出现在前台，
 * 并且文章的评论数会在同一事务里 +1。
 */
@Controller
@RequestMapping("/admin/comments")
public class CommentAdminController extends AdminBaseController {

    private final CommentService commentService;

    public CommentAdminController(ConfigService configService,
                                  CategoryService categoryService,
                                  TagService tagService,
                                  LinkService linkService,
                                  CommentService commentService) {
        super(configService, categoryService, tagService, linkService);
        this.commentService = commentService;
    }

    /** 评论分页列表，可按状态（全部 / 待审核 / 已通过 / 已拒绝）与关键词筛选 */
    @GetMapping
    public String list(@RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "articleId", required = false) Long articleId,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "pageNo", required = false) Integer pageNo,
                       Model model) {
        PageResult<CommentVO> page = commentService.pageAdmin(
                status, articleId, keyword, pageNo(pageNo), 10);

        model.addAttribute("page", page);
        model.addAttribute("pageBase", pageBase("/admin/comments",
                "status", status, "articleId", articleId, "keyword", keyword));
        model.addAttribute("status", status);
        model.addAttribute("articleId", articleId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingTotal", commentService.countPending());
        // 供侧边导航「评论管理」右侧的待审核角标使用
        model.addAttribute("pendingCommentCount", commentService.countPending());
        model.addAttribute("statusOptions", CommentStatus.values());
        model.addAttribute("activeMenu", "comment");
        model.addAttribute("pageTitle", "评论管理");
        return "admin/comment-list";
    }

    /** 审核通过 */
    @PutMapping("/{id}/approve")
    @ResponseBody
    public Result<Void> approve(@PathVariable("id") Long id) {
        commentService.approve(id);
        return Result.ok("已通过审核，评论已在前台展示", null);
    }

    /** 审核拒绝 */
    @PutMapping("/{id}/reject")
    @ResponseBody
    public Result<Void> reject(@PathVariable("id") Long id) {
        commentService.reject(id);
        return Result.ok("已拒绝该评论", null);
    }

    /** 批量通过审核 */
    @PostMapping("/batch-approve")
    @ResponseBody
    public Result<Void> batchApprove(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            commentService.approve(id);
        }
        return Result.ok("已通过 " + ids.size() + " 条评论", null);
    }

    /** 删除评论（连同其回复） */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(id);
        return Result.ok("评论已删除", null);
    }
}
