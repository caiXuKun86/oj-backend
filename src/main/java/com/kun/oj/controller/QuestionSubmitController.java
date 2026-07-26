package com.kun.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kun.oj.common.BaseResponse;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.vo.QuestionSubmitVO;
import com.kun.oj.service.QuestionSubmitService;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/question/submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QuestionSubmitVO questionSubmitVO = questionSubmitService.getQuestionSubmitVO(id,request);
        return ResultUtils.success(questionSubmitVO);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitVOByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
            HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, request));
    }

}
