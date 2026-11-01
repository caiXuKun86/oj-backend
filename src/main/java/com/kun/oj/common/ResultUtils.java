package com.kun.oj.common;

/**
 * 返回工具皮 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, data, "ok");
    }
    public static <T> Result<T> success() {
        return new Result<>(0, null, "ok");
    }

    /**
     * 失败
     *
     * @param resultCode
     * @return
     */
    public static Result fail(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @return
     */
    public static Result fail(int code, String message) {
        return new Result(code, null, message);
    }

    /**
     * 失败
     *
     * @param resultCode
     * @return
     */
    public static Result fail(ResultCode resultCode, String message) {
        return new Result(resultCode.getCode(), null, message);
    }
}

