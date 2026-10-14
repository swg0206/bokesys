package cn.swust.bokesys.vo;

import java.io.Serializable;

/**
 * 仪表盘「文章发布趋势」的一个数据点。
 *
 * <p>日期用字符串承载（yyyy-MM-dd），由 Service 层用 Java 补零后再交给页面，
 * 因为 H2 没有 MySQL 的 DATE_FORMAT 函数，把格式化放到 Java 里才能两库通用。
 */
public class TrendPointVO implements Serializable {

    /** 日期，yyyy-MM-dd */
    private String statDate;
    /** 当日发布文章数 */
    private Integer articleCount;

    public TrendPointVO() {
    }

    public TrendPointVO(String statDate, Integer articleCount) {
        this.statDate = statDate;
        this.articleCount = articleCount;
    }

    public String getStatDate() {
        return statDate;
    }

    public void setStatDate(String statDate) {
        this.statDate = statDate;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
