package cn.swust.bokesys.po;

import java.io.Serializable;

/**
 * 文章-标签关联持久化对象，对应数据表 {@code t_article_tag}（《概要设计》5.2.6）。
 *
 * <p>文章与标签之间是多对多关系，本表为中间表，
 * 并对 (article_id, tag_id) 建立唯一索引避免同一篇文章重复关联同一标签。
 */
public class ArticleTag implements Serializable {

    private Long id;
    private Long articleId;
    private Long tagId;

    public ArticleTag() {
    }

    public ArticleTag(Long articleId, Long tagId) {
        this.articleId = articleId;
        this.tagId = tagId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
