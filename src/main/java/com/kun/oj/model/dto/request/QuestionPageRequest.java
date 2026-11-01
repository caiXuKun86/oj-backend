package com.kun.oj.model.dto.request;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionPageRequest extends PageRequest {
    /**
     * 难度
     */
    private String difficulty;
    /**
     * 是否通过
     */
    private Boolean isAccept;

}
