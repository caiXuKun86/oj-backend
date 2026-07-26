package com.kun.oj.model.dto.question;

import lombok.Data;

/**
 * 题目用例
 */
@Data
public class SampleJudgeCase {

    /**
     * 输入用例
     */
    private String input;

    /**
     * 输出用例
     */
    private String output;
}

