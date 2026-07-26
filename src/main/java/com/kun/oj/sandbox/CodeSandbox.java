package com.kun.oj.sandbox;


import com.kun.oj.model.codesandbox.ExecuteCodeRequest;
import com.kun.oj.model.codesandbox.ExecuteCodeResponse;

public interface CodeSandbox {

    ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest);


}
