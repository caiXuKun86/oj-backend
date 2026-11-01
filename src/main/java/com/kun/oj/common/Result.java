package com.kun.oj.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回皮 *
 * @param <T>
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class Result<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public Result(int code, T data) {
        this(code, data, "");
    }

    public Result(ResultCode resultCode) {
        this(resultCode.getCode(), null, resultCode.getMessage());
    }
}

