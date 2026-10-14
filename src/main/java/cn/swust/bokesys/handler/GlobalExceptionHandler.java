package cn.swust.bokesys.handler;

import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理，对应《概要设计》3.2：
 * 通过 GlobalExceptionHandler 对全局异常做统一兜底处理，
 * 避免异常堆栈直接暴露给用户。
 *
 * <p>分流策略：
 * <ul>
 *   <li>异步请求（fetch / Accept: application/json）→ 返回 {@code Result} JSON，前端渲染成提示</li>
 *   <li>页面请求 → 渲染错误页，带上可读的原因</li>
 * </ul>
 *
 * <p>实现说明：处理方法统一声明返回 {@code Object}，按实际返回类型分流 ——
 * 返回 {@code ResponseEntity} 时由 Spring 直接写出响应体（不需要 {@code @ResponseBody}），
 * 返回 {@code ModelAndView} 时走视图渲染。这样同一个方法就能兼顾两种客户端。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：这是「预期内」的失败（分类下还有文章、命中敏感词、密码错误等），
     * 只记 warn 且不打印堆栈，避免日志被正常业务分支刷屏。
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]：{}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        if (isAjax(request)) {
            return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
        }
        ModelAndView mv = new ModelAndView("error/business");
        mv.addObject("message", ex.getMessage());
        mv.addObject("backUrl", safeReferer(request));
        return mv;
    }

    /**
     * 参数校验失败（{@code @Valid}）。
     *
     * <p>不加这个分支的话会落到兜底 handler，用户看到的是「服务器开小差了」，
     * 明明是「标题不能为空」却像是系统故障，因此这里把首个字段错误直接回给前端。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValidation(Exception ex, HttpServletRequest request) {
        BindingResult binding = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();
        String message = binding.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isEmpty())
                .findFirst()
                .orElse("提交的内容不符合要求，请检查后重试");
        log.warn("参数校验失败 [{} {}]：{}", request.getMethod(), request.getRequestURI(), message);

        if (isAjax(request)) {
            return ResponseEntity.ok(Result.fail(Result.CODE_BAD_REQUEST, message));
        }
        ModelAndView mv = new ModelAndView("error/business");
        mv.addObject("message", message);
        mv.addObject("backUrl", safeReferer(request));
        return mv;
    }

    /** 参数类型不对（例如路径里的 id 传了非数字）→ 提示而不是 500 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public Object handleBadParameter(Exception ex, HttpServletRequest request) {
        log.warn("请求参数不合法 [{} {}]：{}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        String message = "请求参数不合法，请检查后重试";
        if (isAjax(request)) {
            return ResponseEntity.ok(Result.fail(Result.CODE_BAD_REQUEST, message));
        }
        ModelAndView mv = new ModelAndView("error/business");
        mv.addObject("message", message);
        mv.addObject("backUrl", safeReferer(request));
        return mv;
    }

    /** 静态资源或路径不存在 → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail(HttpStatus.NOT_FOUND.value(), "请求的资源不存在"));
        }
        ModelAndView mv = new ModelAndView("error/404");
        mv.addObject("path", request.getRequestURI());
        return mv;
    }

    /** 兜底：非预期异常，记录完整堆栈供排查，但对外只给一句通用提示 */
    @ExceptionHandler(Exception.class)
    public Object handleOther(Exception ex, HttpServletRequest request) {
        log.error("未处理异常 [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        if (isAjax(request)) {
            return ResponseEntity.ok(Result.fail("服务器开小差了，请稍后重试"));
        }
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", "服务器开小差了，请稍后重试");
        return mv;
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /**
     * 取 referer 作为「返回上一页」的兜底目标。
     *
     * <p>只保留路径部分，丢弃协议与主机，避免把外部站点写进跳转链接。
     */
    private String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            return "/";
        }
        int schemeIdx = referer.indexOf("://");
        if (schemeIdx < 0) {
            return "/";
        }
        int pathStart = referer.indexOf('/', schemeIdx + 3);
        if (pathStart < 0) {
            return "/";
        }
        String path = referer.substring(pathStart);
        return path.isEmpty() ? "/" : path;
    }
}
