package com.kun.oj.enums;

import lombok.Getter;

/**
 * 判题状态枚举
 */
@Getter
public enum JudgeStatusEnum {

    WAITING(0, "待判题"),
    JUDGING(1, "判题中"),
    RUNNING(1, "判题中"),
    SUCCESS(2, "判题成功"),
    SUCCEED(2, "判题成功"),
    FAILED(3, "判题失败");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 信息
     */
    private final String message;

    JudgeStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static JudgeStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JudgeStatusEnum value : JudgeStatusEnum.values()) {
            if (code.equals(value.getCode())) {
                return value;
            }
        }
        return null;
    }

    /**
     * 兼容老代码调用：等同于 getCode()
     */
    public Integer getValue() {
        return this.code;
    }

    /**
     * 兼容老代码调用：等同于 getMessage()
     */
    public String getText() {
        return this.message;
    }
}
