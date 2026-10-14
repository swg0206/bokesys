package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.constant.CommentStatus;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.common.util.PageUtil;
import cn.swust.bokesys.common.util.SensitiveWordUtil;
import cn.swust.bokesys.dto.CommentDTO;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.CommentMapper;
import cn.swust.bokesys.po.Article;
import cn.swust.bokesys.po.Comment;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.CommentService;
import cn.swust.bokesys.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论业务实现，对应《概要设计》4.2.2「评论互动模块」。
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;

    public CommentServiceImpl(CommentMapper commentMapper, ArticleMapper articleMapper) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
    }

    /**
     * 组装评论楼。
     *
     * <p>做法是「一次查全量，再在内存里按 parentId 建索引挂子树」，
     * 而不是递归查库 —— 后者在评论稍多时会产生大量往返，也容易写出无限递归。
     */
    @Override
    public List<CommentVO> listApprovedTree(Long articleId) {
        List<Comment> flat = commentMapper.selectApprovedByArticleId(articleId);
        if (flat.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CommentVO> index = new LinkedHashMap<>();
        for (Comment c : flat) {
            index.put(c.getId(), toVO(c));
        }

        List<CommentVO> roots = new ArrayList<>();
        for (Comment c : flat) {
            CommentVO vo = index.get(c.getId());
            Long parentId = c.getParentId();
            if (parentId == null) {
                roots.add(vo);
                continue;
            }
            CommentVO parent = index.get(parentId);
            if (parent == null) {
                // 父评论未通过审核（因此不在结果集中）时，把回复提升为顶层，
                // 避免这条回复因为父节点缺失而彻底消失
                roots.add(vo);
                continue;
            }
            vo.setReplyToNickname(parent.getNickname());
            parent.getChildren().add(vo);
        }
        return roots;
    }

    @Override
    public PageResult<CommentVO> pageAdmin(Integer status, Long articleId, String keyword,
                                           Integer pageNo, Integer pageSize) {
        PageUtil.startPage(pageNo, pageSize);
        List<CommentVO> list = commentMapper.selectAdminPage(status, articleId, keyword);
        return PageUtil.toResult(list);
    }

    /**
     * 提交评论。
     *
     * <p>三条规则按顺序执行：
     * <ol>
     *   <li>必须登录（游客只能看，不能发）</li>
     *   <li>内容过敏感词过滤，命中直接拒绝</li>
     *   <li>落库状态一律为「待审核」，前台查不出来</li>
     * </ol>
     */
    @Override
    @Transactional
    public void submit(CommentDTO dto, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "请先登录后再发表评论");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("评论内容不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        String hit = SensitiveWordUtil.firstHit(content);
        if (hit != null) {
            throw new BusinessException("评论包含违规词「" + hit + "」，请修改后再提交");
        }

        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        if (!ArticleStatus.PUBLISHED.matches(article.getStatus())) {
            throw new BusinessException("该文章尚未发布，无法评论");
        }

        // 回复场景：父评论必须存在且属于同一篇文章，防止跨文章挂靠
        if (dto.getParentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("被回复的评论不存在或已被删除");
            }
            if (!parent.getArticleId().equals(dto.getArticleId())) {
                throw new BusinessException("被回复的评论不属于当前文章");
            }
        }

        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(dto.getParentId());
        comment.setUserId(loginUser.getId());
        // 昵称优先取用户填写的，其次取注册账号的昵称/用户名
        String nickname = dto.getNickname() == null ? "" : dto.getNickname().trim();
        comment.setNickname(nickname.isEmpty() ? loginUser.getDisplayName() : nickname);
        comment.setEmail(dto.getEmail() == null || dto.getEmail().trim().isEmpty()
                ? loginUser.getEmail() : dto.getEmail().trim());
        comment.setContent(content);
        comment.setStatus(CommentStatus.PENDING.getCode());
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);
    }

    /**
     * 审核通过。
     *
     * <p>「改状态」与「回写文章评论数」必须在同一事务内，
     * 否则可能出现评论已展示但计数没有增加的中间态。
     * 重复审核做了幂等处理，避免把计数加两次。
     */
    @Override
    @Transactional
    public void approve(Long id) {
        Comment comment = requireComment(id);
        if (CommentStatus.APPROVED.matches(comment.getStatus())) {
            return;
        }
        commentMapper.updateStatus(id, CommentStatus.APPROVED.getCode());
        articleMapper.incrementCommentCount(comment.getArticleId());
    }

    @Override
    @Transactional
    public void reject(Long id) {
        Comment comment = requireComment(id);
        if (CommentStatus.REJECTED.matches(comment.getStatus())) {
            return;
        }
        // 若此前是已通过状态，改为拒绝时要把它从文章评论数里扣掉
        if (CommentStatus.APPROVED.matches(comment.getStatus())) {
            articleMapper.decrementCommentCount(comment.getArticleId());
        }
        commentMapper.updateStatus(id, CommentStatus.REJECTED.getCode());
    }

    /**
     * 删除评论。
     *
     * <p>t_comment 的 parent_id 是自关联外键，删父节点前必须先清掉整棵子树的叶子。
     * 因此这里先按层收集整棵子树，再从最深一层往上逐层删除；
     * 最后把其中已通过的条数从文章评论数里扣回。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Comment root = requireComment(id);

        List<List<Comment>> levels = new ArrayList<>();
        List<Comment> current = List.of(root);
        levels.add(current);
        while (true) {
            List<Comment> next = new ArrayList<>();
            for (Comment c : current) {
                next.addAll(commentMapper.selectByParentId(c.getId()));
            }
            if (next.isEmpty()) {
                break;
            }
            levels.add(next);
            current = next;
        }

        long approvedRemoved = 0;
        for (int i = levels.size() - 1; i >= 0; i--) {
            List<Comment> level = levels.get(i);
            List<Long> ids = new ArrayList<>();
            for (Comment c : level) {
                ids.add(c.getId());
                if (CommentStatus.APPROVED.matches(c.getStatus())) {
                    approvedRemoved++;
                }
            }
            if (!ids.isEmpty()) {
                commentMapper.deleteBatch(ids);
            }
        }

        for (long i = 0; i < approvedRemoved; i++) {
            articleMapper.decrementCommentCount(root.getArticleId());
        }
    }

    @Override
    public long countAll() {
        return commentMapper.countAll();
    }

    @Override
    public long countPending() {
        return commentMapper.countByStatus(CommentStatus.PENDING.getCode());
    }

    // ── 内部工具 ────────────────────────────────────────────────────────

    private Comment requireComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在或已被删除");
        }
        return comment;
    }

    private CommentVO toVO(Comment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setArticleId(c.getArticleId());
        vo.setParentId(c.getParentId());
        vo.setUserId(c.getUserId());
        vo.setNickname(c.getNickname());
        vo.setEmail(c.getEmail());
        vo.setContent(c.getContent());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
