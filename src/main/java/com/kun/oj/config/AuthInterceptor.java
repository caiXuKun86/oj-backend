package com.kun.oj.config;

import cn.hutool.core.util.StrUtil;
import com.kun.oj.common.ResultCode;
import com.kun.oj.constant.CommonConstants;
import com.kun.oj.constant.RedisConstants;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.utils.JwtUtils;
import com.kun.oj.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 细节一：放行 OPTIONS 预检请求（解决跨域预检失败问题）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 2. 细节二：如果不是映射到 Controller 方法（如静态资源等），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR, "请先登录");
        }
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀

        // 2. 校验 Token
        if (!JwtUtils.validateToken(token)) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR, "登录已过期或凭证无效");
        }
        Long userId = JwtUtils.getUserId(token);
        String redisKey = String.format(RedisConstants.USER_LOGIN_TOKEN, userId);
        String tokenRedis = stringRedisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isBlank(tokenRedis) || !tokenRedis.equals(token)) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR, "登录已过期或凭证无效");
        }
        // 6. 细节三：Token 自动续期（只要用户持续活跃，就自动延长登录有效期）
        stringRedisTemplate.expire(token, CommonConstants.EXPIRE_TIME, TimeUnit.MILLISECONDS);

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserContext.remove();
    }
}
