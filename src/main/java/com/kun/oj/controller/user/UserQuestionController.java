package com.kun.oj.controller.user;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.model.dto.request.QuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionSearchRequest;
import com.kun.oj.model.dto.request.QuestionSubmitRequest;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.service.user.UserQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/question")
public class UserQuestionController {


    private final UserQuestionService questionService;


    /**
     * 分页获取题目列表
     *
     * @param questionPageRequest
     * @return
     */
    @GetMapping("/list")
    public Result<PageResult<QuestionPageVO>> queryQuestionPage(@RequestBody QuestionPageRequest questionPageRequest) {
        PageResult<QuestionPageVO> result = questionService.queryQuestionPage(questionPageRequest);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取题目列表
     *
     * @param questionSearchRequest
     * @return
     */
    @GetMapping("/search")
    public Result<PageResult<QuestionPageVO>> searchQuestionPage(@RequestBody QuestionSearchRequest questionSearchRequest) {
        PageResult<QuestionPageVO> result = questionService.searchQuestionPage(questionSearchRequest);
        return ResultUtils.success(result);
    }


    /**
     * 根据id查询题目,普通用户用
     *
     * @param questionId
     * @return
     */
    @GetMapping("/{questionId}")
    public Result<QuestionVO> queryQuestionDetail(@PathVariable Long questionId) {

        QuestionVO questionVO = questionService.queryQuestionDetail(questionId);
        return ResultUtils.success(questionVO);
    }


    @PostMapping("/submit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> submitQuestion(@RequestBody QuestionSubmitRequest questionSubmitRequest) {
        Long result = questionService.submitQuestion(questionSubmitRequest);
        return ResultUtils.success(result);
    }


}
