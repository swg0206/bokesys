package cn.swust.bokesys.common.constant;

/**
 * Session 属性名常量，集中管理避免各处硬编码字符串拼错。
 *
 * <p>对应《概要设计》第 2 章「安全性」：管理员登录成功后把身份写入 HttpSession，
 * 由 LoginInterceptor 拦截 /admin/** 做统一校验。
 */
public final class SessionConstant {

    /** 已登录的管理员对象（存 Admin 实例） */
    public static final String LOGIN_ADMIN = "loginAdmin";

    /** 已登录的注册用户对象（存 User 实例） */
    public static final String LOGIN_USER = "loginUser";

    /** 后台登录页的图形验证码文本 */
    public static final String CAPTCHA_CODE = "captchaCode";

    /** 后台登录连续失败次数 */
    public static final String LOGIN_FAIL_COUNT = "loginFailCount";

    /** 后台登录锁定截止时间戳（毫秒） */
    public static final String LOGIN_LOCK_UNTIL = "loginLockUntil";

    private SessionConstant() {
    }
}
