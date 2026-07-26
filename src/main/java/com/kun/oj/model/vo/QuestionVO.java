package com.kun.oj.model.vo;

import com.kun.oj.model.dto.question.HiddenJudgeCase;
import com.kun.oj.model.dto.question.SampleJudgeCase;
import com.kun.oj.model.entity.QuestionTag;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 帖子视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class QuestionVO implements Serializable {

    /**
     * id
     */
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
     * 内容
     */
    private String content;

    /**
     * 题目标签
     */
    private List<QuestionTag> tags;

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
     * 测试用例（json 数组）
     */
    private List<SampleJudgeCase> sampleJudgeCaseList;

    /**
     * 判题用例
     */
    private HiddenJudgeCase hiddenJudgeCase;
    /**
     * 时间限制
     */
    private Integer timeLimit;
    /**
     * 空间限制
     */
    private Integer memoryLimit;
    /**
     * 用户是否通过
     */
    private Boolean isAccepted;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建人信息
     */
    private UserVO user;

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

    private static final long serialVersionUID = 1L;


}

