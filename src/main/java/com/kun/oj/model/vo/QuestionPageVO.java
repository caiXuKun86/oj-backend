package com.kun.oj.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionPageVO {
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
     * 题目标签(json 字符串数组)
     */
    private String tags;

    /**
     * 难度
     */
    private String difficulty;


    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

    /**
     * 通过率
     */
    private BigDecimal passRate;
    /**
     * 是否通过
     */
    private Boolean isAccept;


}
