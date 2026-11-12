package com.kun.oj.controller.admin;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.model.dto.request.AdminQuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionAddRequest;
import com.kun.oj.model.dto.request.QuestionUpdateRequest;
import com.kun.oj.model.dto.response.JudgeCaseUploadResponse;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.service.admin.AdminQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/question/admin")
public class AdminQuestionController {

    private final AdminQuestionService questionService;


    /**
     * 分页获取题目列表
     *
     * @param questionPageRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/list")
    public Result<PageResult<QuestionPageVO>> queryQuestionPage(AdminQuestionPageRequest questionPageRequest) {
        PageResult<QuestionPageVO> result = questionService.queryQuestionPage(questionPageRequest);
        return ResultUtils.success(result);
    }


    /**
     * 根据id查询题目,普通用户用
     *
     * @param questionId
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/{questionId}")
    public Result<QuestionVO> queryQuestionDetail(@PathVariable Long questionId) {

        QuestionVO questionVO = questionService.queryQuestionDetail(questionId);
        return ResultUtils.success(questionVO);
    }


    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @return
     */

    @PostMapping
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {

        Long result = questionService.addQuestion(questionAddRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/upload/judge-case")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<JudgeCaseUploadResponse> uploadTestCaseZip(@RequestParam("file") MultipartFile multipartFile, @RequestParam Long questionId) {

        JudgeCaseUploadResponse response = questionService.uploadTestCases(multipartFile, questionId);
        return ResultUtils.success(response);
    }

    /**
     * 删除题目
     *
     * @return
     */
    @DeleteMapping("/{questionId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteQuestion(@PathVariable Long questionId) {

        Boolean result = questionService.deleteQuestion(questionId);
        return ResultUtils.success(result);
    }

    /**
     * 更新题目
     *
     * @param questionUpdateRequest
     * @return
     */
    @PutMapping("/{questionId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest, @PathVariable Long questionId) {

        boolean result = questionService.updateQuestion(questionUpdateRequest, questionId);
        return ResultUtils.success(result);
    }


}
