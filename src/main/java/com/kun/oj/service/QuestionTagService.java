package com.kun.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.model.dto.questionTag.QuestionTagQueryRequest;
import com.kun.oj.model.entity.QuestionTag;
import com.kun.oj.model.vo.TagVO;

import java.util.List;

/**
* @author root
* @description 针对表【question_tag(题目标签)】的数据库操作Service
* @createDate 2026-06-20 19:10:04
*/
public interface QuestionTagService extends IService<QuestionTag> {
    void validQuestionTag(QuestionTag questionTag, boolean add);

    QueryWrapper<QuestionTag> getQueryWrapper(QuestionTagQueryRequest questionTagQueryRequest);



    List<TagVO> getAllTags();

}
