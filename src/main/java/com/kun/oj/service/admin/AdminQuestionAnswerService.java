package com.kun.oj.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.QuestionAnswerAddRequest;
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.dto.request.QuestionAnswerUpdateRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;

/**
 * 针对表【question_answer(题目答案)】的后台管理Service
 */
public interface AdminQuestionAnswerService extends IService<QuestionAnswer> {

    /**
     * 新增/上传官方答案
     */
    Long addQuestionAnswer(QuestionAnswerAddRequest addRequest);

    /**
     * 修改官方答案
     */
    Boolean updateQuestionAnswer(QuestionAnswerUpdateRequest updateRequest, Long answerId);

    /**
     * 删除官方答案
     */
    Boolean deleteQuestionAnswer(Long answerId);

    /**
     * 分页查询官方答案列表
     */
    PageResult<QuestionPageAnswerVO> queryQuestionAnswerPage(QuestionAnswerPageRequest pageRequest);

    /**
     * 查询官方答案详情
     */
    QuestionAnswerVO queryQuestionAnswerDetail(Long answerId);
}
