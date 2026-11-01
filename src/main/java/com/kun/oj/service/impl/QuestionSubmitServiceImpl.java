package com.kun.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.ResultCode;
import com.kun.oj.enums.CodeLanguageEnum;
import com.kun.oj.enums.JudgeMessageEnum;
import com.kun.oj.enums.JudgeStatusEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.mapper.QuestionSubmitMapper;
import com.kun.oj.model.dto.request.QuestionSubmitPageRequest;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.vo.QuestionSubmitPageVO;
import com.kun.oj.model.vo.QuestionSubmitVO;
import com.kun.oj.service.QuestionSubmitService;
import org.springframework.stereotype.Service;

/**
 * @author Lenovo
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2026-10-31 10:44:24
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Override
    public PageResult<QuestionSubmitPageVO> queryQuestionSubmitPage(QuestionSubmitPageRequest pageRequest) {

        String language = pageRequest.getLanguage();
        String message = pageRequest.getMessage();
        Integer status = pageRequest.getStatus();
        Long questionId = pageRequest.getQuestionId();
        String number = pageRequest.getNumber();
        Long userId = pageRequest.getUserId();


        if (StrUtil.isNotBlank(language) && CodeLanguageEnum.getByCode(language) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "暂不支持该语言");
        }
        if (StrUtil.isNotBlank(message) && JudgeMessageEnum.getByCode(message) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "测评信息不存在");
        }
        if (status != null && JudgeStatusEnum.getByCode(status) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "测评状态不存在");
        }
        LambdaQueryWrapper<QuestionSubmit> queryWrapper = new LambdaQueryWrapper<QuestionSubmit>()
                .eq(StrUtil.isNotBlank(language), QuestionSubmit::getLanguage, language)
                .eq(StrUtil.isNotBlank(message), QuestionSubmit::getMessage, message)
                .eq(StrUtil.isNotBlank(number), QuestionSubmit::getNumber, number)
                .eq(status != null, QuestionSubmit::getStatus, status)
                .eq(questionId != null, QuestionSubmit::getQuestionId, questionId)
                .eq(userId != null, QuestionSubmit::getUserId, userId);

        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderBy(true, pageRequest.getIsAsc(), QuestionSubmit::getCreateTime);
        }
        Page<QuestionSubmit> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        return PageResult.of(questionPage, questionSubmit -> BeanUtil.copyProperties(questionSubmit, QuestionSubmitPageVO.class));


    }

    @Override
    public QuestionSubmitVO queryQuestionDetail(Long submitId) {
        QuestionSubmit questionSubmit = this.getById(submitId);
        if (questionSubmit == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        return BeanUtil.copyProperties(questionSubmit, QuestionSubmitVO.class);
    }
}




