package com.kun.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.constant.CommonConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.mapper.QuestionAnswerMapper;
import com.kun.oj.model.dto.questionAnswer.QuestionAnswerQueryRequest;
import com.kun.oj.model.entity.QuestionAnswer;
import com.kun.oj.model.entity.QuestionBase;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.enums.LanguageEnum;
import com.kun.oj.model.vo.QuestionAnswerVO;
import com.kun.oj.service.QuestionAnswerService;
import com.kun.oj.service.QuestionBaseService;
import com.kun.oj.service.UserService;
import com.kun.oj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//import com.kun.oj.utils.SqlUtils;

/**
 * @author Lenovo
 * @description 针对表【question_answer(题目答案)】的数据库操作Service实现
 * @createDate 2026-06-12 21:57:59
 */
@Service
public class QuestionAnswerServiceImpl extends ServiceImpl<QuestionAnswerMapper, QuestionAnswer>
        implements QuestionAnswerService {
    @Resource
    private UserService userService;
    @Resource
    private QuestionBaseService questionBaseService;


    @Override
    public void validQuestionAnswer(QuestionAnswer questionAnswer, boolean add) {
        if (questionAnswer == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long questionId = questionAnswer.getQuestionId();
        Long userId = questionAnswer.getUserId();
        String language = questionAnswer.getLanguage();
        String content = questionAnswer.getContent();


        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(ObjUtil.hasNull(questionId, userId, language), ErrorCode.PARAMS_ERROR);
        }
        if (language != null) {
            LanguageEnum languageEnum = LanguageEnum.getEnumByValue(language);
            if (languageEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "目前不支持该语言");
            }
        }
        if (questionId != null) {
            QuestionBase questionBase = questionBaseService.getById(questionId);
            if (questionBase == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
            }
        }

        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }

    }

    /**
     * 获取查询包装类
     *
     * @param questionAnswerQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionAnswer> getQueryWrapper(QuestionAnswerQueryRequest questionAnswerQueryRequest) {

        QueryWrapper<QuestionAnswer> queryWrapper = new QueryWrapper<>();
        if (questionAnswerQueryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.select("id", "questionId", "userId", "language", "reviewStatus", "reviewMessage", "reviewerId");

        Long questionId = questionAnswerQueryRequest.getQuestionId();

        String userAccount = questionAnswerQueryRequest.getUserAccount();
        String number = questionAnswerQueryRequest.getNumber();
        if (questionId == null && StrUtil.isNotBlank(number)) {
            questionId = questionBaseService.lambdaQuery()
                    .select(QuestionBase::getId)
                    .eq(QuestionBase::getNumber, number)
                    .one()
                    .getId();
        }
        Long userId=null;

        if(StrUtil.isNotBlank(userAccount)){
            userId= userService.lambdaQuery()
                    .select(User::getId)
                    .eq(User::getUserAccount, userAccount)
                    .one()
                    .getId();
        }




        String language = questionAnswerQueryRequest.getLanguage();
        Integer reviewStatus = questionAnswerQueryRequest.getReviewStatus();

        String sortField = questionAnswerQueryRequest.getSortField();
        String sortOrder = questionAnswerQueryRequest.getSortOrder();


        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public QuestionAnswerVO getQuestionAnswerVO(QuestionAnswer questionAnswer, HttpServletRequest request) {
        QuestionAnswerVO questionAnswerVO = new QuestionAnswerVO();
        BeanUtil.copyProperties(questionAnswer, questionAnswerVO);
        Long userId = questionAnswer.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        if (user != null) {
            questionAnswerVO.setUserAccount(user.getUserAccount());
        }
        QuestionBase questionBase = null;
        Long questionId = questionAnswer.getQuestionId();
        if (questionId != null && questionId > 0) {
            questionBase = questionBaseService.getById(userId);
        }
        if (questionBase != null) {
            questionAnswerVO.setTitle(questionBase.getTitle());
            questionAnswerVO.setNumber(questionBase.getNumber());

        }
        return questionAnswerVO;
    }

    @Override
    public Page<QuestionAnswerVO> getQuestionAnswerVOPage(Page<QuestionAnswer> questionAnswerPage, HttpServletRequest request) {
        List<QuestionAnswer> questionAnswerList = questionAnswerPage.getRecords();
        Page<QuestionAnswerVO> questionAnswerVOPage = new Page<>(questionAnswerPage.getCurrent(), questionAnswerPage.getSize(), questionAnswerPage.getTotal());
        if (CollUtil.isEmpty(questionAnswerList)) {
            return questionAnswerVOPage;
        }
        Set<Long> userIds = questionAnswerList.stream().map(QuestionAnswer::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, v -> v));

        Set<Long> questionIds = questionAnswerList.stream().map(QuestionAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBase> questionBaseMap = questionBaseService.listByIds(questionIds).stream().collect(Collectors.toMap(QuestionBase::getId, questionBase -> questionBase));

        List<QuestionAnswerVO> records = questionAnswerList.stream().map(questionAnswer -> {
            QuestionAnswerVO vo = new QuestionAnswerVO();
            BeanUtil.copyProperties(questionAnswer, vo);
            vo.setUserAccount(userMap.get(vo.getUserId()).getUserAccount());
            QuestionBase questionBase = questionBaseMap.get(vo.getQuestionId());
            vo.setTitle(questionBase.getTitle());
            vo.setNumber(questionBase.getNumber());
            return vo;
        }).collect(Collectors.toList());
        questionAnswerVOPage.setRecords(records);
        return questionAnswerVOPage;
    }


}




