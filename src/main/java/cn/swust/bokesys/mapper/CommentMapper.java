package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.Comment;
import cn.swust.bokesys.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论表 {@code t_comment} 的数据访问接口。
 */
@Mapper
public interface CommentMapper {

    /** 某篇文章下已通过审核的评论（含回复），前台展示用 */
    List<Comment> selectApprovedByArticleId(@Param("articleId") Long articleId);

    /**
     * 后台评论分页：可按状态、文章、关键词筛选，附带文章标题。
     * 对应接口 API-18。
     */
    List<CommentVO> selectAdminPage(@Param("status") Integer status,
                                    @Param("articleId") Long articleId,
                                    @Param("keyword") String keyword);

    Comment selectById(@Param("id") Long id);

    List<Comment> selectByParentId(@Param("parentId") Long parentId);

    int insert(Comment comment);

    /** 审核：更新单条评论状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int insertBatch(@Param("list") List<Comment> list);

    int deleteById(@Param("id") Long id);

    int deleteBatch(@Param("ids") List<Long> ids);

    /** 按父评论删除回复，删除评论时级联清理其子评论 */
    int deleteByParentId(@Param("parentId") Long parentId);

    /** 按文章删除全部评论，删除文章时调用 */
    int deleteByArticleId(@Param("articleId") Long articleId);

    /**
     * 把某篇文章下所有评论的 parent_id 置空。
     *
     * <p>t_comment 的 parent_id 是自关联外键，若直接 {@code DELETE ... WHERE article_id = ?}
     * 一条语句删掉整篇文章的评论，数据库会在删父评论时发现子评论仍引用它而报外键冲突
     * （H2 与 MySQL 的外键都是即时校验）。先把整棵森林的父子指针解开，
     * 再整体删除就不会触发约束。仅在「整篇文章的评论全部要删」时使用。
     */
    int clearParentIdByArticleId(@Param("articleId") Long articleId);

    long countAll();

    long countByStatus(@Param("status") Integer status);

    long countByArticleId(@Param("articleId") Long articleId);
}
