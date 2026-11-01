package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class QuestionAnswerUpdateRequest {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 答案内容（标程源码）
     */
    private String content;
}
