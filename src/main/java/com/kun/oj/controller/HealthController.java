package com.kun.oj.controller;

import com.kun.oj.common.BaseResponse;
import com.kun.oj.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("health");
    }
}
