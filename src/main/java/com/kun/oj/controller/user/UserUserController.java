package com.kun.oj.controller.user;

import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.model.dto.request.UserLoginRequest;
import com.kun.oj.model.dto.request.UserRegisterRequest;
import com.kun.oj.model.vo.CurrentUserVO;
import com.kun.oj.model.vo.LoginUserVO;
import com.kun.oj.service.user.UserUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserUserController {

    private final UserUserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public Result<Void> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {

        userService.userRegister(userRegisterRequest);
        return ResultUtils.success();
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/login")
    public Result<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        LoginUserVO loginUserVO = userService.userLogin(userLoginRequest);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 用户注销
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<Boolean> userLogout() {

        boolean result = userService.userLogout();
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @GetMapping("/current")
    public Result<CurrentUserVO> getLoginUser() {
        CurrentUserVO currentUserVO = userService.getLoginUser();
        return ResultUtils.success(currentUserVO);
    }


}
