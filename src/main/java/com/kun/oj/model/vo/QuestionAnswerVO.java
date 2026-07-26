package com.kun.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionAnswerVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;
    /**
     * 题目
     */
    private String title;
    /**
     * 题目编号
     */
    private String number;

    /**
     * 提交用户 id
     */
    private Long userId;
    /**
     * 提交用户信息
     */
    private String userAccount;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 答案内容
     */
    private String content;






    private static final long serialVersionUID = 1L;


}

