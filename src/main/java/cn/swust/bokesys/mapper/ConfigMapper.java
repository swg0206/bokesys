package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置表 {@code t_config} 的数据访问接口，对应接口 API-21。
 */
@Mapper
public interface ConfigMapper {

    List<SysConfig> selectAll();

    SysConfig selectByKey(@Param("configKey") String configKey);

    int insert(SysConfig config);

    /** 按配置键更新配置值（返回 0 表示该键不存在） */
    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);

    long countAll();
}
