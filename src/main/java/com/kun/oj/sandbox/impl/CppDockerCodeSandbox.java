package com.kun.oj.sandbox.impl;

import com.kun.oj.sandbox.AbstractCodeSandboxTemplate;
import org.springframework.stereotype.Component;

@Component("cppDockerCodeSandbox")
public class CppDockerCodeSandbox extends AbstractCodeSandboxTemplate {

    @Override
    public String getFileName() {
        return "main.cpp";
    }



    @Override
    public String getImageName() {
        return "n0madic/alpine-gcc:9.2.0";
    }

    @Override
    public String[] getCompileCommand() {
        return new String[]{
                "g++", "-fmax-errors=3", "-O2", "-std=c++14", "main.cpp", "-o", "Main"
        };

    }

}
