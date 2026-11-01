package com.kun.oj.model.dto.request;

import lombok.Data;

@Data
public class QuestionUpdateRequest {


    /**
     * 题目名称
     */
    private String title;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 题目标签(json 字符串数组)
     */
    private String tags;

    /**
     * 难度
     */
    private String difficulty;

    /**
     * 时间限制(ms)
     */
    private Integer timeLimit;

    /**
     * 空间限制(MB)
     */
    private Integer memoryLimit;

    /**
     * 测试判题用例
     */
    private String judgeCaseSample;
    /**
     * 判题用例 url/key
     */
    private String judgeCaseKey;

    /**
     * 判题用例 版本
     */
    private String judgeCaseVersion;

    /**
     * 判题用例 数量
     */
    private Integer judgeCaseCount;


}
