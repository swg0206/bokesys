package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.ArticleTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章-标签关联表 {@code t_article_tag} 的数据访问接口。
 *
 * <p>文章与标签是多对多关系，本接口承担批量插入与按文章删除关联的职责，
 * 与文章保存处在同一事务内。
 */
@Mapper
public interface ArticleTagMapper {

    /** 批量插入关联（动态 SQL 的 &lt;foreach&gt;） */
    int batchInsert(@Param("list") List<ArticleTag> list);

    /** 按文章删除全部关联，编辑文章时先清后插 */
    int deleteByArticleId(@Param("articleId") Long articleId);

    /** 按标签删除全部关联，删除标签前调用 */
    int deleteByTagId(@Param("tagId") Long tagId);

    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    /** 统计某标签被多少篇文章引用，删除前校验用 */
    long countByTagId(@Param("tagId") Long tagId);
}
