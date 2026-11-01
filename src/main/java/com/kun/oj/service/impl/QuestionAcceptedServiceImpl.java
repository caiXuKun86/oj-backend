package com.kun.oj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.mapper.UserQuestionAcceptedMapper;
import com.kun.oj.model.entity.UserQuestionAccepted;
import com.kun.oj.service.QuestionAcceptedService;
import org.springframework.stereotype.Service;

/**
 * @author Lenovo
 * @description 针对表【user_question_accepted(用户通过题目关联表)】的数据库操作Service实现
 * @createDate 2026-10-31 15:56:59
 */
@Service
public class QuestionAcceptedServiceImpl extends ServiceImpl<UserQuestionAcceptedMapper, UserQuestionAccepted>
        implements QuestionAcceptedService {

}




