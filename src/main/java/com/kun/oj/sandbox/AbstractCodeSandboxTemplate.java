package com.kun.oj.sandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.kun.oj.enums.JudgeMessageEnum;
import com.kun.oj.enums.JudgeStatusEnum;
import com.kun.oj.model.dto.OutputInfo;
import com.kun.oj.model.dto.request.ExecuteCodeRequest;
import com.kun.oj.model.dto.response.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractCodeSandboxTemplate implements CodeSandbox {

    @Resource
    private DockerClient dockerClient;

    @Value("${oj.path.code}")
    private String globalCodePath;

    public abstract String getFileName();

    public abstract String getImageName();

    public abstract String[] getCompileCommand();

    public abstract String getRunCommand();

    /**
     * 编译代码
     *
     * @param codeParentPath 代码所在目录
     * @return 编译错误信息（返回 null 或空字符串表示编译成功）
     */
    public String compileCode(String codeParentPath) {
        String containerId = null;

        try {
            // 1. 创建编译容器
            HostConfig hostConfig = new HostConfig()
                    .withMemory(256 * 1024 * 1024L) // 编译期分配 256MB
                    .withCpuCount(1L)
                    .withMemorySwap(256 * 1024 * 1024L) // 禁止内存交换
                    .withTmpFs(Collections.singletonMap("/tmp", "rw,noexec,nosuid,size=65536k"))
                    .withPidsLimit(50L)
                    .withSecurityOpts(Collections.singletonList("no-new-privileges"));
            hostConfig.setBinds(
                    new Bind(codeParentPath, new Volume("/app/code"), AccessMode.rw)
            );

            CreateContainerResponse response = dockerClient.createContainerCmd(getImageName())
                    .withNetworkDisabled(true)
                    .withHostConfig(hostConfig)
                    .withReadonlyRootfs(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("/bin/sh", "-c", "tail -f /dev/null")
                    .exec();

            // 2. 启动容器
            containerId = response.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // 3. 执行编译命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withWorkingDir("/app/code")
                    .withCmd(getCompileCommand())
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();

            String execId = execCreateCmdResponse.getId();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            boolean completed = dockerClient.execStartCmd(execId)
                    .exec(new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            byte[] payload = frame.getPayload();
                            if (payload != null) {
                                outputStream.write(payload, 0, payload.length);
                            }
                        }
                    })
                    .awaitCompletion(10, TimeUnit.SECONDS); // 编译超时控制

            if (!completed) {
                return "编译超时";
            }

            // 检查编译进程退出码
            Long exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCodeLong();
            if (exitCode != null && exitCode != 0) {
                // 退出码非 0 说明编译失败，返回错误输出信息
                String errMsg = outputStream.toString("UTF-8").trim();
                return StrUtil.isNotBlank(errMsg) ? errMsg : "编译失败，错误退出码: " + exitCode;
            }

            // 编译成功
            return null;
        } catch (Exception e) {
            log.error("编译代码发生异常", e);
            throw new RuntimeException("编译代码异常: " + e.getMessage(), e);
        } finally {
            if (StrUtil.isNotBlank(containerId)) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.error("清理编译容器失败, containerId: {}", containerId, e);
                }
            }
        }
    }

    /**
     * 运行代码
     */
    public ExecuteCodeResponse runCode(ExecuteCodeRequest executeCodeRequest, ExecuteCodeResponse response, String codeParentPath) {
        String language = executeCodeRequest.getLanguage();
        Integer timeLimit = executeCodeRequest.getTimeLimit();
        Integer memoryLimit = executeCodeRequest.getMemoryLimit();
        Integer count = executeCodeRequest.getCount();
        String judgeCaseParentPath = executeCodeRequest.getJudgeCaseParentPath();
        String resultParentPath = executeCodeRequest.getResultParentPath();

        // 规范化内存限制（防止传入过小或单位错误导致 Docker 拒绝创建，Docker 最小需 6MB）
        long containerMemory = (memoryLimit == null || memoryLimit <= 0) ? 64 * 1024 * 1024L : memoryLimit.longValue();
        if (containerMemory < 6 * 1024 * 1024L) {
            // 如果传入的是 MB 或 KB，自动转换为字节
            if (containerMemory <= 1024) {
                containerMemory = containerMemory * 1024 * 1024L; // MB -> Byte
            } else {
                containerMemory = containerMemory * 1024L; // KB -> Byte
            }
        }
        // 对于 Java，由于 JVM 自身元空间和运行时开销约 30-50MB，为容器内存留出兜底缓冲
        if ("java".equalsIgnoreCase(language)) {
            containerMemory = Math.max(containerMemory, 128 * 1024 * 1024L);
        }

        String containerId = null;
        try {
            // 1. 创建运行容器
            HostConfig hostConfig = new HostConfig()
                    .withMemory(containerMemory)
                    .withCpuCount(1L)
                    .withMemorySwap(containerMemory) // 禁止内存交换
                    .withTmpFs(Collections.singletonMap("/tmp", "rw,noexec,nosuid,size=65536k"))
                    .withPidsLimit(50L) // 限制并发线程数，防止 fork 炸弹
                    .withSecurityOpts(Collections.singletonList("no-new-privileges"));
            hostConfig.setBinds(
                    new Bind(codeParentPath, new Volume("/app/code"), AccessMode.ro), // 只读挂载代码
                    new Bind(judgeCaseParentPath, new Volume("/app/testcase"), AccessMode.ro), // 只读挂载测试用例
                    new Bind(resultParentPath, new Volume("/app/result"), AccessMode.rw) // 读写挂载输出结果
            );

            CreateContainerResponse container = dockerClient.createContainerCmd(getImageName())
                    .withNetworkDisabled(true) // 禁用网络访问
                    .withHostConfig(hostConfig)
                    .withReadonlyRootfs(true) // 只读根文件系统
                    .withCmd("/bin/sh", "-c", "tail -f /dev/null")
                    .exec();

            // 2. 启动容器
            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // 3. 循环运行各测试用例
            List<OutputInfo> outputInfoList = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                String containerInputPath = "/app/testcase/" + i + ".in";
                OutputInfo outputInfo;
                try {
                    outputInfo = executeCommandInContainer(containerId, containerInputPath, resultParentPath, timeLimit, language, i);
                } catch (Exception e) {
                    log.error("测试点 {} 代码执行发生非预期崩溃: {}", i, e.getMessage());
                    response.setJudgeMessage(JudgeMessageEnum.RUNTIME_ERROR.getCode());
                    response.setJudgeStatus(JudgeStatusEnum.FAILED.getCode());
                    return response;
                }
                outputInfoList.add(outputInfo);
            }

            response.setJudgeStatus(JudgeStatusEnum.SUCCESS.getCode());
            response.setOutputInfoList(outputInfoList);
            return response;
        } catch (Exception e) {
            log.error("运行代码容器生命周期异常", e);
            throw new RuntimeException("运行代码异常: " + e.getMessage(), e);
        } finally {
            if (StrUtil.isNotBlank(containerId)) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.error("清理运行容器失败, containerId: {}", containerId, e);
                }
            }
        }
    }

    /**
     * 沙箱模板执行主入口
     */
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        String language = executeCodeRequest.getLanguage();
        response.setLanguage(language);
        String codeParentPath = null;

        try {
            // 1. 保存用户代码到临时目录
            if (!FileUtil.exist(globalCodePath)) {
                FileUtil.mkdir(globalCodePath);
            }
            codeParentPath = globalCodePath + File.separator + UUID.randomUUID();
            String codePath = codeParentPath + File.separator + getFileName();
            FileUtil.writeString(executeCodeRequest.getCode(), codePath, StandardCharsets.UTF_8);

            // 2. 编译代码（如果语言有编译命令则执行编译）
            String[] compileCommand = getCompileCommand();
            if (compileCommand != null && compileCommand.length > 0) {
                String compileErr = compileCode(codeParentPath);
                if (StrUtil.isNotBlank(compileErr)) {
                    response.setJudgeStatus(JudgeStatusEnum.SUCCESS.getCode());
                    response.setJudgeMessage(JudgeMessageEnum.COMPILE_ERROR.getCode());
                    return response;
                }
            }

            // 3. 执行代码
            return runCode(executeCodeRequest, response, codeParentPath);

        } catch (Exception e) {
            log.error("代码沙箱运行异常", e);
            response.setJudgeStatus(JudgeStatusEnum.FAILED.getCode());
            response.setJudgeMessage(JudgeMessageEnum.SYSTEM_ERROR.getCode());
            return response;
        } finally {
            // 4. 清理本地用户代码文件目录
            if (codeParentPath != null) {
                try {
                    FileUtil.del(codeParentPath);
                    log.info("成功清理本地临时代码目录: {}", codeParentPath);
                } catch (Exception e) {
                    log.error("清理本地代码目录失败: {}", codeParentPath, e);
                }
            }
        }
    }

    /**
     * 在容器内执行单条命令并采集资源监控与输出
     */
    private OutputInfo executeCommandInContainer(String containerId,
                                                 String inputFilePath, String resultParentPath,
                                                 Integer timeLimit, String language, Integer index) {
        OutputInfo outputInfo = new OutputInfo();
        String[] cmd = getCmdByLanguage(inputFilePath, index);

        // 1. 创建执行命令
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withCmd(cmd)
                .exec();

        String execId = execCreateCmdResponse.getId();
        if (execId == null) {
            throw new RuntimeException("创建 Docker 执行命令失败");
        }

        try {
            // 2. 启动执行命令（给 Docker 守护进程和进程启停留出安全缓冲时间，防止因通信延迟被误杀）
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            long waitTimeout = Math.max(timeLimit * 2L, timeLimit + 2000L);
            boolean isFinished = execStartCmd.exec(new ExecStartResultCallback())
                    .awaitCompletion(waitTimeout, TimeUnit.MILLISECONDS);

            // 3. 超时强杀残留进程
            if (!isFinished) {
                log.warn("测试点 {} 执行超过硬超时限度 ({}ms)，强杀沙箱内进程...", index, waitTimeout);
                try {
                    // 进程名称匹配强杀
                    String killCmd = "pkill -9 -i -f main || pkill -9 -i -f java || pkill -9 -i -f python";
                    ExecCreateCmdResponse killExec = dockerClient.execCreateCmd(containerId)
                            .withCmd("sh", "-c", killCmd)
                            .exec();
                    dockerClient.execStartCmd(killExec.getId()).exec(new ExecStartResultCallback()).awaitCompletion(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("清理超时残留进程失败", e);
                }
                outputInfo.setTime(timeLimit);
                outputInfo.setMemory(0);
                outputInfo.setMessage(JudgeMessageEnum.TIME_LIMIT_EXCEEDED.getCode());
                return outputInfo;
            }

            // 4. 读取 /usr/bin/time 统计的资源数据
            File statsFile = new File(resultParentPath, "stats_" + index + ".txt");
            if (statsFile.exists()) {
                try {
                    String statsStr = FileUtil.readUtf8String(statsFile).trim();
                    if (StrUtil.isNotBlank(statsStr)) {
                        String[] lines = statsStr.split("\n");
                        String actualStats = lines[lines.length - 1].trim();
                        String[] parts = actualStats.split(",");
                        if (parts.length >= 2) {
                            int actualTime = (int) (Double.parseDouble(parts[0].trim()) * 1000);
                            int actualMemory = Integer.parseInt(parts[1].trim());
                            outputInfo.setTime(actualTime);
                            outputInfo.setMemory(actualMemory);

                            // 如果采集到的实际耗时超过题目的 timeLimit，标记超时
                            if (actualTime > timeLimit) {
                                outputInfo.setMessage(JudgeMessageEnum.TIME_LIMIT_EXCEEDED.getCode());
                                return outputInfo;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析测试点 {} 的 stats 资源统计失败: {}", index, e.getMessage());
                }
            }

            // 5. 检查进程退出码状态
            Long exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCodeLong();
            if (exitCode != null && exitCode != 0) {
                // 137 (SIGKILL) 常见于内存超限被 OOM-Killer 杀死
                if (exitCode == 137 || exitCode == 9) {
                    outputInfo.setMessage(JudgeMessageEnum.MEMORY_LIMIT_EXCEEDED.getCode());
                } else {
                    // 其他非 0 退出码均为运行异常（除零、段错误、未捕获异常、语法错误等）
                    outputInfo.setMessage(JudgeMessageEnum.RUNTIME_ERROR.getCode());
                }
            }
        } catch (Exception e) {
            log.error("代码执行异常", e);
            outputInfo.setMessage(JudgeMessageEnum.RUNTIME_ERROR.getCode());
            return outputInfo;
        }

        return outputInfo;
    }

    /**
     * 组装运行和度量命令
     */
    private String[] getCmdByLanguage(String inputFilePath, Integer index) {
        String timeFormat = "%e,%M";
        String out = "/app/result/output_" + index + ".txt";
        String err = "/app/result/error_" + index + ".txt";
        String stats = "/app/result/stats_" + index + ".txt";
        String runCommand = getRunCommand();

        // 统一使用 /usr/bin/time 包装命令：重定向输入、标准输出、错误输出以及资源监控
        String cmd = String.format("/usr/bin/time -o %s -f %s " + runCommand + " < %s > %s 2> %s",
                stats, timeFormat, inputFilePath, out, err);
        return new String[]{"sh", "-c", cmd};
    }
}
