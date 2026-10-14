package cn.swust.bokesys.common.constant;

/**
 * 注册用户状态，对应《概要设计》5.2.2：1 正常 / 0 禁用。
 */
public enum UserStatus {

    /** 禁用：不可登录、不可发表评论 */
    DISABLED(0, "禁用"),
    /** 正常 */
    NORMAL(1, "正常");

    private final int code;
    private final String label;

    UserStatus(int code, String label) {
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

    public static UserStatus of(Integer code) {
        if (code != null) {
            for (UserStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return NORMAL;
    }
}
