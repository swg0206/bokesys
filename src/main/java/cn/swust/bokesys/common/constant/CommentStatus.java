package cn.swust.bokesys.common.constant;

/**
 * 评论状态，对应《概要设计》5.2.7：0 待审核 / 1 已通过 / 2 已拒绝。
 *
 * <p>核心业务规则：用户提交的评论一律落库为 {@link #PENDING}，
 * 只有管理员审核通过（{@link #APPROVED}）后才在前台展示。
 */
public enum CommentStatus {

    /** 待审核：已落库但前台不可见 */
    PENDING(0, "待审核"),
    /** 已通过：前台展示，并计入文章的评论数 */
    APPROVED(1, "已通过"),
    /** 已拒绝：前台不展示 */
    REJECTED(2, "已拒绝");

    private final int code;
    private final String label;

    CommentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 判断给定状态码是否等于本枚举值（{@link #getCode()} 是原始 int，不能直接 .equals） */
    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

    public static CommentStatus of(Integer code) {
        if (code != null) {
            for (CommentStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return PENDING;
    }
}
