package com.kun.oj.model.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * 题目
 * @TableName question
 */
@TableName(value ="question")
@Data
public class QuestionVO {
    /**
     * id
     */
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

    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

}
