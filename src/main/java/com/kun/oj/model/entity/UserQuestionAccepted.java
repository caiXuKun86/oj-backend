package com.kun.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户通过题目关联表
 * @TableName user_question_accepted
 */
@TableName(value ="user_question_accepted")
@Data
public class UserQuestionAccepted {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 关联的首次通过提交记录 id
     */
    private Long submitId;

    /**
     * 首次通过时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}