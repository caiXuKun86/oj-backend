package com.kun.oj.service;


import com.kun.oj.model.vo.QuestionSubmitVO;

public interface JudgeService {

    QuestionSubmitVO doJudge(long questionSubmitId);
}
