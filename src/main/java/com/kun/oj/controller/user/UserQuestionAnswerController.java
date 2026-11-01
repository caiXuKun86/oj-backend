package com.kun.oj.controller.user;

import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;
import com.kun.oj.service.user.UserQuestionAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/question-answer")
public class UserQuestionAnswerController {

    private final UserQuestionAnswerService questionAnswerService;

    @GetMapping("/list")
    public Result<PageResult<QuestionPageAnswerVO>> queryQuestionAnswerPage(QuestionAnswerPageRequest questionAnswerPageRequest) {
        PageResult<QuestionPageAnswerVO> result = questionAnswerService.queryQuestionAnswerPage(questionAnswerPageRequest);
        return ResultUtils.success(result);

    }

    @GetMapping("/{answerId}")
    public Result<QuestionAnswerVO> queryQuestionAnswerDetail(@PathVariable Long answerId) {
        QuestionAnswerVO questionAnswerVO = questionAnswerService.queryQuestionAnswerDetail(answerId);
        return ResultUtils.success(questionAnswerVO);
    }


}
