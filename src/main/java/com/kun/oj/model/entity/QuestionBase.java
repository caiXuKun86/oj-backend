package com.kun.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 题目
 *
 * @TableName question
 */
@TableName(value = "question_base")
@Data
public class QuestionBase {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    private Long timeLimit;
    /**
     * 空间限制
     */
    private Long memoryLimit;
    /**
     * 创建者 id
     */
    private Long userId;
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
