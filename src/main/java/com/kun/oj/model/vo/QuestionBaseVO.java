package com.kun.oj.model.vo;

import com.kun.oj.model.entity.QuestionTag;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBaseVO {
    /**
     * id
     */
    private Long id;
    /**
     * 编号
     */
    private String number;
    /**
     * 标题
     */
    private String title;
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
     * 时间限制
     */
    private Integer timeLimit;
    /**
     * 空间限制
     */
    private Integer memoryLimit;
    /**
     * 用户是否通过该题目
     */
    private Boolean isAccepted;
    /**
     * 标签列表
     */
    private List<QuestionTag> tags;


}
