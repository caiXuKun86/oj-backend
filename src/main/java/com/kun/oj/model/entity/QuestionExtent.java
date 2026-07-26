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
@TableName(value = "question_extent")
@Data
public class QuestionExtent {
    /**
     * id
     */
    @TableId(type = IdType.NONE)
    private Long id;
    /**
     * 内容
     */
    private String content;

    /**
     * 测试判题用例（json 数组）
     */
    private String sampleJudgeCase;
    /**
     * 判题用例（json 数组）
     */
    private String hiddenJudgeCase;


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
