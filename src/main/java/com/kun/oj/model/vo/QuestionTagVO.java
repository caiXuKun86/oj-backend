package com.kun.oj.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.kun.oj.model.entity.QuestionTag;
import lombok.Data;

@Data
public class QuestionTagVO {
    /**
     * id
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 父标签Id
     */
    private Long parentId;

    private static final long serialVersionUID = 1L;

    public static QuestionTagVO objToVo(QuestionTag questionTag) {
        QuestionTagVO questionTagVO=new QuestionTagVO();
        BeanUtil.copyProperties(questionTag,questionTagVO);
        return questionTagVO;

    }
}

