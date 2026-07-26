package com.kun.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kun.oj.model.dto.question.QuestionQueryRequest;
import com.kun.oj.model.entity.QuestionBase;
import org.apache.ibatis.annotations.Param;

/**
* @author Lenovo
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2026-06-12 21:58:20
* @Entity com.kun.oj.model.entity.Question
*/
public interface QuestionBaseMapper extends BaseMapper<QuestionBase> {

    Page<QuestionBase> selectQuestionPage(Page<QuestionBase> page, @Param("query") QuestionQueryRequest query);
}






