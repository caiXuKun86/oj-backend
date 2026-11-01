package com.kun.oj.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class LoginUserVO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;
    /**
     * token
     */
    private String token;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户角色：user/admin/ban
     */
    private String role;

    /**
     * 创建时间
     */
    private Date createTime;


}
