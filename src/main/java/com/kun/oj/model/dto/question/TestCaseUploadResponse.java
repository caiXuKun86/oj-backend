package com.kun.oj.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseUploadResponse {
    private String key;
    private Integer count;
    private String testcaseVersion;
}

