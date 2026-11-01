package com.kun.oj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 * @TableName user
 */
@Data
public class UserPageVO {
    /**
     * id
     */
    private Long id;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}