package cn.swust.bokesys.vo;

import cn.swust.bokesys.po.Tag;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章展示对象，对应《概要设计》3.5：
 * 文章列表 / 详情展示对象，含分类名、标签集合、评论数。
 *
 * <p>把「数据库结构」与「页面展示结构」隔离开，
 * 页面需要的分类名、标签列表都在这里一次装配好，避免模板里再去查库。
 */
public class ArticleVO implements Serializable {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private Long categoryId;
    /** 分类名称（由 t_category 关联得到） */
    private String categoryName;
    /** 状态：1 已发布 / 0 草稿 */
    private Integer status;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 标签集合 */
    private List<Tag> tags = new ArrayList<>();

    /** 标签名拼接串，供列表页一行展示与 meta keywords 使用 */
    public String getTagNames() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream().map(Tag::getName).collect(Collectors.joining("、"));
    }

    /** 是否为已发布状态 */
    public boolean isPublished() {
        return status != null && status == 1;
    }

    /** 列表页摘要：优先取文章自带摘要 */
    public String getDisplaySummary() {
        return summary;
    }

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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
