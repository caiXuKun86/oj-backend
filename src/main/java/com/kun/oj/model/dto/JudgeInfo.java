package com.kun.oj.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class JudgeInfo {
    private Integer memory;
    private Integer time;
    private String message;
    private List<OutputInfo> outputList;
}
