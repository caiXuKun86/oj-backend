package com.kun.oj.sandbox.impl;

import com.kun.oj.sandbox.AbstractCodeSandboxTemplate;
import org.springframework.stereotype.Component;

@Component("pythonDockerCodeSandbox")
public class PythonDockerCodeSandbox extends AbstractCodeSandboxTemplate {


    @Override
    public String getFileName() {
        // Python 脚本的标准后缀
        return "main.py";
    }


    @Override
    public String getImageName() {
        // 🌟 使用 Python 的 Alpine 极简镜像，体积小（约 50MB），安全且启动极快
        return "python:3.9-alpine";
    }

    @Override
    public String[] getCompileCommand() {
        return new String[0];
    }

    @Override
    public String getRunCommand() {
        return "python3 /app/code/main.py";
    }
}