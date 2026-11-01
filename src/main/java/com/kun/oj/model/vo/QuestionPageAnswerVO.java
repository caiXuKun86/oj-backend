package com.kun.oj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目答案
 *
 * @TableName question_answer
 */
@Data
public class QuestionPageAnswerVO {
    /**
     * id
     */
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}