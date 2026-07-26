package com.kun.oj.sandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.kun.oj.model.codesandbox.ExecuteCodeRequest;
import com.kun.oj.model.codesandbox.ExecuteCodeResponse;
import com.kun.oj.model.codesandbox.OutputInfo;
import com.kun.oj.model.enums.JudgeMessageEnum;
import com.kun.oj.model.enums.JudgeStatusEnum;
import com.kun.oj.sandbox.utils.ProcessUtils;
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


    public String compileCode(String codeParentPath)  {
        String containerId = null;

        try {
            //1.创建容器
            HostConfig hostConfig = new HostConfig()
                    .withMemory(256 * 1024 * 1024L) //最大内存
                    .withCpuCount(1L) //CPU核心数
                    .withMemorySwap(256 * 1024 * 1024L)//禁止内存交换
                    .withTmpFs(Collections.singletonMap("/tmp", "rw,noexec,nosuid,size=65536k"))
                    .withPidsLimit(50L)//最多50个线程
                    .withSecurityOpts(Arrays.asList("no-new-privileges"));
            hostConfig.setBinds(
                    //挂载用户code
                    new Bind(codeParentPath, new Volume("/app/code"), AccessMode.rw)
            );
            CreateContainerResponse response = dockerClient.createContainerCmd(getImageName())
                    .withNetworkDisabled(true)
                    .withHostConfig(hostConfig)
                    .withReadonlyRootfs(true)
                    .withAttachStdout(true)         // 允许获取标准输出
                    .withAttachStderr(true)         // 允许获取标准错误输出
                    .withCmd("/bin/sh", "-c", "tail -f /dev/null")
                    .exec();
            //2.启动容器
            containerId = response.getId();
            dockerClient.startContainerCmd(containerId).exec();
            //3.执行命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withWorkingDir("/app/code")
                    .withCmd(getCompileCommand())
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            boolean completed = dockerClient.execStartCmd(execId)
                    .exec(new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            // 每当容器打印一行日志，就会触发一次 onNext
                            StreamType streamType = frame.getStreamType();
                            byte[] payload = frame.getPayload();
                            if (StreamType.STDERR.equals(streamType)) {
                                stderrStream.write(payload, 0, payload.length);
                            }
                        }
                    })
                    .awaitCompletion(5, TimeUnit.SECONDS); // ⏱️ 超时控制（防死循环）
            if (!completed) {
                return "编译超时";
            }
            return stderrStream.toString("UTF-8");
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    public ExecuteCodeResponse runCode(ExecuteCodeRequest executeCodeRequest, ExecuteCodeResponse response, String codeParentPath) {
        String language = executeCodeRequest.getLanguage();
        Long timeLimit = executeCodeRequest.getTimeLimit();
        Long memoryLimit = executeCodeRequest.getMemoryLimit();
        Integer count = executeCodeRequest.getCount();
        String testcaseParentPath = executeCodeRequest.getTestcaseParentPath();
        String resultParentPath = executeCodeRequest.getResultParentPath();

        String containerId = null;
        try {
            //1.创建容器
            HostConfig hostConfig = new HostConfig()
                    .withMemory(memoryLimit) //最大内存
                    .withCpuCount(1L) //CPU核心数
                    .withMemorySwap(memoryLimit)//禁止内存交换
                    .withTmpFs(Collections.singletonMap("/tmp", "rw,noexec,nosuid,size=65536k"))
                    .withPidsLimit(50L)//最多50个线程
                    .withSecurityOpts(Arrays.asList("no-new-privileges"));
            hostConfig.setBinds(
                    //挂载用户code
                    new Bind(codeParentPath, new Volume("/app/code"), AccessMode.ro),
                    // 挂载输入用例
                    new Bind(testcaseParentPath, new Volume("/app/testcase"), AccessMode.ro),
                    // 挂载用户代码输出
                    new Bind(resultParentPath, new Volume("/app/result"), AccessMode.rw)
            );
            CreateContainerResponse container = dockerClient.createContainerCmd(getImageName())
                    .withNetworkDisabled(true)
                    .withHostConfig(hostConfig)
                    .withReadonlyRootfs(true)
                    .withCmd("/bin/sh", "-c", "tail -f /dev/null")
                    .exec();
            //2.启动容器
            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            //3.执行代码
            List<OutputInfo> outputInfoList = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                // 构造当前测试点的输入文件对象
                String containerInputPath = "/app/testcase/" + i + ".in";
                OutputInfo outputInfo = null;
                try {
                    outputInfo = ProcessUtils.executeCommandInContainer(dockerClient, containerId, containerInputPath, resultParentPath, timeLimit, language, i);
                } catch (Exception e) {
                    log.error("代码执行发生非预期崩溃：" + e.getMessage());
                    response.setMessage(JudgeMessageEnum.RUNTIME_ERROR.getValue());
                    response.setStatus(JudgeStatusEnum.FAILED.getValue());
                    return response;
                }
                outputInfoList.add(outputInfo);
            }
            response.setStatus(JudgeStatusEnum.SUCCEED.getValue());
            response.setOutputInfoList(outputInfoList);
            return response;
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }

    }

    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        String language = executeCodeRequest.getLanguage();
        if (!language.equals("cpp")) {
            executeCodeRequest.setTimeLimit(executeCodeRequest.getTimeLimit() << 1);
            executeCodeRequest.setMemoryLimit(executeCodeRequest.getMemoryLimit() << 1);

        }
        response.setLanguage(language);
        String codeParentPath = null;
        try {
            //1.保存代码文件
            if (!FileUtil.exist(globalCodePath)) {
                FileUtil.mkdir(globalCodePath);
            }
            codeParentPath = globalCodePath + File.separator + UUID.randomUUID();
            String codePath = codeParentPath + File.separator + getFileName();
            FileUtil.writeString(executeCodeRequest.getCode(), codePath, StandardCharsets.UTF_8);

            // 2.编译代码
            if(!language.equals("python")){
                String compileErr = compileCode(codeParentPath);
                if (StrUtil.isNotBlank(compileErr)) {
                    response.setStatus(JudgeStatusEnum.FAILED.getValue());
                    response.setMessage(JudgeMessageEnum.COMPILE_ERROR.getValue());
                    return response;
                }
            }
            // 3.执行代码
            return runCode(executeCodeRequest, response, codeParentPath);

        } catch (Exception e) {
            log.error("代码运行发生其他异常" + e.getMessage());
            response.setStatus(JudgeStatusEnum.FAILED.getValue());
            response.setMessage(JudgeMessageEnum.SYSTEM_ERROR.getValue());
            return response;
        } finally {
            //删除code文件
            if (codeParentPath != null) {
                boolean del = FileUtil.del(codeParentPath);
                if (!del) {
                    log.error("清理本地用户代码目录失败，可能存在权限问题或残留占用，路径: {}", codeParentPath);
                } else {
                    log.info("成功清理本地用户代码目录: {}", codeParentPath);
                }
            }
        }
    }

}
