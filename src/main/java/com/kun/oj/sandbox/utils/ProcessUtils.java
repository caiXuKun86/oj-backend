package com.kun.oj.sandbox.utils;

import cn.hutool.core.io.FileUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.kun.oj.model.codesandbox.OutputInfo;
import com.kun.oj.model.enums.JudgeMessageEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 进程工具类
 */
@Slf4j
public class ProcessUtils {

    public static OutputInfo executeCommandInContainer(DockerClient dockerClient, String containerId,
                                                       String inputFilePath, String resultParentPath,
                                                       Long timeLimit, String language, Integer index) {
        OutputInfo outputInfo = new OutputInfo();
        String[] cmd = getCmdByLanguage(language, inputFilePath, index);

        // 必须开启 Attach 流，否则 awaitCompletion 会瞬间返回，导致后台进程被中途强杀！
        //1.创建命令
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
            // 2. 启动执行命令
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            boolean isFinished = execStartCmd.exec(new ExecStartResultCallback()).awaitCompletion(timeLimit, TimeUnit.MILLISECONDS);
            // 3. 处理超时
            if (!isFinished) {
                log.warn("检测到进程超时，准备清理沙箱内残留的僵尸进程...");
                try {
                    // 利用 pkill -9 强杀该容器内残留的程序
                    String processName = language.equals("java") ? "java" : (language.equals("python") ? "python" : "main");
                    String killCmd = "pkill -9 -f " + processName;
                    ExecCreateCmdResponse killExec = dockerClient.execCreateCmd(containerId)
                            .withCmd("sh", "-c", killCmd)
                            .exec();
                    dockerClient.execStartCmd(killExec.getId()).exec(new ExecStartResultCallback()).awaitCompletion();
                } catch (Exception e) {
                    log.error("清理超时残留进程失败", e);
                }
                outputInfo.setMemory(0L);
                outputInfo.setMessage(JudgeMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
                return outputInfo;
            }
            // 4.读取底层资源监控统计
            File statsFile = new File(resultParentPath, "stats_" + index + ".txt");
            if (statsFile.exists()) {
                String statsStr = FileUtil.readUtf8String(statsFile).trim();
                String[] lines = statsStr.split("\n");
                String actualStats = lines[lines.length - 1].trim();
                String[] parts = actualStats.split(",");
                if (parts.length >= 2) {
                    // 第一部分是时间
                    outputInfo.setTime((long) (Double.parseDouble(parts[0].trim()) * 1000));
                    // 第二部分是内存
                    outputInfo.setMemory(Long.parseLong(parts[1].trim()));
                }
            }
            //5. 处理异常状态
            Long exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCodeLong();

            if (exitCode != null && exitCode == 1) {
                outputInfo.setMessage(JudgeMessageEnum.RUNTIME_ERROR.getValue());
            }
            if (exitCode != null && exitCode == 9) {
                outputInfo.setMessage(JudgeMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            }
        } catch (Exception e) {
            log.error("代码执行异常{}", e.getMessage());
            outputInfo.setMessage(JudgeMessageEnum.RUNTIME_ERROR.getValue());
            return outputInfo;
        }
        return outputInfo;
    }

    private static String[] getCmdByLanguage(String language, String inputFilePath, Integer index) {
        String timeFormat = "%e,%M";
        // 约定容器内的输出重定向路径（对应我们挂载的可写目录）
        String out = "/app/result/output_" + index + ".txt";
        String err = "/app/result/error_" + index + ".txt";
        String stats = "/app/result/stats_" + index + ".txt";

        if (language.equals("java")) {
            // -o %s: 将 time 命令的统计结果写到 stats.txt
            // > %s 2> %s: 将程序的正常输出写到 output.txt，报错写到 error.txt
            // 注意：-cp 路径改为了 /app/code
            String cmd = String.format("/usr/bin/time -o %s -f %s java -Dfile.encoding=UTF-8 -cp /app/code Main < %s > %s 2> %s",
                    stats, timeFormat, inputFilePath, out, err);
            return new String[]{"sh", "-c", cmd};

        } else if (language.equals("cpp")) {
            // 注意：C++ 编译后的可执行文件路径改为 /app/code/main
            String cmd = String.format("/usr/bin/time -o %s -f %s /app/code/main < %s > %s 2> %s",
                    stats, timeFormat, inputFilePath, out, err);
            return new String[]{"sh", "-c", cmd};

        } else if (language.equals("python")) {
            // 注意：Python 脚本路径改为 /app/code/main.py
            String cmd = String.format("/usr/bin/time -o %s -f %s python3 /app/code/main.py < %s > %s 2> %s",
                    stats, timeFormat, inputFilePath, out, err);
            return new String[]{"sh", "-c", cmd};
        }

        return new String[]{}; // 兜底处理
    }
}


