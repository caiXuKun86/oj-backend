package com.kun.oj.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.ResultCode;
import com.kun.oj.constant.RedisConstants;
import com.kun.oj.enums.CodeLanguageEnum;
import com.kun.oj.enums.JudgeStatusEnum;
import com.kun.oj.enums.QuestionDifficultyEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.mapper.QuestionMapper;
import com.kun.oj.mapper.QuestionSubmitMapper;
import com.kun.oj.mapper.UserMapper;
import com.kun.oj.mapper.UserQuestionAcceptedMapper;
import com.kun.oj.model.dto.request.QuestionPageRequest;
import com.kun.oj.model.dto.request.QuestionSearchRequest;
import com.kun.oj.model.dto.request.QuestionSubmitRequest;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.entity.UserQuestionAccepted;
import com.kun.oj.model.vo.QuestionPageVO;
import com.kun.oj.model.vo.QuestionVO;
import com.kun.oj.service.JudgeService;
import com.kun.oj.service.user.UserQuestionService;
import com.kun.oj.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2026-10-31 10:44:23
 */
@Service
@RequiredArgsConstructor
public class UserQuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements UserQuestionService {
    private final StringRedisTemplate stringRedisTemplate;
    private final UserQuestionAcceptedMapper acceptedMapper;
    private final RedissonClient redissonClient;
    private final QuestionSubmitMapper submitMapper;
    private final UserMapper userMapper;
    private final JudgeService judgeService;
    private final Executor judgeExecutor;

    @Override
    public PageResult<QuestionPageVO> queryQuestionPage(QuestionPageRequest pageRequest) {
        String difficulty = pageRequest.getDifficulty();
        Boolean isAccept = pageRequest.getIsAccept();
        if (StrUtil.isNotBlank(difficulty) && QuestionDifficultyEnum.getByCode(difficulty) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "难度类型不存在");
        }
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<Question>()
                .eq(StrUtil.isNotBlank(difficulty), Question::getDifficulty, difficulty);


        Long userId = UserContext.getUserId();

        if (isAccept != null) {
            if (userId == null) {
                // 未登录用户：若筛选已通过则无结果，若筛选未通过则无需过滤全部展示
                if (Boolean.TRUE.equals(isAccept)) {
                    queryWrapper.apply("1=2");
                }
            } else {
                String existsSql = String.format("select 1 from user_question_accepted uqa where uqa.question_id = question.id and uqa.user_id = %d", userId);
                if (isAccept) {
                    // 已通过：EXISTS
                    queryWrapper.exists(existsSql);
                } else {
                    // 未通过：NOT EXISTS
                    queryWrapper.notExists(existsSql);
                }
            }
        }
        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderByAsc(true, Question::getCreateTime);
        }

        Page<Question> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        List<Question> records = questionPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageResult.of(Collections.emptyList(), pageRequest.getPageNum(), pageRequest.getPageSize(), 0L);
        }
        List<Long> questionIds = records.stream().map(Question::getId).collect(Collectors.toList());

        final Set<Long> acceptSet = (userId != null && CollUtil.isNotEmpty(questionIds))
                ? acceptedMapper.selectObjs(new LambdaQueryWrapper<UserQuestionAccepted>()
                                .select(UserQuestionAccepted::getQuestionId)
                                .eq(UserQuestionAccepted::getUserId, userId)
                                .in(UserQuestionAccepted::getQuestionId, questionIds))
                        .stream().map(obj -> (Long) obj).collect(Collectors.toSet())
                : Collections.emptySet();

        List<QuestionPageVO> voList = records.stream().map(q -> {
            QuestionPageVO vo = new QuestionPageVO();
            BeanUtils.copyProperties(q, vo);
            Integer acceptedNum = q.getAcceptedNum();
            Integer submitNum = q.getSubmitNum();
            BigDecimal passRate;
            if (submitNum == null || submitNum == 0) {
                // 没人提交过，通过率为 0.00
                passRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                int safeAccepted = (acceptedNum == null) ? 0 : acceptedNum;
                // 公式：(acceptedNum * 100) / submitNum，四舍五入保留两位小数
                passRate = BigDecimal.valueOf(safeAccepted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(submitNum), 2, RoundingMode.HALF_UP);
            }
            vo.setPassRate(passRate);

            // 判断这道题是否在通过集合里
            vo.setIsAccept(acceptSet.contains(q.getId()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, pageRequest.getPageNum(), pageRequest.getPageSize(), questionPage.getTotal());


    }

    @Override
    public PageResult<QuestionPageVO> searchQuestionPage(QuestionSearchRequest pageRequest) {
        String keyword = pageRequest.getKeyword();


        Long userId = UserContext.getUserId();

        Page<Question> questionPage = baseMapper.searchQuestionByScore(pageRequest.toPage(), keyword);

        List<Question> records = questionPage.getRecords();

        if (CollUtil.isEmpty(records)) {
            return PageResult.of(Collections.emptyList(), pageRequest.getPageNum(), pageRequest.getPageSize(), 0L);
        }
        List<Long> questionIds = records.stream().map(Question::getId).collect(Collectors.toList());

        final Set<Long> acceptSet = (userId != null && CollUtil.isNotEmpty(questionIds))
                ? acceptedMapper.selectObjs(new LambdaQueryWrapper<UserQuestionAccepted>()
                                .select(UserQuestionAccepted::getQuestionId)
                                .eq(UserQuestionAccepted::getUserId, userId)
                                .in(UserQuestionAccepted::getQuestionId, questionIds))
                        .stream().map(obj -> (Long) obj).collect(Collectors.toSet())
                : Collections.emptySet();

        List<QuestionPageVO> voList = records.stream().map(q -> {
            QuestionPageVO vo = new QuestionPageVO();
            BeanUtils.copyProperties(q, vo);
            Integer acceptedNum = q.getAcceptedNum();
            Integer submitNum = q.getSubmitNum();
            BigDecimal passRate;
            if (submitNum == null || submitNum == 0) {
                // 没人提交过，通过率为 0.00
                passRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                int safeAccepted = (acceptedNum == null) ? 0 : acceptedNum;
                // 公式：(acceptedNum * 100) / submitNum，四舍五入保留两位小数
                passRate = BigDecimal.valueOf(safeAccepted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(submitNum), 2, RoundingMode.HALF_UP);
            }
            vo.setPassRate(passRate);

            // 判断这道题是否在通过集合里
            vo.setIsAccept(acceptSet.contains(q.getId()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, pageRequest.getPageNum(), pageRequest.getPageSize(), questionPage.getTotal());

    }

    @Override
    public QuestionVO queryQuestionDetail(Long questionId) {
        String redisKey = String.format(RedisConstants.CACHE_QUESTION_INFO, questionId);
        String questionStr = stringRedisTemplate.opsForValue().get(redisKey);
        if (questionStr != null) {
            if (StrUtil.isBlank(questionStr)) {
                throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");

            }
            return JSONUtil.toBean(questionStr, QuestionVO.class);
        }
        String lockKey = String.format(RedisConstants.CACHE_LOCK_QUESTION_INFO, questionId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(2, TimeUnit.SECONDS);
            if (!isLocked) {
                // 没抢到锁（排队超过2秒），兜底最后查一次缓存，查不到就快速失败降级
                questionStr = stringRedisTemplate.opsForValue().get(redisKey);
                if (StrUtil.isNotBlank(questionStr)) {
                    return JSONUtil.toBean(questionStr, QuestionVO.class);
                }
                throw new BusinessException(ResultCode.OPERATION_ERROR, "系统繁忙，请稍后重试");
            }
            questionStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (questionStr != null) {
                if (StrUtil.isBlank(questionStr)) {
                    throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");

                }
                return JSONUtil.toBean(questionStr, QuestionVO.class);
            }
            Question question = this.getById(questionId);
            if (question == null) {
                stringRedisTemplate.opsForValue().set(redisKey, "", 30, TimeUnit.SECONDS);
                throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
            }

            QuestionVO questionVO = BeanUtil.copyProperties(question, QuestionVO.class);
            stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(questionVO), 2, TimeUnit.HOURS);
            return questionVO;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Long submitQuestion(QuestionSubmitRequest questionSubmitRequest) {
        String language = questionSubmitRequest.getLanguage();
        String code = questionSubmitRequest.getCode();
        Long questionId = questionSubmitRequest.getQuestionId();

        ThrowUtils.throwIf(ObjUtil.hasNull(language, questionId), ResultCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(code), ResultCode.PARAMS_ERROR);
        CodeLanguageEnum languageEnum = CodeLanguageEnum.getByCode(language);
        if (languageEnum == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "目前不支持该语言");
        }
        if (code.length() > 8192) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "代码内容过长");
        }
        Question question = this.getById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目不存在");
        }
        Long userId = UserContext.getUserId();
        String lockKey = String.format(RedisConstants.LOCK_QUESTION_SUBMIT, questionId, userId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean b = lock.tryLock();
            if (!b) {
                throw new BusinessException(ResultCode.OPERATION_ERROR, "请勿重复提交");
            }
            Long count = submitMapper.selectCount(new LambdaQueryWrapper<QuestionSubmit>()
                    .eq(QuestionSubmit::getQuestionId, questionId)
                    .eq(QuestionSubmit::getUserId, userId)
                    .in(QuestionSubmit::getStatus, JudgeStatusEnum.WAITING.getCode(), JudgeStatusEnum.JUDGING.getCode())
            );
            ThrowUtils.throwIf(count > 0, ResultCode.OPERATION_ERROR, "你已有待处理的提交记录,请勿重复提交");

            QuestionSubmit questionSubmit = new QuestionSubmit();
            questionSubmit.setLanguage(language);
            questionSubmit.setCode(code);
            questionSubmit.setJudgeInfo("{}");
            questionSubmit.setStatus(JudgeStatusEnum.WAITING.getCode());
            questionSubmit.setQuestionId(questionId);
            questionSubmit.setUserId(userId);
            questionSubmit.setNumber(question.getNumber());
            questionSubmit.setTitle(question.getTitle());
            questionSubmit.setUserId(userId);
            questionSubmit.setUsername(userMapper.selectById(userId).getUsername());

            int result = submitMapper.insert(questionSubmit);
            ThrowUtils.throwIf(result < 0, ResultCode.OPERATION_ERROR, "提交失败");
            Long submitId = questionSubmit.getId();
            CompletableFuture.runAsync(() -> {
                judgeService.doJudge(submitId);
            }, judgeExecutor);
            return submitId;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}




