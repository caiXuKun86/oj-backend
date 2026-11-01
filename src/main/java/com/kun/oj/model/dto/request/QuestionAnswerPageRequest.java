package com.kun.oj.model.dto.request;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionAnswerPageRequest extends PageRequest {
    /**
     * 难度
     */
    private Long questionId;
    /**
     * 是否通过
     */
    private String language;

}
