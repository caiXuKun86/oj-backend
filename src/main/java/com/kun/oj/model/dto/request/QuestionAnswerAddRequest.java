package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class QuestionAnswerAddRequest {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 答案内容（标程源码）
     */
    private String content;
}
