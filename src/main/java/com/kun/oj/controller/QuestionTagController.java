package com.kun.oj.controller;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.BaseResponse;
import com.kun.oj.common.DeleteRequest;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.model.dto.questionTag.QuestionTagAddRequest;
import com.kun.oj.model.dto.questionTag.QuestionTagUpdateRequest;
import com.kun.oj.model.entity.QuestionTag;
import com.kun.oj.model.vo.TagVO;
import com.kun.oj.service.QuestionTagService;
import com.kun.oj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/question/tag")
@Slf4j
public class QuestionTagController {

    @Resource
    private QuestionTagService questionTagService;
    @Resource
    private UserService userService;

    /**
     * 创建
     *
     * @param questionTagAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionTag(@RequestBody QuestionTagAddRequest questionTagAddRequest, HttpServletRequest request) {
        if (questionTagAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionTag questionTag = new QuestionTag();
        BeanUtils.copyProperties(questionTagAddRequest, questionTag);
        questionTag.setUserId(userService.getLoginUser(request).getId());
        questionTagService.validQuestionTag(questionTag, true);


        boolean result = questionTagService.save(questionTag);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionTagId = questionTag.getId();
        return ResultUtils.success(newQuestionTagId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionTag(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionTag oldQuestionTag = questionTagService.getById(id);
        ThrowUtils.throwIf(oldQuestionTag == null, ErrorCode.NOT_FOUND_ERROR);
        if (oldQuestionTag.getParentId() == 0) {
            boolean exists = questionTagService.lambdaQuery()
                    .eq(QuestionTag::getParentId, id)
                    .exists();
            if (exists) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "必须先删除所有子标签");
            }
        }
        boolean b = questionTagService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionTagUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionTag(@RequestBody QuestionTagUpdateRequest questionTagUpdateRequest) {
        if (questionTagUpdateRequest == null || questionTagUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionTag questionTag = new QuestionTag();
        BeanUtils.copyProperties(questionTagUpdateRequest, questionTag);

        // 参数校验
        questionTagService.validQuestionTag(questionTag, false);
        long id = questionTagUpdateRequest.getId();
        // 判断是否存在
        QuestionTag oldQuestionTag = questionTagService.getById(id);
        ThrowUtils.throwIf(oldQuestionTag == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionTagService.updateById(questionTag);
        return ResultUtils.success(result);
    }



    @GetMapping("/all")
    public BaseResponse<List<TagVO>> getAllTags(){
        List<TagVO> tagVOList =  questionTagService.getAllTags();
        return ResultUtils.success(tagVOList);
    }

}
