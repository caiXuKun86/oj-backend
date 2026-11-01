package com.kun.oj.exception;


import com.kun.oj.common.ResultCode;

/**
 * 抛异常工具类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resultCode
     */
    public static void throwIf(boolean condition, ResultCode resultCode) {
        throwIf(condition, new BusinessException(resultCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resultCode
     * @param message
     */
    public static void throwIf(boolean condition, ResultCode resultCode, String message) {
        throwIf(condition, new BusinessException(resultCode, message));
    }
}

