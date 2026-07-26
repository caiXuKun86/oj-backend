package com.kun.oj.model.dto.questionAnswer;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionAnswerUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;

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





    private static final long serialVersionUID = 1L;
}
