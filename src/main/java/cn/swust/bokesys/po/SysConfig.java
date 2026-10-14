package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置持久化对象，对应数据表 {@code t_config}（《概要设计》5.2.9）。
 *
 * <p>站点名称、站点描述、备案信息等参数持久化在本表，修改后无需重新部署即可生效。
 * 列名使用 {@code config_key} / {@code config_value}，刻意避开 H2 的保留字 key / value。
 */
public class SysConfig implements Serializable {

    private Long id;
    /** 配置项键名，如 site_name、site_description、icp_no */
    private String configKey;
    /** 配置项值 */
    private String configValue;
    /** 配置项说明 */
    private String remark;
    private LocalDateTime updateTime;

    public SysConfig() {
    }

    public SysConfig(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
