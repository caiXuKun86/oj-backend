package com.kun.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.vo.QuestionSubmitVO;


import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2026-06-12 21:58:27
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {

    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    QuestionSubmitVO getQuestionSubmitVO(Long  id, HttpServletRequest request);

    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, HttpServletRequest request);
}
