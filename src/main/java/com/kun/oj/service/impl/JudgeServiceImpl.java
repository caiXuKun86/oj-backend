package com.kun.oj.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.kun.oj.common.ResultCode;
import com.kun.oj.enums.CodeLanguageEnum;
import com.kun.oj.enums.JudgeMessageEnum;
import com.kun.oj.enums.JudgeStatusEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.manage.oss.OssTemplate;
import com.kun.oj.model.dto.JudgeInfo;
import com.kun.oj.model.dto.OutputInfo;
import com.kun.oj.model.dto.request.ExecuteCodeRequest;
import com.kun.oj.model.dto.response.ExecuteCodeResponse;
import com.kun.oj.model.entity.Question;
import com.kun.oj.model.entity.QuestionSubmit;
import com.kun.oj.sandbox.CodeSandbox;
import com.kun.oj.sandbox.CodeSandboxFactory;
import com.kun.oj.service.JudgeService;
import com.kun.oj.service.QuestionSubmitService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kun.oj.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service; // 添加此引入
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
@Service // 确保被 Spring 扫描到
public class JudgeServiceImpl implements JudgeService {


    private final QuestionMapper questionMapper;

    private final QuestionSubmitService questionSubmitService;
    private final OssTemplate ossTemplate;

    private final CodeSandboxFactory codeSandboxFactory;
    private final RedissonClient redissonClient;

    @Value("${oj.path.judge-case}")
    private String baseJudgePath;
    @Value("${oj.path.result}")
    private String baseResultPath;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doJudge(long questionSubmitId) {
        // 1. 获取提交与题目信息
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "提交信息不存在");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "题目信息不存在");
        }
        // 2. 上传测试用例
        // 2.1 先进行一次锁外的轻量级检查
        String judgeCaseVersion = question.getJudgeCaseVersion();
        String judgeCasePath = baseJudgePath + File.separator + questionId;
        File judgeCaseDir = FileUtil.mkdir(judgeCasePath);
        File versionFile = new File(judgeCaseDir, ".version");

        boolean needDownload = true;
        if (versionFile.exists()) {
            String localVersion = FileUtil.readUtf8String(versionFile);
            if (judgeCaseVersion.equals(localVersion.trim())) {
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
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "等待测试用例同步超时");
                }

                // 关键改动 2：二次检查（Double-Check）
                if (versionFile.exists() && judgeCaseVersion.equals(FileUtil.readUtf8String(versionFile).trim())) {
                    needDownload = false;
                }

                // 3. 执行真正的更新逻辑
                if (needDownload) {
                    log.info("开始同步题目 {} 的测试用例，目标版本: {}", questionId, judgeCaseVersion);

                    // 关键改动 3：下载到临时目录，避免直接删除旧文件破坏正在运行的判题任务
                    File tempDir = FileUtil.mkdir(baseJudgePath + File.separator + questionId + "_temp_" + System.currentTimeMillis());
                    File zipFile = new File(tempDir, "testcase.zip");

                    try {
                        String ossKey = question.getJudgeCaseKey();
                        ossTemplate.downloadFile(ossKey, zipFile);

                        // 解压到临时目录
                        ZipUtil.unzip(zipFile, tempDir, StandardCharsets.UTF_8);
                        FileUtil.del(zipFile); // 解压完立刻删 zip

                        // 写入版本号
                        File tempVersionFile = new File(tempDir, ".version");
                        FileUtil.writeUtf8String(judgeCaseVersion, tempVersionFile);

                        // 将临时目录中的文件覆盖覆盖到正式目录
                        FileUtil.copyContent(tempDir, judgeCaseDir, true);

                    } catch (Exception e) {
                        log.error("测试用例同步失败, questionId: {}", questionId, e);
                        throw new BusinessException(ResultCode.OPERATION_ERROR, "测试用例同步失败");
                    } finally {
                        // 清理临时目录
                        FileUtil.del(tempDir);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取锁被中断");
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }


        // 3. 更新提交状态为“判题中”,并更新题目提交数量
        if (!questionSubmit.getStatus().equals(JudgeStatusEnum.WAITING.getCode())) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "题目正在判题中，请勿重复提交");
        }
        boolean update = questionSubmitService.lambdaUpdate()
                .set(questionSubmit.getJudgeInfo() != null, QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(questionSubmit.getJudgeInfo()))
                .set(QuestionSubmit::getStatus, JudgeStatusEnum.JUDGING.getCode())
                .eq(QuestionSubmit::getId, questionSubmitId)
                .eq(QuestionSubmit::getStatus, JudgeStatusEnum.WAITING.getCode())
                .update();
        if (!update) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "判题状态更新失败");
        }
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .setSql("submit_num = submit_num  + 1")
                .eq(Question::getId, questionId));


        String resultParentPath = baseResultPath + File.separator + UUID.randomUUID();
        try {
            // 4. 构造请求调用代码沙箱
            Integer count = question.getJudgeCaseCount();
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(questionSubmit.getCode())
                    .language(questionSubmit.getLanguage())
                    .judgeCaseParentPath(judgeCasePath)
                    .resultParentPath(resultParentPath)
                    .count(count)
                    .timeLimit(question.getTimeLimit())
                    .memoryLimit(question.getMemoryLimit() * 1024 * 1024)
                    .build();
            try {
                checkBeforeExec(executeCodeRequest);
            } catch (Exception e) {
                log.error("参数异常{}", e.getMessage());
                saveJudgeResult(questionSubmitId, null, JudgeStatusEnum.FAILED.getCode());
                return;
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
            judgeInfo.setMemory(0);
            judgeInfo.setTime(0);
            // 如果沙箱本身执行失败（例如调用超时、系统错误）
            if (executeCodeResponse == null) {
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getCode());
                saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getCode());
                return;
            }

            // 如果是编译错误，直接记录并返回
            if (JudgeMessageEnum.COMPILE_ERROR.getCode().equals(executeCodeResponse.getJudgeMessage())) {
                judgeInfo.setMessage(JudgeMessageEnum.COMPILE_ERROR.getCode());
                saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.SUCCESS.getCode());
                return;
            }

            // 如果沙箱执行失败或者没有得到输出结果
            List<OutputInfo> outputInfoList = executeCodeResponse.getOutputInfoList();
            if (!executeCodeResponse.getJudgeStatus().equals(JudgeStatusEnum.SUCCESS.getCode()) || outputInfoList == null || outputInfoList.isEmpty()) {
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getCode());
                saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getCode());
                return;
            }
            // 5. 遍历验证结果
            int maxTime = 0;
            int maxMemory = 0;
            String finalMessage = JudgeMessageEnum.ACCEPTED.getCode(); // 默认全对

            List<OutputInfo> outputList = new ArrayList<>(count + 1);
            try {
                for (int i = 1; i <= count; i++) {
                    //期望输出用例路径和用户输出用例路径
                    String expectedOutputPath = judgeCasePath + File.separator + i + ".out";
                    String resultPath = resultParentPath + File.separator + "output_" + i + ".txt";
                    OutputInfo outputInfo = outputInfoList.get(i - 1);
                    outputList.add(outputInfo);

                    // 记录最大时间和内存 (替换为取 Max)
                    maxTime = Math.max(maxTime, outputInfo.getTime() == null ? 0 : outputInfo.getTime());
                    maxMemory = Math.max(maxMemory, outputInfo.getMemory() == null ? 0 : outputInfo.getMemory());


                    // 如果某个用例自身运行报错 (如超出内存、运行时异常)
                    if (StrUtil.isNotBlank(outputInfo.getMessage())) {
                        if (finalMessage.equals(JudgeMessageEnum.ACCEPTED.getCode())) {
                            finalMessage = outputInfo.getMessage();
                        }
                        continue;
                    }

                    // 对比输出 (加入 trim 防止空格回车误判)
                    if (!isFileContentEqualLenient(expectedOutputPath, resultPath)) {
                        finalMessage = JudgeMessageEnum.WRONG_ANSWER.getCode();
                        outputInfo.setMessage(JudgeMessageEnum.WRONG_ANSWER.getCode());
                    }

                }
                judgeInfo.setOutputList(outputList);
                judgeInfo.setMessage(finalMessage);
                judgeInfo.setTime(maxTime);
                judgeInfo.setMemory(maxMemory);
                // 6. 更新数据库最终结果
                if (finalMessage.equals(JudgeMessageEnum.ACCEPTED.getCode())) {
                    //更新通过数
                    questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                            .setSql("accepted_num = accepted_num + 1")
                            .eq(Question::getId, questionId));
                }

                saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.SUCCESS.getCode());
            } catch (Exception e) {
                log.error("判题异常" + e.getMessage());
                judgeInfo.setTime(0);
                judgeInfo.setMemory(0);
                judgeInfo.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getCode());
                judgeInfo.setOutputList(null);
                saveJudgeResult(questionSubmitId, judgeInfo, JudgeStatusEnum.FAILED.getCode());
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
        Integer timeLimit = executeCodeRequest.getTimeLimit();
        Integer memoryLimit = executeCodeRequest.getMemoryLimit();
        String judgeCaseParentPath = executeCodeRequest.getJudgeCaseParentPath();
        String resultParentPath = executeCodeRequest.getResultParentPath();
        Integer count = executeCodeRequest.getCount();
        ThrowUtils.throwIf(StrUtil.hasBlank(code, language, judgeCaseParentPath, resultParentPath), ResultCode.PARAMS_ERROR);
        if (CodeLanguageEnum.getByCode(language) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "暂时不支持该语言");
        }
        if (count == null || count < 1) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "测试用例错误");
        }

        if (timeLimit == null) {
            executeCodeRequest.setTimeLimit(1000);
        }
        if (memoryLimit == null) {
            executeCodeRequest.setMemoryLimit(64 * 1024 * 1024);
        }
    }

    /**
     * 抽取公共方法：保存判题最终结果
     */
    private void saveJudgeResult(long questionSubmitId, JudgeInfo judgeInfo, int status) {
        boolean update = questionSubmitService.lambdaUpdate()
                .set(judgeInfo != null, QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(judgeInfo))
                .set(QuestionSubmit::getStatus, status)
                .set(judgeInfo != null,QuestionSubmit::getMessage, judgeInfo.getMessage())
                .eq(QuestionSubmit::getId, questionSubmitId)
                .eq(QuestionSubmit::getStatus, JudgeStatusEnum.JUDGING.getCode())
                .update();
        if (!update) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "更新判题状态失败");
        }
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

