package cn.swust.bokesys.service;

import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.dto.CommentDTO;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.vo.CommentVO;

import java.util.List;

/**
 * 评论业务接口，对应《概要设计》3.3：
 * 评论提交（置为待审核）、敏感词过滤、按文章查询已审核评论、评论树组装、
 * 审核通过、删除；审核通过后回写文章评论数。
 */
public interface CommentService {

    /**
     * 某篇文章下的评论楼（树形）。
     *
     * <p>只包含已通过审核的评论 —— 这是「先审核后展示」在前台的落点。
     */
    List<CommentVO> listApprovedTree(Long articleId);

    /** 后台评论分页，可按状态 / 文章 / 关键词筛选 */
    PageResult<CommentVO> pageAdmin(Integer status, Long articleId, String keyword,
                                    Integer pageNo, Integer pageSize);

    /**
     * 提交评论。
     *
     * <p>规则：必须登录；内容先过敏感词过滤；落库一律为「待审核」状态。
     */
    void submit(CommentDTO dto, User loginUser);

    /** 审核通过：置为已通过，并在同一事务内回写文章评论数 */
    void approve(Long id);

    /** 审核拒绝：置为已拒绝，若此前已通过则回退文章评论数 */
    void reject(Long id);

    /** 删除评论：连同其所有回复一起删除，并纠正文章评论数 */
    void delete(Long id);

    long countAll();

    long countPending();
}
