package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {


    /**
     * 用户名
     */
    private String username;


    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户角色：user/admin/ban
     */
    private String role;
    /**
     * 密码
     */
    private String password;

}
