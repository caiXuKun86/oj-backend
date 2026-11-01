package com.kun.oj.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.AdminQuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionAddRequest;
import com.kun.oj.model.dto.request.QuestionUpdateRequest;
import com.kun.oj.model.dto.response.JudgeCaseUploadResponse;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Service
 * @createDate 2026-10-31 10:44:23
 */
public interface AdminQuestionService extends IService<Question> {

    PageResult<QuestionPageVO> queryQuestionPage(AdminQuestionPageRequest questionPageRequest);

    QuestionVO queryQuestionDetail(Long questionId);

    Long addQuestion(QuestionAddRequest questionAddRequest);

    JudgeCaseUploadResponse uploadTestCases(MultipartFile multipartFile, Long questionId);

    boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest,Long questionId);

    Boolean deleteQuestion(Long questionId);
}
