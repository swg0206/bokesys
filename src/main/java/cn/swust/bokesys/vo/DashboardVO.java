package cn.swust.bokesys.vo;

import cn.swust.bokesys.po.Article;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘统计对象，对应《概要设计》3.5 与 4.2.4 的输出：
 * 文章总数、评论总数、分类数、标签数四张统计卡片，外加待审核提醒、
 * 最近发布文章列表与各分类文章分布。
 */
public class DashboardVO implements Serializable {

    /** 文章总数 */
    private long articleTotal;
    /** 已发布文章数 */
    private long publishedTotal;
    /** 草稿数 */
    private long draftTotal;
    /** 评论总数 */
    private long commentTotal;
    /** 待审核评论数（用于提醒条） */
    private long pendingCommentTotal;
    /** 分类数 */
    private long categoryTotal;
    /** 标签数 */
    private long tagTotal;
    /** 累计浏览量 */
    private long viewTotal;

    /** 最近发布的文章 */
    private List<Article> recentArticles = new ArrayList<>();

    /** 近 7 天文章发布趋势 */
    private List<TrendPointVO> trend = new ArrayList<>();

    /** 各分类文章分布 */
    private List<CategoryStatVO> categoryStats = new ArrayList<>();

    /** 是否存在待审核评论，供模板控制提醒条的显隐 */
    public boolean isHasPending() {
        return pendingCommentTotal > 0;
    }

    /** 趋势图中的最大计数，用于换算柱状高度 */
    public int getTrendMax() {
        int max = 1;
        for (TrendPointVO p : trend) {
            if (p.getArticleCount() != null && p.getArticleCount() > max) {
                max = p.getArticleCount();
            }
        }
        return max;
    }

    public long getArticleTotal() {
        return articleTotal;
    }

    public void setArticleTotal(long articleTotal) {
        this.articleTotal = articleTotal;
    }

    public long getPublishedTotal() {
        return publishedTotal;
    }

    public void setPublishedTotal(long publishedTotal) {
        this.publishedTotal = publishedTotal;
    }

    public long getDraftTotal() {
        return draftTotal;
    }

    public void setDraftTotal(long draftTotal) {
        this.draftTotal = draftTotal;
    }

    public long getCommentTotal() {
        return commentTotal;
    }

    public void setCommentTotal(long commentTotal) {
        this.commentTotal = commentTotal;
    }

    public long getPendingCommentTotal() {
        return pendingCommentTotal;
    }

    public void setPendingCommentTotal(long pendingCommentTotal) {
        this.pendingCommentTotal = pendingCommentTotal;
    }

    public long getCategoryTotal() {
        return categoryTotal;
    }

    public void setCategoryTotal(long categoryTotal) {
        this.categoryTotal = categoryTotal;
    }

    public long getTagTotal() {
        return tagTotal;
    }

    public void setTagTotal(long tagTotal) {
        this.tagTotal = tagTotal;
    }

    public long getViewTotal() {
        return viewTotal;
    }

    public void setViewTotal(long viewTotal) {
        this.viewTotal = viewTotal;
    }

    public List<Article> getRecentArticles() {
        return recentArticles;
    }

    public void setRecentArticles(List<Article> recentArticles) {
        this.recentArticles = recentArticles == null ? new ArrayList<>() : recentArticles;
    }

    public List<TrendPointVO> getTrend() {
        return trend;
    }

    public void setTrend(List<TrendPointVO> trend) {
        this.trend = trend == null ? new ArrayList<>() : trend;
    }

    public List<CategoryStatVO> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<CategoryStatVO> categoryStats) {
        this.categoryStats = categoryStats == null ? new ArrayList<>() : categoryStats;
    }
}
