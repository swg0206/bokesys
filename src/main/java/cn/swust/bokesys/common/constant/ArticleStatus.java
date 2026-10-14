package cn.swust.bokesys.common.constant;

/**
 * 文章状态，对应《概要设计》5.2.3：1 已发布 / 0 草稿。
 */
public enum ArticleStatus {

    /** 草稿：仅在后台可见，前台首页与检索均不展示 */
    DRAFT(0, "草稿"),
    /** 已发布：前台可浏览、可被检索 */
    PUBLISHED(1, "已发布");

    private final int code;
    private final String label;

    ArticleStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 判断给定状态码是否等于本枚举值。
     *
     * <p>注意 {@link #getCode()} 返回的是原始 int，直接 {@code getCode().equals(x)}
     * 无法编译，统一用本方法做比较，同时天然处理了 null。
     */
    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

    public static ArticleStatus of(Integer code) {
        if (code != null) {
            for (ArticleStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return DRAFT;
    }
}
