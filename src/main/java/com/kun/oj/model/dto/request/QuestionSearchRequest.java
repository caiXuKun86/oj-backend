package com.kun.oj.model.dto.request;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionSearchRequest extends PageRequest {
    /**
     * 难度
     */
    private String keyword;


}
