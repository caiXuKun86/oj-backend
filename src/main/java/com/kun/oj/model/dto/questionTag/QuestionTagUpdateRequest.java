package com.kun.oj.model.dto.questionTag;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionTagUpdateRequest implements Serializable {

    private Long id;
    /**
     * 标签名称
     */
    private String name;


    private static final long serialVersionUID = 1L;
}
