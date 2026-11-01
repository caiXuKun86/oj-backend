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
public enum UserRoleEnum {

    USER("cpp", "普通用户"),
    ADMIN("java", "管理员");


    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;

    UserRoleEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static UserRoleEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (UserRoleEnum value : UserRoleEnum.values()) {
            if (code.equals(value.getCode())) {
                return value;
            }
        }
        return null;
    }


}

