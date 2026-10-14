package cn.swust.bokesys.common.exception;

import cn.swust.bokesys.common.result.Result;

/**
 * 业务异常，携带错误码与可直接展示给用户的提示信息。
 *
 * <p>对应《概要设计》3.3：业务逻辑层统一抛出本异常，由控制层的
 * {@code GlobalExceptionHandler} 捕获后转换为 {@code Result} 或错误页。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(Result.CODE_ERROR, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = Result.CODE_ERROR;
    }

    public int getCode() {
        return code;
    }
}
