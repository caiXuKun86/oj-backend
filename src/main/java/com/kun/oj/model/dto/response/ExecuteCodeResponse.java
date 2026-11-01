package com.kun.oj.model.dto.response;


import com.kun.oj.model.dto.OutputInfo;
import lombok.Data;

import java.util.List;

@Data
public class ExecuteCodeResponse {
    private String language;
    private Integer judgeStatus;
    private String judgeMessage;
    private List<OutputInfo> outputInfoList;
}
