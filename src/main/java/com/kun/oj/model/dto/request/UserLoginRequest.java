package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class UserLoginRequest {


    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;


}
