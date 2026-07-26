package com.kun.oj.model.dto.questionTag;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionTagAddRequest implements Serializable {



    /**
     * 标签名称
     */
    private String name;

    /**
     * 父标签Id
     */
    private Long parentId;



    private static final long serialVersionUID = 1L;
}
