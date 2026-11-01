package com.kun.oj.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.model.dto.request.UserLoginRequest;
import com.kun.oj.model.dto.request.UserRegisterRequest;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.vo.CurrentUserVO;
import com.kun.oj.model.vo.LoginUserVO;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserUserService extends IService<User> {


    void userRegister(UserRegisterRequest userRegisterRequest);

    LoginUserVO userLogin(UserLoginRequest userLoginRequest);

    /**
     * 获取当前登录用户
     *
     * @return
     */
    CurrentUserVO getLoginUser();

    /**
     * 用户注销
     *
     * @return
     */
    boolean userLogout();


}
