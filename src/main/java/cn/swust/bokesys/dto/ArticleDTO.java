package cn.swust.bokesys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章保存入参，对应《概要设计》3.5 域模型层的 ArticleDTO。
 *
 * <p>同时承载「发布」与「保存草稿」两种动作：由 {@link #status} 区分
 * （1 发布 / 0 草稿），也决定了前端点击的是哪个按钮。
 */
public class ArticleDTO implements Serializable {

    /** 文章 ID，编辑场景传入；新增时为空 */
    private Long id;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "文章摘要不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    private String coverImage;

    @NotNull(message = "请选择文章分类")
    private Long categoryId;

    /** 状态：1 已发布 / 0 草稿 */
    private Integer status;

    /** 关联标签 ID 集合 */
    private List<Long> tagIds = new ArrayList<>();

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

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds == null ? new ArrayList<>() : tagIds;
    }
}
