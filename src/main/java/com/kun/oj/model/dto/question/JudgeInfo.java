package com.kun.oj.model.dto.question;

import com.kun.oj.model.codesandbox.OutputInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class JudgeInfo {
    /**
     * 输出集合
     */
    private List<OutputInfo> outputList;
    /**
     * 报错信息
     */
    private String message;
    /**
     * 运行最长时皮
     */
    private Long time;
    /**
     * 运行最大内存
     */
    private Long memory;
}

