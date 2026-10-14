package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签持久化对象，对应数据表 {@code t_tag}（《概要设计》5.2.5）。
 */
public class Tag implements Serializable {

    private Long id;
    /** 标签名称，唯一 */
    private String name;
    private LocalDateTime createTime;

    /** 关联的文章数量（派生字段，非表字段，用于「热门标签」与后台标签列表） */
    private Integer articleCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
