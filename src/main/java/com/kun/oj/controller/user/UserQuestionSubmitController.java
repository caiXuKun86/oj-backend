package com.kun.oj.controller.user;

import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.model.dto.request.QuestionSubmitPageRequest;
import com.kun.oj.model.vo.QuestionSubmitPageVO;
import com.kun.oj.model.vo.QuestionSubmitVO;
import com.kun.oj.service.QuestionSubmitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/question-submit")
@Slf4j
public class UserQuestionSubmitController {

    private final QuestionSubmitService questionSubmitService;

    /**
     * 根据 id 获取
     *
     * @return
     */
    @GetMapping("/{submitId}")
    public Result<QuestionSubmitVO> queryQuestionDetail(@PathVariable Long submitId) {
        QuestionSubmitVO questionSubmitVO = questionSubmitService.queryQuestionDetail(submitId);
        return ResultUtils.success(questionSubmitVO);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @PostMapping("/list")
    public Result<PageResult<QuestionSubmitPageVO>> queryQuestionSubmitPage(@RequestBody QuestionSubmitPageRequest questionSubmitQueryRequest) {
        PageResult<QuestionSubmitPageVO> result = questionSubmitService.queryQuestionSubmitPage(questionSubmitQueryRequest);
        return ResultUtils.success(result);
    }

}
