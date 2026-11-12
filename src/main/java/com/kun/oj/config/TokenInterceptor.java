package com.kun.oj.config;

import cn.hutool.core.util.StrUtil;
import com.kun.oj.constant.CommonConstants;
import com.kun.oj.constant.RedisConstants;
import com.kun.oj.utils.JwtUtils;
import com.kun.oj.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 全局用户 Token 解析拦截器（弱鉴权）：
 * 尝试从请求头中解析 Authorization Bearer Token 并注入 UserContext；
 * 若无 Token 或 Token 无效则直接放行（由后续强鉴权拦截器决定是否必须登录）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (JwtUtils.validateToken(token)) {
                try {
                    Long userId = JwtUtils.getUserId(token);
                    String redisKey = String.format(RedisConstants.USER_LOGIN_TOKEN, userId);
                    String tokenRedis = stringRedisTemplate.opsForValue().get(redisKey);
                    if (StrUtil.isNotBlank(tokenRedis) && tokenRedis.equals(token)) {
                        UserContext.setUserId(userId);
                        // 活跃续期
                        stringRedisTemplate.expire(redisKey, CommonConstants.EXPIRE_TIME, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    log.error("Token 解析失败: {}", e.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }
}
