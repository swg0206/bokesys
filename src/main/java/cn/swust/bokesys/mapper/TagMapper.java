package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签表 {@code t_tag} 的数据访问接口。
 */
@Mapper
public interface TagMapper {

    /** 全部标签，附带引用文章数 */
    List<Tag> selectAllWithCount();

    /** 热门标签：按引用文章数倒序取前 N 个，供首页标签云使用 */
    List<Tag> selectHotTags(@Param("limit") int limit);

    /** 某篇文章关联的标签 */
    List<Tag> selectByArticleId(@Param("articleId") Long articleId);

    Tag selectById(@Param("id") Long id);

    Tag selectByName(@Param("name") String name);

    int insert(Tag tag);

    int update(Tag tag);

    int deleteById(@Param("id") Long id);

    long countAll();
}
