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
public enum CodeLanguageEnum {

    CPP("cpp", "cpp"),
    JAVA("java", "java"),
    PYTHON("python", "python");


    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;

    CodeLanguageEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static CodeLanguageEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (CodeLanguageEnum value : CodeLanguageEnum.values()) {
            if (code.equals(value.getCode())) {
                return value;
            }
        }
        return null;
    }


}

