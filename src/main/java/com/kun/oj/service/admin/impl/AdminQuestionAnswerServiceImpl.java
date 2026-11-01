package com.kun.oj.service.admin.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.ResultCode;
import com.kun.oj.enums.CodeLanguageEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.mapper.QuestionAnswerMapper;
import com.kun.oj.mapper.QuestionMapper;
import com.kun.oj.model.dto.request.QuestionAnswerAddRequest;
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.dto.request.QuestionAnswerUpdateRequest;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;
import com.kun.oj.service.admin.AdminQuestionAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 针对表【question_answer(题目答案)】的后台管理Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionAnswerServiceImpl extends ServiceImpl<QuestionAnswerMapper, QuestionAnswer>
        implements AdminQuestionAnswerService {

    private final QuestionMapper questionMapper;

    @Override
    public Long addQuestionAnswer(QuestionAnswerAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        Long questionId = addRequest.getQuestionId();
        String language = addRequest.getLanguage();
        String content = addRequest.getContent();

        if (questionId == null || StrUtil.isBlank(language) || StrUtil.isBlank(content)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "题目ID、语言和代码内容不能为空");
        }

        // 校验编程语言
        if (CodeLanguageEnum.getByCode(language) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "暂不支持该编程语言");
        }

        // 校验题目是否存在
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }

        // 唯一性校验：每道题目同一种语言只能有一份官方标程
        Long count = this.lambdaQuery()
                .eq(QuestionAnswer::getQuestionId, questionId)
                .eq(QuestionAnswer::getLanguage, language)
                .count();
        if (count > 0) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "该题目已存在该语言的官方答案，请勿重复添加");
        }

        QuestionAnswer questionAnswer = new QuestionAnswer();
        questionAnswer.setQuestionId(questionId);
        questionAnswer.setLanguage(language);
        questionAnswer.setContent(content);

        boolean saved = this.save(questionAnswer);
        if (!saved) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "保存答案失败");
        }
        return questionAnswer.getId();
    }

    @Override
    public Boolean updateQuestionAnswer(QuestionAnswerUpdateRequest updateRequest, Long answerId) {
        if (updateRequest == null || answerId == null || answerId <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        QuestionAnswer oldAnswer = this.getById(answerId);
        if (oldAnswer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "答案不存在");
        }

        String language = updateRequest.getLanguage();
        String content = updateRequest.getContent();

        // 如果修改了编程语言，检查是否支持且防止与同题下其他答案冲突
        if (StrUtil.isNotBlank(language) && !language.equalsIgnoreCase(oldAnswer.getLanguage())) {
            if (CodeLanguageEnum.getByCode(language) == null) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "暂不支持该编程语言");
            }
            Long count = this.lambdaQuery()
                    .eq(QuestionAnswer::getQuestionId, oldAnswer.getQuestionId())
                    .eq(QuestionAnswer::getLanguage, language)
                    .ne(QuestionAnswer::getId, answerId)
                    .count();
            if (count > 0) {
                throw new BusinessException(ResultCode.OPERATION_ERROR, "该题目已存在该语言的答案，不可修改为重复语言");
            }
            oldAnswer.setLanguage(language);
        }

        if (StrUtil.isNotBlank(content)) {
            oldAnswer.setContent(content);
        }

        boolean updated = this.updateById(oldAnswer);
        if (!updated) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "更新答案失败");
        }
        return true;
    }

    @Override
    public Boolean deleteQuestionAnswer(Long answerId) {
        if (answerId == null || answerId <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        QuestionAnswer oldAnswer = this.getById(answerId);
        if (oldAnswer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "答案不存在");
        }
        boolean removed = this.removeById(answerId);
        if (!removed) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "删除答案失败");
        }
        return true;
    }

    @Override
    public PageResult<QuestionPageAnswerVO> queryQuestionAnswerPage(QuestionAnswerPageRequest pageRequest) {
        String language = pageRequest.getLanguage();
        Long questionId = pageRequest.getQuestionId();

        if (StrUtil.isNotBlank(language) && CodeLanguageEnum.getByCode(language) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "暂不支持该编程语言");
        }

        LambdaQueryWrapper<QuestionAnswer> queryWrapper = new LambdaQueryWrapper<QuestionAnswer>()
                .eq(StrUtil.isNotBlank(language), QuestionAnswer::getLanguage, language)
                .eq(questionId != null, QuestionAnswer::getQuestionId, questionId);

        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderBy(true, pageRequest.getIsAsc(), QuestionAnswer::getCreateTime);
        }

        Page<QuestionAnswer> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        return PageResult.of(questionPage, answer -> BeanUtil.copyProperties(answer, QuestionPageAnswerVO.class));
    }

    @Override
    public QuestionAnswerVO queryQuestionAnswerDetail(Long answerId) {
        QuestionAnswer questionAnswer = this.getById(answerId);
        if (questionAnswer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "答案不存在");
        }
        return BeanUtil.copyProperties(questionAnswer, QuestionAnswerVO.class);
    }
}
