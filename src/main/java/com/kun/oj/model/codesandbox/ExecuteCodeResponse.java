package com.kun.oj.model.codesandbox;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeResponse {
    /**
     * 输出集合
     */
    private List<OutputInfo> outputInfoList;

    /**
     * 异常信息
     */
    private String message;
    /**
     * 运行状态
     */
    private Integer status;
    /**
     * 语言
     */
    private String language;


}

