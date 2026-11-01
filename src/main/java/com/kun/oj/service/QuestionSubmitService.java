package com.kun.oj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.QuestionSubmitPageRequest;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.vo.QuestionSubmitPageVO;
import com.kun.oj.model.vo.QuestionSubmitVO;

/**
 * @author Lenovo
 * @description 针对表【question_submit(题目提交)】的数据库操作Service
 * @createDate 2026-10-31 10:44:24
 */
public interface QuestionSubmitService extends IService<QuestionSubmit> {

    PageResult<QuestionSubmitPageVO> queryQuestionSubmitPage(QuestionSubmitPageRequest questionSubmitQueryRequest);

    QuestionSubmitVO queryQuestionDetail(Long submitId);
}
