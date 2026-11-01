package com.kun.oj.controller.admin;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.model.dto.request.QuestionAnswerAddRequest;
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.dto.request.QuestionAnswerUpdateRequest;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;
import com.kun.oj.service.admin.AdminQuestionAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 题目官方答案/标程管理接口（仅管理员）
 */
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/question-answer/admin")
public class AdminQuestionAnswerController {

    private final AdminQuestionAnswerService questionAnswerService;

    /**
     * 分页获取题目答案列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<PageResult<QuestionPageAnswerVO>> queryQuestionAnswerPage(QuestionAnswerPageRequest questionAnswerPageRequest) {
        PageResult<QuestionPageAnswerVO> result = questionAnswerService.queryQuestionAnswerPage(questionAnswerPageRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查询答案详情
     */
    @GetMapping("/{answerId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<QuestionAnswerVO> queryQuestionAnswerDetail(@PathVariable Long answerId) {
        QuestionAnswerVO questionAnswerVO = questionAnswerService.queryQuestionAnswerDetail(answerId);
        return ResultUtils.success(questionAnswerVO);
    }

    /**
     * 上传/新增官方答案
     */
    @PostMapping
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> addQuestionAnswer(@RequestBody QuestionAnswerAddRequest addRequest) {
        Long result = questionAnswerService.addQuestionAnswer(addRequest);
        return ResultUtils.success(result);
    }

    /**
     * 修改官方答案
     */
    @PutMapping("/{answerId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateQuestionAnswer(@RequestBody QuestionAnswerUpdateRequest updateRequest, @PathVariable Long answerId) {
        Boolean result = questionAnswerService.updateQuestionAnswer(updateRequest, answerId);
        return ResultUtils.success(result);
    }

    /**
     * 删除官方答案
     */
    @DeleteMapping("/{answerId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteQuestionAnswer(@PathVariable Long answerId) {
        Boolean result = questionAnswerService.deleteQuestionAnswer(answerId);
        return ResultUtils.success(result);
    }
}
