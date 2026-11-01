package com.kun.oj.model.dto.request;

import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionSubmitPageRequest extends PageRequest {


    /**
     * 编程语言
     */
    private String language;


    /**
     * 测评信息
     */
    private String message;


    /**
     * 判题状态（0-待判题、1-判题中、2-成功、3-失败）
     */
    private Integer status;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题目编号
     */
    private String number;


    /**
     * 创建用户 id
     */
    private Long userId;


}
