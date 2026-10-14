package cn.swust.bokesys.interceptor;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 后台登录拦截器，对应《概要设计》第 2 章「安全性」与 3.2：
 * 拦截所有 /admin/** 请求，未登录访问一律重定向至登录页。
 *
 * <p>注意：Spring Boot 4 用的是 Spring Framework 7，Servlet API 已迁到
 * {@code jakarta.servlet.*}，不再是 javax。
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** Session 中读取当前登录管理员的键 */
    private static final String ADMIN_KEY = SessionConstant.LOGIN_ADMIN;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object admin = session == null ? null : session.getAttribute(ADMIN_KEY);
        if (admin != null) {
            return true;
        }

        // 异步请求返回 JSON，让前端能把提示渲染成 toast，而不是吞掉一个 302
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result<Void> body = Result.fail(Result.CODE_UNAUTHORIZED, "登录状态已失效，请重新登录");
            response.getOutputStream().write(toJson(body).getBytes(StandardCharsets.UTF_8));
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/admin/login");
        return false;
    }

    /**
     * 判断是否为异步请求。
     *
     * <p>只认 X-Requested-With 与 Accept 头，不依赖任何第三方库。
     */
    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /**
     * 手写最小 JSON，避免为了一个响应体而注入 ObjectMapper。
     *
     * <p>本项目消息里可能含引号，所以做一次最小转义。
     */
    private String toJson(Result<?> result) {
        String message = result.getMessage() == null ? ""
                : result.getMessage().replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"code\":" + result.getCode()
                + ",\"message\":\"" + message + "\",\"data\":null}";
    }
}
