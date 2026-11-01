package com.kun.oj.sandbox;


import com.kun.oj.model.dto.request.ExecuteCodeRequest;
import com.kun.oj.model.dto.response.ExecuteCodeResponse;

public interface CodeSandbox {

    ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest);


}
