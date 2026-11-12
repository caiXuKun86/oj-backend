package com.kun.oj.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.ResultCode;
import com.kun.oj.constant.CommonConstants;
import com.kun.oj.constant.RedisConstants;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.mapper.UserMapper;
import com.kun.oj.model.dto.request.UserLoginRequest;
import com.kun.oj.model.dto.request.UserRegisterRequest;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.vo.CurrentUserVO;
import com.kun.oj.model.vo.LoginUserVO;
import com.kun.oj.service.user.UserUserService;
import com.kun.oj.utils.JwtUtils;
import com.kun.oj.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;


/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserUserServiceImpl extends ServiceImpl<UserMapper, User> implements UserUserService {


    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "IKUNjige666";


    @Override
    public void userRegister(UserRegisterRequest userRegisterRequest) {
        String username = userRegisterRequest.getUsername();
        String password = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String nickname = userRegisterRequest.getNickname();
        if (StrUtil.hasBlank(username, password, checkPassword, nickname)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "参数为空");
        }
        if (username.length() < 4) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "用户账号过短");
        }
        if (password.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        boolean exists = this.lambdaQuery()
                .eq(User::getUsername, username)
                .exists();

        if (exists) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "账号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword);
        user.setNickname(nickname);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "注册失败");
        }


    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest) {
        String username = userLoginRequest.getUsername();
        String password = userLoginRequest.getPassword();
        if (StrUtil.hasBlank(username, password)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        // 查询用户是否存在
        User user = this.lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getPassword, encryptPassword)
                .one();
        if (user == null) {
            throw new BusinessException(ResultCode.OPERATION_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        String token = JwtUtils.createToken(user.getId(), user.getRole());
        String redisKey = String.format(RedisConstants.USER_LOGIN_TOKEN, user.getId());
        stringRedisTemplate.opsForValue().set(redisKey, token, CommonConstants.EXPIRE_TIME, TimeUnit.MILLISECONDS);
        LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);
        loginUserVO.setToken(token);
        return loginUserVO;
    }


    /**
     * 获取当前登录用户
     *
     * @return
     */
    @Override
    public CurrentUserVO getLoginUser() {

        long userId = UserContext.getUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return BeanUtil.copyProperties(user, CurrentUserVO.class);
    }

    /**
     * 用户注销
     *
     */
    @Override
    public boolean userLogout() {
        Long userId = UserContext.getUserId();
        String redisKey = String.format(RedisConstants.USER_LOGIN_TOKEN, userId);
        stringRedisTemplate.delete(redisKey);
        // 移除登录态
        return true;
    }


}
