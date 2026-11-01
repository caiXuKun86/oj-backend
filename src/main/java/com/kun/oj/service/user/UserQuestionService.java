package com.kun.oj.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.QuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionSearchRequest;
import com.kun.oj.model.dto.request.QuestionSubmitRequest;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Service
 * @createDate 2026-10-31 10:44:23
 */
public interface UserQuestionService extends IService<Question> {
    PageResult<QuestionPageVO> queryQuestionPage(QuestionPageRequest questionPageRequest);

    PageResult<QuestionPageVO> searchQuestionPage(QuestionSearchRequest questionSearchRequest);

    QuestionVO queryQuestionDetail(Long questionId);

    Long submitQuestion(QuestionSubmitRequest questionSubmitRequest);

}
