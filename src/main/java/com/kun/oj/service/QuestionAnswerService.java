package com.kun.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.model.dto.questionAnswer.QuestionAnswerQueryRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.vo.QuestionAnswerVO;


import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【question_answer(题目答案)】的数据库操作Service
* @createDate 2026-06-12 21:57:59
*/
public interface QuestionAnswerService extends IService<QuestionAnswer> {

    void validQuestionAnswer(QuestionAnswer questionAnswer, boolean add);

    QueryWrapper<QuestionAnswer> getQueryWrapper(QuestionAnswerQueryRequest questionAnswerQueryRequest);

    QuestionAnswerVO getQuestionAnswerVO(QuestionAnswer questionAnswer, HttpServletRequest request);

    Page<QuestionAnswerVO> getQuestionAnswerVOPage(Page<QuestionAnswer> questionAnswerPage, HttpServletRequest request);
}
