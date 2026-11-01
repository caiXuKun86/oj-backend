package com.kun.oj.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 判题结果信息枚举
 */
@Getter
public enum JudgeMessageEnum {

    ACCEPTED("ACCEPTED", "通过"),
    WRONG_ANSWER("WRONG_ANSWER", "答案错误"),
    COMPILE_ERROR("COMPILE_ERROR", "编译错误"),
    MEMORY_LIMIT_EXCEEDED("MEMORY_LIMIT_EXCEEDED", "内存超限"),
    TIME_LIMIT_EXCEEDED("TIME_LIMIT_EXCEEDED", "运行超时"),
    DANGEROUS_OPERATION("DANGEROUS_OPERATION", "危险操作"),
    RUNTIME_ERROR("RUNTIME_ERROR", "运行错误"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;

    JudgeMessageEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据 code 获取枚举
     */
    public static JudgeMessageEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (JudgeMessageEnum value : JudgeMessageEnum.values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }


}
