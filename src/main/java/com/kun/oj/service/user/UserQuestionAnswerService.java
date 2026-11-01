package com.kun.oj.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;

/**
 * @author Lenovo
 * @description 针对表【question_answer(题目答案)】的数据库操作Service
 * @createDate 2026-10-31 10:44:23
 */
public interface UserQuestionAnswerService extends IService<QuestionAnswer> {

    PageResult<QuestionPageAnswerVO> queryQuestionAnswerPage(QuestionAnswerPageRequest questionAnswerPageRequest);

    QuestionAnswerVO queryQuestionAnswerDetail(Long answerId);
}
