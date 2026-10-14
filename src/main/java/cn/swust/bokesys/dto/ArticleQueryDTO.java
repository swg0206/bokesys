package cn.swust.bokesys.dto;

import java.io.Serializable;

/**
 * 后台文章列表的查询条件，对应《概要设计》4.2.3 的「查询条件输入」：
 * 标题关键词、分类 ID、文章状态、页码。
 */
public class ArticleQueryDTO implements Serializable {

    /** 标题关键词，模糊匹配 */
    private String title;
    private Long categoryId;
    /** 状态：1 已发布 / 0 草稿；为空表示不限 */
    private Integer status;
    /** 标签 ID，用于按标签筛选 */
    private Long tagId;
    /** 关键词，同时匹配标题与正文（前台搜索用） */
    private String keyword;

    private Integer pageNo = 1;
    private Integer pageSize = 10;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
