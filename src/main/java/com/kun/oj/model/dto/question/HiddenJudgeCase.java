package com.kun.oj.model.dto.question;

import lombok.Data;

/**
 * 题目用例
 */
@Data
public class HiddenJudgeCase {

    /**
     * COS key
     */
    private String key;

    /**
     * 用例数量
     */
    private Integer count;
    /**
     * 测试用例版本
     */
    private String testcaseVersion;
}

