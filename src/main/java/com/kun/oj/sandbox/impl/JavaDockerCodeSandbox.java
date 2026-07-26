package com.kun.oj.sandbox.impl;


import com.kun.oj.sandbox.AbstractCodeSandboxTemplate;
import org.springframework.stereotype.Component;

@Component("javaDockerCodeSandbox")
public class JavaDockerCodeSandbox extends AbstractCodeSandboxTemplate {

    @Override
    public String getFileName() {
        return "Main.java";
    }


    @Override
    public String getImageName() {
        return "docker.m.daocloud.io/bellsoft/liberica-openjdk-alpine:8";
    }

    @Override
    public String[] getCompileCommand() {

        return new String[]{
                "javac", "-encoding", "utf-8", "Main.java"
        };
    }

}
