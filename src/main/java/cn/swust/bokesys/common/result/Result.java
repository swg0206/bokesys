package cn.swust.bokesys.common.result;

import java.io.Serializable;

/**
 * 统一返回体，对应《概要设计》3.6 内部接口约定：
 * {@code { "code": 200, "message": "操作成功", "data": {...} }}
 *
 * <p>所有异步接口（/api/**）与后台的增删改接口统一返回本类型。
 */
public class Result<T> implements Serializable {

    /** 操作成功 */
    public static final int CODE_SUCCESS = 200;
    /** 参数错误 */
    public static final int CODE_BAD_REQUEST = 400;
    /** 未登录 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 服务器内部错误 */
    public static final int CODE_ERROR = 500;

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(CODE_ERROR, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
