package com.kun.oj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目提交
 * @TableName question_submit
 */
@Data
public class QuestionSubmitVO {
    /**
     * id
     */
    private Long id;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 测评信息
     */
    private String message;

    /**
     * 判题信息（json 格式）
     */
    private String judgeInfo;

    /**
     * 判题状态（0-待判题、1-判题中、2-成功、3-失败）
     */
    private Integer status;

    /**
     * 题目编号
     */
    private String number;

    /**
     * 题目标题
     */
    private String title;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}