package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章持久化对象，对应数据表 {@code t_article}（《概要设计》5.2.3）。
 */
public class Article implements Serializable {

    private Long id;
    private String title;
    /** 文章摘要，列表页展示 */
    private String summary;
    /** 文章正文（Markdown 源文） */
    private String content;
    /** 封面图访问路径 */
    private String coverImage;
    /** 所属分类 ID */
    private Long categoryId;
    /** 状态：1 已发布 / 0 草稿 */
    private Integer status;
    /** 浏览量 */
    private Integer viewCount;
    /** 已审核通过的评论数 */
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
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
}
