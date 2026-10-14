package cn.swust.bokesys.vo;

import java.io.Serializable;

/**
 * 仪表盘「各分类文章分布」的一项。
 */
public class CategoryStatVO implements Serializable {

    private String name;
    private Integer articleCount;

    public CategoryStatVO() {
    }

    public CategoryStatVO(String name, Integer articleCount) {
        this.name = name;
        this.articleCount = articleCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }
}
