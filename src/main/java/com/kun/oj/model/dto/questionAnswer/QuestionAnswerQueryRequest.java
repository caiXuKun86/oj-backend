package com.kun.oj.model.dto.questionAnswer;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionAnswerQueryRequest extends PageRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;


    /**
     * 提交用户
     */
    private String userAccount;
    /**
     * 题目编号
     */
    private String number;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 状态：0-待审皮 1-通过, 2-拒绝
     */
    private Integer reviewStatus;






    private static final long serialVersionUID = 1L;
}
