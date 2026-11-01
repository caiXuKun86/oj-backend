package com.kun.oj.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JudgeCaseUploadResponse {
    private String key;
    private Integer count;
    private String version;
}
