package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.mapper.ConfigMapper;
import cn.swust.bokesys.po.SysConfig;
import cn.swust.bokesys.service.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置业务实现。
 *
 * <p>配置项读多写少（每个前台页面都要渲染站点名与备案号），因此在内存中缓存一份，
 * 保存时整体失效。用 {@code volatile} 引用替换而非原地修改，保证读线程看到的
 * 始终是一份完整快照。
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    private final ConfigMapper configMapper;

    private volatile Map<String, String> cache;

    public ConfigServiceImpl(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public List<SysConfig> listAll() {
        return configMapper.selectAll();
    }

    @Override
    public Map<String, String> loadAllAsMap() {
        Map<String, String> snapshot = this.cache;
        if (snapshot != null) {
            return snapshot;
        }
        Map<String, String> loaded = new LinkedHashMap<>();
        for (SysConfig config : configMapper.selectAll()) {
            loaded.put(config.getConfigKey(), config.getConfigValue());
        }
        this.cache = loaded;
        return loaded;
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = loadAllAsMap().get(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * 批量保存。
     *
     * <p>注意：无法用数据库方言相关的 upsert（H2 的 MERGE 与 MySQL 的
     * ON DUPLICATE KEY UPDATE 语法不同），所以统一走「先 UPDATE，
     * 影响行数为 0 再 INSERT」的通用形态，两库通吃。
     */
    @Override
    @Transactional
    public void save(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            int updated = configMapper.updateByKey(key, value);
            if (updated == 0) {
                SysConfig config = new SysConfig(key, value);
                config.setRemark("由后台系统管理页面新增");
                config.setUpdateTime(now);
                configMapper.insert(config);
            }
        }
        evictCache();
    }

    @Override
    public void evictCache() {
        this.cache = null;
    }
}
