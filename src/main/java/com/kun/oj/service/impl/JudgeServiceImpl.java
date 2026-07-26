package com.kun.oj.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.manage.cos.CosManager;
import com.kun.oj.model.codesandbox.ExecuteCodeRequest;
import com.kun.oj.model.codesandbox.ExecuteCodeResponse;
import com.kun.oj.model.codesandbox.OutputInfo;
import com.kun.oj.model.dto.question.HiddenJudgeCase;
import com.kun.oj.model.dto.question.JudgeInfo;
import com.kun.oj.model.entity.QuestionBase;
import com.kun.oj.model.entity.QuestionExtent;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.model.enums.JudgeMessageEnum;
import com.kun.oj.model.enums.JudgeStatusEnum;
import com.kun.oj.model.enums.LanguageEnum;
import com.kun.oj.model.vo.QuestionSubmitVO;
import com.kun.oj.sandbox.CodeSandbox;
import com.kun.oj.sandbox.CodeSandboxFactory;
import com.kun.oj.service.JudgeService;
import com.kun.oj.service.QuestionBaseService;
import com.kun.oj.service.QuestionExtentService;
import com.kun.oj.service.QuestionSubmitService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service; // 添加此引入
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service // 确保被 Spring 扫描到
public class JudgeServiceImpl implements JudgeService {


    @Resource
    private QuestionBaseService questionBaseService;
    @Resource
    private CosManager cosManager;
    @Resource
    private QuestionSubmitService questionSubmitService;
    @Resource
    private QuestionExtentService questionExtentService;
    @Resource
    private CodeSandboxFactory codeSandboxFactory;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Value("${oj.path.testcase}")
    private String globalTestcasePath;
    @Value("${oj.path.result}")
    private  String globalResultPath;


    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public QuestionSubmitVO doJudge(long questionSubmitId) {
        // 1. 获取提交与题目信息
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }

        Long questionId = questionSubmit.getQuestionId();
        QuestionBase questionBase = questionBaseService.getById(questionId);
        QuestionExtent questionExtent = questionExtentService.getById(questionId);
        if (questionBase == null || questionExtent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目信息不存在");
        }
        // 2. 上传测试用例
        // 2.1 先进行一次锁外的轻量级检查
        HiddenJudgeCase judgeCase = JSONUtil.toBean(questionExtent.getHiddenJudgeCase(), HiddenJudgeCase.class);
        String targetVersion = judgeCase.getTestcaseVersion();
        String testcasePath = globalTestcasePath + File.separator + questionId;
        File testcaseDir = FileUtil.mkdir(testcasePath);
        File versionFile = new File(testcaseDir, ".version");

        boolean needDownload = true;
        if (versionFile.exists()) {
            String localVersion = FileUtil.readUtf8String(versionFile);
            if (targetVersion.equals(localVersion.trim())) {
                needDownload = false;
            }
        }

        // 2.2 如果需要更新，获取分布式锁
        if (needDownload) {
            RLock lock = redissonClient.getLock("lock:judge:testcase:" + questionId);
            try {
                // 关键改动 1：等待最多 30 秒获取锁，加锁后超时 60 秒自动释放，防止直接抛错
                boolean acquired = lock.tryLock(30, 60, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "等待测试用例同步超时");
                }

                // 关键改动 2：二次检查（Double-Check）
                if (versionFile.exists() && targetVersion.equals(FileUtil.readUtf8String(versionFile).trim())) {
                    needDownload = false;
                }

                // 3. 执行真正的更新逻辑
                if (needDownload) {
                    log.info("开始同步题目 {} 的测试用例，目标版本: {}", questionId, targetVersion);

                    // 关键改动 3：下载到临时目录，避免直接删除旧文件破坏正在运行的判题任务
                    File tempDir = FileUtil.mkdir(globalTestcasePath + File.separator + questionId + "_temp_" + System.currentTimeMillis());
                    File zipFile = new File(tempDir, "testcase.zip");

                    try {
                        String cosKey = judgeCase.getKey();
                        cosManager.getObject(cosKey, zipFile);

                        // 解压到临时目录
                        ZipUtil.unzip(zipFile, tempDir, StandardCharsets.UTF_8);
                        FileUtil.del(zipFile); // 解压完立刻删 zip

                        // 写入版本号
                        File tempVersionFile = new File(tempDir, ".version");
                        FileUtil.writeUtf8String(targetVersion, tempVersionFile);

                        // 将临时目录中的文件覆盖覆盖到正式目录
                        FileUtil.copyContent(tempDir, testcaseDir, true);

                    } catch (Exception e) {
                        log.error("测试用例同步失败, questionId: {}", questionId, e);
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "测试用例同步失败");
                    } finally {
                        // 清理临时目录
                        FileUtil.del(tempDir);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取锁被中断");
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }


        // 3. 更新提交状态为“判题中”,并更新题目提交数量
        if (!questionSubmit.getStatus().equals(JudgeStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中，请勿重复提交");
        }
        boolean update = questionSubmitService.lambdaUpdate()
                .set(questionSubmit.getJudgeInfo() != null, QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(questionSubmit.getJudgeInfo()))
                .set(QuestionSubmit::getStatus, JudgeStatusEnum.RUNNING.getValue())
                .eq(QuestionSubmit::getId, questionSubmitId)
                .eq(QuestionSubmit::getStatus, JudgeStatusEnum.WAITING.getValue())
                .update();
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题状态更新失败");
        }
        questionBaseService.lambdaUpdate()
                .setSql("submitNum = submitNum + " + 1)
                .eq(QuestionBase::getId, questionId)
                .update();


        String resultParentPath = globalResultPath + File.separator + UUID.randomUUID();
        try {
            // 4. 构造请求调用代码沙箱
            Integer count = judgeCase.getCount();
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(questionSubmit.getCode())
                    .language(questionSubmit.getLanguage())
                    .testcaseParentPath(testcasePath)
                    .resultParentPath(resultParentPath)
                    .count(count)
                    .timeLimit(questionBase.getTimeLimit())
                    .memoryLimit(questionBase.getMemoryLimit() * 1024)
                    .build();
            try {
                checkBeforeExec(executeCodeRequest);
            } catch (Exception e) {
                log.error("参数异常{}", e.getMessage());
                return saveJudgeResult(questionSubmitId, null, JudgeStatusEnum.FAILED.getValue());
            }
            ExecuteCodeResponse executeCodeResponse = null;
            try {
                String language = executeCodeRequest.getLanguage();
                CodeSandbox codeSandbox = codeSandboxFactory.getSandbox(language);
                executeCodeResponse = codeSandbox.execute(executeCodeRequest);
            } catch (Exception e) {
                log.error("远程调用沙箱失败{}", e.getMessage());
            }

            // 5. 根据沙箱执行结果，核对预期输出
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMemory(0L);
            judgeInfo.setTime(0L);
            // 如果沙箱本身执行失败（例如调用超时、系统错误）
            if (executeCodeResponse == null) {
                // 这里你原来的逻辑是直接 return 原生 submitVO，建议统一走下面的最终保存逻辑
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getValue());
                return saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getValue());
            }
            //如果沙箱执行失败或者没有得到输出结果
            List<OutputInfo> outputInfoList = executeCodeResponse.getOutputInfoList();
            if (!executeCodeResponse.getStatus().equals(JudgeStatusEnum.SUCCEED.getValue()) || outputInfoList == null || outputInfoList.isEmpty()) {
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getValue());
                return saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getValue());
            }
            // 5. 遍历验证结果
            long maxTime = 0;
            long maxMemory = 0;
            String finalMessage = JudgeMessageEnum.ACCEPTED.getValue(); // 默认全对

            List<OutputInfo> outputList = new ArrayList<>(count + 1);
            try {
                for (int i = 1; i <= count; i++) {
                    //期望输出用例路径和用户输出用例路径
                    String expectedOutputPath = testcasePath + File.separator + i + ".out";
                    String resultPath = resultParentPath + File.separator + "output_" + i + ".txt";
                    OutputInfo outputInfo = outputInfoList.get(i - 1);
                    outputList.add(outputInfo);

                    // 记录最大时间和内存 (替换为取 Max)
                    maxTime = Math.max(maxTime, outputInfo.getTime() == null ? 0 : outputInfo.getTime());
                    maxMemory = Math.max(maxMemory, outputInfo.getMemory() == null ? 0 : outputInfo.getMemory());


                    // 如果某个用例自身运行报错 (如超出内存、运行时异常)
                    if (StrUtil.isNotBlank(outputInfo.getMessage())) {
                        if (finalMessage.equals(JudgeMessageEnum.ACCEPTED.getValue())) {
                            finalMessage = outputInfo.getMessage();
                        }
                        continue;
                    }

                    // 对比输出 (加入 trim 防止空格回车误判)
                    if (!isFileContentEqualLenient(expectedOutputPath, resultPath)) {
                        finalMessage = JudgeMessageEnum.WRONG_ANSWER.getValue();
                        outputInfo.setMessage(JudgeMessageEnum.WRONG_ANSWER.getValue());
                    }

                }
                judgeInfo.setOutputList(outputList);
                judgeInfo.setMessage(finalMessage);
                judgeInfo.setTime(maxTime);
                judgeInfo.setMemory(maxMemory);
                // 6. 更新数据库最终结果
                if (finalMessage.equals(JudgeMessageEnum.ACCEPTED.getValue())) {
                    //更新通过数
                    questionBaseService.lambdaUpdate()
                            .setSql("acceptedNum = acceptedNum + " + 1)
                            .eq(QuestionBase::getId, questionId)
                            .update();
                    //存入用户通过题目列表
                    String key = "user:ac:" + questionSubmit.getUserId();
                    stringRedisTemplate.opsForSet().add(key, questionId.toString());
                }

                return saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.SUCCEED.getValue());
            } catch (Exception e) {
                log.error("判题异常" + e.getMessage());
                judgeInfo.setTime(0L);
                judgeInfo.setMemory(0L);
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getValue());
                judgeInfo.setOutputList(null);
                return saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getValue());
            }


        } finally {
            if (resultParentPath != null) {
                boolean del = FileUtil.del(resultParentPath);
                if (!del) {
                    log.error("清理本地用户代码目录失败，可能存在权限问题或残留占用，路径: {}", resultParentPath);
                } else {
                    log.info("成功清理本地用户代码目录: {}", resultParentPath);
                }
            }

        }
    }

    private void checkBeforeExec(ExecuteCodeRequest executeCodeRequest) {
        //1.校验参数
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        Long timeLimit = executeCodeRequest.getTimeLimit();
        Long memoryLimit = executeCodeRequest.getMemoryLimit();
        String testcaseParentPath = executeCodeRequest.getTestcaseParentPath();
        String userOutputParentPath = executeCodeRequest.getResultParentPath();
        Integer count = executeCodeRequest.getCount();
        ThrowUtils.throwIf(StrUtil.hasBlank(code, language, testcaseParentPath, userOutputParentPath), ErrorCode.PARAMS_ERROR);
        if (LanguageEnum.getEnumByValue(language) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂时不支持该语言");
        }
        if (count == null || count < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例错误");
        }

        if (timeLimit == null) {
            executeCodeRequest.setTimeLimit(1000L);
        }
        if (memoryLimit == null) {
            executeCodeRequest.setMemoryLimit(64 * 1024 * 1024L);
        }
    }

    /**
     * 抽取公共方法：保存判题最终结果
     */
    private QuestionSubmitVO saveJudgeResult(long questionSubmitId, JudgeInfo judgeInfo, int status) {
        boolean update = questionSubmitService.lambdaUpdate()
                .set(judgeInfo != null, QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(judgeInfo))
                .set(QuestionSubmit::getStatus, status)
                .set(QuestionSubmit::getMessage,judgeInfo.getMessage())
                .eq(QuestionSubmit::getId, questionSubmitId)
                .eq(QuestionSubmit::getStatus, JudgeStatusEnum.RUNNING.getValue())
                .update();
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新判题状态失败");
        }
        return QuestionSubmitVO.objToVo(questionSubmitService.getById(questionSubmitId));
    }


    /**
     * 宽松对比两个文件的内容（忽略行尾空格和空行）
     */
    public boolean isFileContentEqualLenient(String userOutputPath, String expectedOutputPath) {
        if (!FileUtil.exist(userOutputPath)) {
            return false;
        }

        if (!FileUtil.exist(expectedOutputPath)) {
            return false;
        }

        try (BufferedReader userReader = new BufferedReader(new FileReader(userOutputPath));
             BufferedReader expectedReader = new BufferedReader(new FileReader(expectedOutputPath))) {

            String userLine, expectedLine;

            while (true) {
                userLine = getNextValidLine(userReader);
                expectedLine = getNextValidLine(expectedReader);

                // 如果两个文件同时读完，说明完全匹配
                if (userLine == null && expectedLine == null) {
                    return true;
                }

                // 如果一个读完了一个没读完，或者两行内容不同，说明不匹配
                if (userLine == null || expectedLine == null || !userLine.equals(expectedLine)) {
                    return false;
                }
            }
        } catch (IOException e) {
            log.error("读取文件比对时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 辅助方法：获取下一行有效内容，自动去除行末尾的空格
     */
    private String getNextValidLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            // 去除当前行右侧的空白字符（保留左侧和中间的缩进空格）
            line = StrUtil.trimEnd(line);
            if (!line.isEmpty()) {
                return line;
            }
        }
        return null; // 读到文件末尾
    }
}

