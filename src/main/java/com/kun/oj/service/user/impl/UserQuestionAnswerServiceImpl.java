package com.kun.oj.service.user.impl;

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
import com.kun.oj.model.dto.request.QuestionAnswerPageRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.model.vo.QuestionPageAnswerVO;
import com.kun.oj.service.user.UserQuestionAnswerService;
import org.springframework.stereotype.Service;

/**
 * @author Lenovo
 * @description 针对表【question_answer(题目答案)】的数据库操作Service实现
 * @createDate 2026-10-31 10:44:23
 */
@Service
public class UserQuestionAnswerServiceImpl extends ServiceImpl<QuestionAnswerMapper, QuestionAnswer>
        implements UserQuestionAnswerService {

    @Override
    public PageResult<QuestionPageAnswerVO> queryQuestionAnswerPage(QuestionAnswerPageRequest pageRequest) {
        String language = pageRequest.getLanguage();
        Long questionId = pageRequest.getQuestionId();


        if (StrUtil.isNotBlank(language) && CodeLanguageEnum.getByCode(language) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "暂不支持该语言");
        }

        LambdaQueryWrapper<QuestionAnswer> queryWrapper = new LambdaQueryWrapper<QuestionAnswer>()
                .eq(StrUtil.isNotBlank(language), QuestionAnswer::getLanguage, language)
                .eq(questionId != null, QuestionAnswer::getQuestionId, questionId);

        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderBy(true, pageRequest.getIsAsc(), QuestionAnswer::getCreateTime);
        }

        Page<QuestionAnswer> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        return PageResult.of(questionPage, questionAnswer -> BeanUtil.copyProperties(questionAnswer, QuestionPageAnswerVO.class));

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




