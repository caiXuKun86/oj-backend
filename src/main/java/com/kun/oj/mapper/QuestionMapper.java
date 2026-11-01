package com.kun.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kun.oj.model.entity.Question;
import org.apache.ibatis.annotations.Param;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Mapper
 * @createDate 2026-10-31 10:44:23
 * @Entity com.kun.oj.model.entity.Question
 */
public interface QuestionMapper extends BaseMapper<Question> {

    Page<Question> searchQuestionByScore(@Param("page") Page<Question> page, @Param("keyword") String keyword);
}




