package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.Link;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 友情链接表 {@code t_link} 的数据访问接口。
 */
@Mapper
public interface LinkMapper {

    /** 前台可见的友链：status=1，按 sort_order 升序，对应接口 API-08 */
    List<Link> selectVisible();

    /** 后台列表：全部友链，按 sort_order 升序 */
    List<Link> selectAll();

    Link selectById(@Param("id") Long id);

    int insert(Link link);

    int update(Link link);

    int deleteById(@Param("id") Long id);

    long countAll();
}
