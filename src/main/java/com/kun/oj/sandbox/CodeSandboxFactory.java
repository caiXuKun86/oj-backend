package com.kun.oj.sandbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CodeSandboxFactory {

    private final Map<String, CodeSandbox> sandboxMap;

    /**
     * 根据语言和模式获取对应的沙箱 Bean
     */
    public CodeSandbox getSandbox(String language) {
        if (language == null) {
            return sandboxMap.get("javaDockerCodeSandbox");
        }
        String lang = language.trim().toLowerCase();
        if ("c++".equals(lang) || "c".equals(lang)) {
            lang = "cpp";
        }
        CodeSandbox sandbox = sandboxMap.get(lang + "DockerCodeSandbox");
        if (sandbox == null) {
            return sandboxMap.get("javaDockerCodeSandbox");
        }
        return sandbox;
    }
}