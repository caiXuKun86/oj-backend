package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class QuestionSubmitRequest {
    /**
     * id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

}
