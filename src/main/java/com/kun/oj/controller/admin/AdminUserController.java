package com.kun.oj.controller.admin;

import com.kun.oj.annotation.AuthCheck;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.Result;
import com.kun.oj.common.ResultUtils;
import com.kun.oj.constant.UserConstant;
import com.kun.oj.model.dto.request.UserPageRequest;
import com.kun.oj.model.dto.request.UserUpdateRequest;
import com.kun.oj.model.vo.UserPageVO;
import com.kun.oj.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


/**
 * 用户接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/user/admin")
public class AdminUserController {

    private final AdminUserService userService;


    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<PageResult<UserPageVO>> queryUserPage(@RequestBody UserPageRequest userQueryRequest) {
        PageResult<UserPageVO> result = userService.queryUserPage(userQueryRequest);
        return ResultUtils.success(result);
    }

    @PutMapping("/{userId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, @PathVariable Long userId) {
        Boolean result = userService.updateUser(userUpdateRequest, userId);
        return ResultUtils.success(result);
    }
//    @PutMapping("/{userId}/reset")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public Result<Boolean> resetPassword(@RequestBody UserUpdateRequest userUpdateRequest, @PathVariable Long userId) {
//        Boolean result = userService.updateUser(userUpdateRequest, userId);
//        return ResultUtils.success(result);
//    }


}
