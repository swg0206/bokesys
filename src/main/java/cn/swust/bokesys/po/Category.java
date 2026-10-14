package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类持久化对象，对应数据表 {@code t_category}（《概要设计》5.2.4）。
 *
 * <p>说明：文档 3.3 提到「文章发布时同步维护分类的文章计数」，但 5.2.4 的字段清单中
 * 分类表并没有计数字段。为保持与文档表结构完全一致，此处不加冗余计数列，
 * 分类下的文章数量由 {@code CategoryMapper} 以 LEFT JOIN 派生查询得到，
 * 好处是不存在计数漂移。
 */
public class Category implements Serializable {

    private Long id;
    /** 分类名称，唯一 */
    private String name;
    private String description;
    /** 排序序号，升序排列 */
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 该分类下的文章数量（派生字段，非表字段） */
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
