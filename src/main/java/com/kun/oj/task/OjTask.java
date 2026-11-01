package com.kun.oj.task;

import cn.hutool.core.collection.CollUtil;
import com.kun.oj.enums.JudgeMessageEnum;
import com.kun.oj.enums.JudgeStatusEnum;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.service.QuestionSubmitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OjTask {

    private final QuestionSubmitService questionSubmitService;
    private final RedissonClient redissonClient;

    private static final String TIMEOUT_LOCK_KEY = "oj:task:judging:timeout:lock";

    /**
     * 每 2 分钟执行一次超时检查，补偿卡死在判题中或排队中的任务
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void handleJudgingTimeout() {
        // 1. 分布式锁控制：防止多实例同时跑批造成锁竞争
        RLock lock = redissonClient.getLock(TIMEOUT_LOCK_KEY);
        boolean acquired = false;
        try {
            // tryLock(waitTime = 0, leaseTime = 60s)
            acquired = lock.tryLock(0, 60, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("其他实例正在处理判题超时任务，跳过本次执行");
                return;
            }

            // 2. 仅查询 ID，防止加载用户大代码段造成 JVM 内存暴涨；orderByAsc 优先处理积压时间最长的记录
            List<Long> timeoutIds = questionSubmitService.lambdaQuery()
                    .select(QuestionSubmit::getId)
                    .in(QuestionSubmit::getStatus, JudgeStatusEnum.JUDGING.getCode(), JudgeStatusEnum.WAITING.getCode())
                    .lt(QuestionSubmit::getCreateTime, LocalDateTime.now().minusMinutes(15L))
                    .orderByAsc(QuestionSubmit::getId)
                    .last("LIMIT 1000")
                    .list()
                    .stream()
                    .map(QuestionSubmit::getId)
                    .collect(Collectors.toList());

            if (CollUtil.isEmpty(timeoutIds)) {
                return;
            }

            // 4. 单条批量 UPDATE + 状态条件兜底（防止并发覆盖已成功的记录）
            boolean success = questionSubmitService.lambdaUpdate()
                    .set(QuestionSubmit::getStatus, JudgeStatusEnum.FAILED.getCode())
                    .set(QuestionSubmit::getMessage, JudgeMessageEnum.SYSTEM_ERROR.getCode())
                    .in(QuestionSubmit::getId, timeoutIds)
                    .in(QuestionSubmit::getStatus, JudgeStatusEnum.JUDGING.getCode(), JudgeStatusEnum.WAITING.getCode())
                    .update();

            log.info("处理超时判题任务完成，扫描到超时记录 {} 条，更新结果: {}", timeoutIds.size(), success);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取判题超时分布式锁被中断", e);
        } catch (Exception e) {
            log.error("处理判题超时任务异常", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}