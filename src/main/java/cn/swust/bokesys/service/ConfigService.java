package cn.swust.bokesys.service;

import cn.swust.bokesys.po.SysConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置业务接口，对应《概要设计》3.3 与接口 API-21。
 *
 * <p>站点名称、站点描述、备案信息等参数持久化在 t_config 表，
 * 修改后无需重新部署即可生效（《概要设计》第 2 章设计原则⑷）。
 */
public interface ConfigService {

    /** 全部配置项（后台「系统管理」页面用） */
    List<SysConfig> listAll();

    /**
     * 以 Map 形式读取全部配置，供前台模板渲染站点信息。
     *
     * <p>每次读取都带缓存，配置变更时主动失效。
     */
    Map<String, String> loadAllAsMap();

    /** 读取单个配置项，不存在时返回默认值 */
    String get(String key, String defaultValue);

    /** 批量保存配置（键不存在则新增，不存在则更新） */
    void save(Map<String, String> values);

    /** 配置变更后清空缓存 */
    void evictCache();
}
