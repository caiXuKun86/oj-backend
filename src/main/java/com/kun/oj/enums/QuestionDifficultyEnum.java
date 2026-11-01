package com.kun.oj.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 自定义错误码
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Getter
public enum QuestionDifficultyEnum {

    EASY("easy", "简单"),
    MIDDLE("middle", "中等"),
    HARD("hard", "困难");


    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;

    QuestionDifficultyEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static QuestionDifficultyEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (QuestionDifficultyEnum value : QuestionDifficultyEnum.values()) {
            if (code.equals(value.getCode())) {
                return value;
            }
        }
        return null;
    }


}

