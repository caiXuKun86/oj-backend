package com.kun.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.BaseResponse;
import com.kun.oj.common.DeleteRequest;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.model.dto.question.*;
import com.kun.oj.model.vo.QuestionBaseVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.service.QuestionBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@Slf4j
@RequestMapping("/question")
public class QuestionController {

    @Resource
    private QuestionBaseService questionBaseService;




    @PostMapping("/submit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> submitQuestion(@RequestBody QuestionSubmitRequest questionSubmitRequest, HttpServletRequest request) {
        Long result = questionBaseService.submitQuestion(questionSubmitRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 创建题目
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long result = questionBaseService.addQuestion(questionAddRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 删除题目
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = questionBaseService.deleteQuestion(deleteRequest);
        return ResultUtils.success(result);
    }

    /**
     * 更新题目
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = questionBaseService.updateQuestion(questionUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据id查询题目,管理员用
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<QuestionVO> getQuestionById(Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QuestionVO questionVO = questionBaseService.getQuestionVOById(id);
        return ResultUtils.success(questionVO);
    }

    /**
     * 根据id查询题目,普通用户用
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionVO questionVO = questionBaseService.getQuestionVOById(id);
        questionVO.setHiddenJudgeCase(null);
        return ResultUtils.success(questionVO);
    }


    /**
     * 分页获取题目列表
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionBaseVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        Page<QuestionBaseVO> page = questionBaseService.listQuestionVOByPage(questionQueryRequest,request);
        return ResultUtils.success(page);
    }



    @PostMapping("/upload/testCases")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<TestCaseUploadResponse> uploadTestCaseZip(@RequestParam("file") MultipartFile multipartFile, @RequestParam Long questionId) {
        // 1. 基础校验：是否为空
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        TestCaseUploadResponse response = questionBaseService.uploadTestCases(multipartFile, questionId);
        return ResultUtils.success(response);
    }

    @PostMapping("/reload/cache")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> reloadCache(){
        questionBaseService.reloadCache();

        return ResultUtils.success();
    }


}
