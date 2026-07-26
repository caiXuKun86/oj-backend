package com.kun.oj.model.dto.questionSubmit;

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
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {


    /**
     * 编程语言
     */
    private String language;

    /**
     * 判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）
     */
    private Integer status;

    /**
     * 题目 编号
     */
    private String number;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 测评信息
     */
    private String message;

    /**
     * 提交用户
     */
    private String userAccount;


    private static final long serialVersionUID = 1L;
}
