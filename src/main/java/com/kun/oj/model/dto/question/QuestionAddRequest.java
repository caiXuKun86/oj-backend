package com.kun.oj.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionAddRequest implements Serializable {
    /**
     * 编号
     */
    private String number;
    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;
    /**
     * 难度
     */
    private String difficulty;

    /**
     * 标签列表（json 数组）
     */
    private List<Long> tagIds;

    /**
     * 时间限制
     */
    private Long timeLimit;
    /**
     * 空间限制
     */
    private Long memoryLimit;


    private static final long serialVersionUID = 1L;
}
