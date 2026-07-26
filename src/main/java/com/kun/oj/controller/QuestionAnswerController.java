package com.kun.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.BaseResponse;
import com.kun.oj.common.DeleteRequest;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.model.dto.questionAnswer.QuestionAnswerAddRequest;
import com.kun.oj.model.dto.questionAnswer.QuestionAnswerQueryRequest;
import com.kun.oj.model.dto.questionAnswer.QuestionAnswerUpdateRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.enums.AnswerReviewStatusEnum;
import com.kun.oj.model.enums.UserRoleEnum;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.service.QuestionAnswerService;
import com.kun.oj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/question/answer")
@Slf4j
public class QuestionAnswerController {

    @Resource
    private QuestionAnswerService questionAnswerService;

    @Resource
    private UserService userService;


    /**
     * 创建
     *
     * @param questionAnswerAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestionAnswer(@RequestBody QuestionAnswerAddRequest questionAnswerAddRequest, HttpServletRequest request) {
        if (questionAnswerAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionAnswer questionAnswer = new QuestionAnswer();
        BeanUtils.copyProperties(questionAnswerAddRequest, questionAnswer);
        User loginUser = userService.getLoginUser(request);
        questionAnswer.setUserId(loginUser.getId());
        questionAnswerService.validQuestionAnswer(questionAnswer, true);

        if(UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())){
            questionAnswer.setReviewerId(loginUser.getId());
            questionAnswer.setReviewStatus(AnswerReviewStatusEnum.PASS.getValue());
            questionAnswer.setReviewMessage("管理员提交答案");
        }

        boolean result = questionAnswerService.save(questionAnswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionAnswerId = questionAnswer.getId();
        return ResultUtils.success(newQuestionAnswerId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionAnswer(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionAnswer oldQuestionAnswer = questionAnswerService.getById(id);
        ThrowUtils.throwIf(oldQuestionAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionAnswer.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionAnswerService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionAnswerUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionAnswer(@RequestBody QuestionAnswerUpdateRequest questionAnswerUpdateRequest) {
        if (questionAnswerUpdateRequest == null || questionAnswerUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionAnswer questionAnswer = new QuestionAnswer();
        BeanUtils.copyProperties(questionAnswerUpdateRequest, questionAnswer);

        // 参数校验
        questionAnswerService.validQuestionAnswer(questionAnswer, false);
        long id = questionAnswerUpdateRequest.getId();
        // 判断是否存在
        QuestionAnswer oldQuestionAnswer = questionAnswerService.getById(id);
        ThrowUtils.throwIf(oldQuestionAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionAnswerService.updateById(questionAnswer);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionAnswerVO> getQuestionAnswerVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionAnswer questionAnswer = questionAnswerService.getById(id);
        if (questionAnswer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionAnswerService.getQuestionAnswerVO(questionAnswer, request));
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionAnswerQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionAnswerVO>> listQuestionAnswerByPage(@RequestBody QuestionAnswerQueryRequest questionAnswerQueryRequest,
                                                                           HttpServletRequest request) {
        long current = questionAnswerQueryRequest.getCurrent();
        long size = questionAnswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<QuestionAnswer> questionAnswerPage = questionAnswerService.page(new Page<>(current, size),
                questionAnswerService.getQueryWrapper(questionAnswerQueryRequest));
        return ResultUtils.success(questionAnswerService.getQuestionAnswerVOPage(questionAnswerPage, request));
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionAnswerQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionAnswerVO>> listQuestionAnswerVOByPage(@RequestBody QuestionAnswerQueryRequest questionAnswerQueryRequest,
            HttpServletRequest request) {
        long current = questionAnswerQueryRequest.getCurrent();
        long size = questionAnswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<QuestionAnswer> questionAnswerPage = questionAnswerService.page(new Page<>(current, size),
                questionAnswerService.getQueryWrapper(questionAnswerQueryRequest));
        return ResultUtils.success(questionAnswerService.getQuestionAnswerVOPage(questionAnswerPage, request));
    }






}
