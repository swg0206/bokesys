package cn.swust.bokesys.mapper;

import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.po.Article;
import cn.swust.bokesys.vo.ArticleVO;
import cn.swust.bokesys.vo.CategoryStatVO;
import cn.swust.bokesys.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章表 {@code t_article} 的数据访问接口。
 *
 * <p>覆盖《概要设计》3.4 中列出的职责：增删改查、分页查询、标题与正文模糊匹配、
 * 阅读量与评论数自增、草稿与已发布状态筛选。
 */
@Mapper
public interface ArticleMapper {

    /**
     * 前台文章分页：只取已发布（status=1），按创建时间倒序。
     * 对应接口 API-01 / API-03 / API-04 / API-05。
     */
    List<ArticleVO> selectFrontPage(ArticleQueryDTO query);

    /**
     * 后台文章分页：不限状态，支持标题、分类、状态组合筛选。
     * 对应接口 API-12。
     */
    List<ArticleVO> selectAdminPage(ArticleQueryDTO query);

    /** 文章详情（含分类名与标签集合） */
    ArticleVO selectVoById(@Param("id") Long id);

    Article selectById(@Param("id") Long id);

    int insert(Article article);

    int update(Article article);

    int deleteById(@Param("id") Long id);

    int deleteBatch(@Param("ids") List<Long> ids);

    /** 阅读量自增，对应接口 API-02「阅读量自动加一」 */
    int incrementViewCount(@Param("id") Long id);

    /** 评论数 +1，评论审核通过时调用 */
    int incrementCommentCount(@Param("id") Long id);

    /** 评论数 -1（不小于 0），删除已通过评论时调用 */
    int decrementCommentCount(@Param("id") Long id);

    /** 按分类统计文章数，供分类删除前校验 */
    long countByCategoryId(@Param("categoryId") Long categoryId);

    /** 统计命中某标签的已发布文章数 */
    long countByTagId(@Param("tagId") Long tagId);

    long countAll();

    long countByStatus(@Param("status") Integer status);

    /** 累计浏览量 */
    long sumViewCount();

    /** 最近发布的文章，仪表盘「最近发布」列表用 */
    List<Article> selectRecent(@Param("limit") int limit);

    /** 上一篇（发布时间更早的最近一篇已发布文章） */
    Article selectPrev(@Param("id") Long id, @Param("createTime") LocalDateTime createTime);

    /** 下一篇（发布时间更晚的最近一篇已发布文章） */
    Article selectNext(@Param("id") Long id, @Param("createTime") LocalDateTime createTime);

    /** 按天统计文章发布量（自 startTime 起），仪表盘趋势图用 */
    List<TrendPointVO> selectTrendSince(@Param("startTime") LocalDateTime startTime);

    /** 各分类文章分布（含无文章的分类） */
    List<CategoryStatVO> selectCategoryStats();
}
