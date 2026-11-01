package com.kun.oj.model.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecuteCodeRequest {
    private String language;
    private String code;
    private String judgeCaseParentPath;
    private String resultParentPath;
    private Integer count;
    private Integer timeLimit;
    private Integer memoryLimit;
}
