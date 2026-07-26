package com.kun.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.constant.CommonConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.mapper.QuestionSubmitMapper;
import com.kun.oj.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.vo.QuestionSubmitVO;
import com.kun.oj.service.QuestionSubmitService;
import com.kun.oj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2026-06-12 21:58:27
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    /**
     * 获取查询包装类
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.select(
                "id",
                "title",
                "number",
                "language",
                "status",
                "userAccount",
                "message"
        );
        String language = questionSubmitQueryRequest.getLanguage();
        String number = questionSubmitQueryRequest.getNumber();
        String title = questionSubmitQueryRequest.getTitle();
        String userAccount = questionSubmitQueryRequest.getUserAccount();
        Integer status = questionSubmitQueryRequest.getStatus();
        String message = questionSubmitQueryRequest.getMessage();

        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();


        queryWrapper.eq(ObjectUtils.isNotEmpty(number), "number", number);
        queryWrapper.eq(ObjectUtils.isNotEmpty(message), "message", message);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userAccount), "userAccount", userAccount);
        queryWrapper.eq(ObjectUtils.isNotEmpty(language), "language", language);
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public QuestionSubmitVO getQuestionSubmitVO(Long id, HttpServletRequest request) {
        QuestionSubmit questionSubmit = this.getById(id);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return QuestionSubmitVO.objToVo(questionSubmit);
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, HttpServletRequest request) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollUtil.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(QuestionSubmitVO::objToVo)
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;

    }

}




