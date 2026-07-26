package com.kun.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.kun.oj.common.DeleteRequest;
import com.kun.oj.model.dto.question.*;
import com.kun.oj.model.entity.QuestionBase;
import com.kun.oj.model.entity.QuestionExtent;
import com.kun.oj.model.vo.QuestionBaseVO;
import com.kun.oj.model.vo.QuestionVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2026-06-12 21:58:20
*/
public interface QuestionBaseService extends IService<QuestionBase> {





    Long submitQuestion(QuestionSubmitRequest questionSubmitRequest, HttpServletRequest request);

    TestCaseUploadResponse uploadTestCases(MultipartFile multipartFile, Long questionId);

    Long addQuestion(QuestionAddRequest questionAddRequest, HttpServletRequest request);

    QuestionVO getQuestionVOById(Long id);

    Page<QuestionBaseVO> listQuestionVOByPage(QuestionQueryRequest questionQueryRequest, HttpServletRequest request);

    boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest);

    Boolean deleteQuestion(DeleteRequest deleteRequest);


    void validate(QuestionBase questionBase, QuestionExtent questionExtent);

    void reloadCache();
}
