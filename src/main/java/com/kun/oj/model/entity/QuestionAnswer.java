package com.kun.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 题目答案
 * @TableName question_answer
 */
@TableName(value ="question_answer")
@Data
public class QuestionAnswer {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 提交用户 id
     */
    private Long userId;

    /**
     * 编程语言
     */
    private String language;

    /**

  * 答案内容
     */
    private String content;
    /**
     * 状态：0-待审皮 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息（如拒绝原因皮     */
    private String reviewMessage;

    /**
     * 审核皮id
     */
    private Long reviewerId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;


}
