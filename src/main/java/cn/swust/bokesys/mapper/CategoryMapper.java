package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类表 {@code t_category} 的数据访问接口。
 *
 * <p>「该分类下的文章数量」采用 LEFT JOIN 派生查询而非冗余计数列，
 * 以保证与文章表强一致、不存在计数漂移（见 Category 类的说明）。
 */
@Mapper
public interface CategoryMapper {

    /** 全部分类，按排序号升序，附带各分类下文章数量（含草稿，后台管理与删除校验用） */
    List<Category> selectAllWithCount();

    /** 全部分类，附带**已发布**文章数量，供前台侧栏导航使用 */
    List<Category> selectPublishedWithCount();

    Category selectById(@Param("id") Long id);

    Category selectByName(@Param("name") String name);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);

    /** 统计该分类下的文章数量，删除前校验用 */
    long countArticleByCategoryId(@Param("categoryId") Long categoryId);

    long countAll();
}
