package com.kun.oj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionSubmitPageVO {
    /**
     * id
     */
    private Long id;

    /**
     * 编程语言
     */
    private String language;


    /**
     * 测评信息
     */
    private String message;


    /**
     * 判题状态（0-待判题、1-判题中、2-成功、3-失败）
     */
    private Integer status;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题目编号
     */
    private String number;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}
