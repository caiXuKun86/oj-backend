package com.kun.oj.model.dto.request;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminQuestionPageRequest extends PageRequest {


    private Long id;

    /**
     * 题目编号
     */
    private String number;

    /**
     * 题目名称
     */
    private String title;


    /**
     * 题目标签(json 字符串数组)
     */
    private String tag;

    /**
     * 难度
     */
    private String difficulty;


}
