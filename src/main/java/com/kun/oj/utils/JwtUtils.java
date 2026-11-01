package com.kun.oj.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.kun.oj.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JwtUtils {

    // 签名密钥（建议放在 application.yaml 中配置，生产环境一定要足够长、足够复杂）
    private static final byte[] SECRET_KEY = "qwertyuiop123".getBytes();


    /**
     * 1. 生成 Token（将 userId, role 放入载荷）
     */
    public static String createToken(Long userId, String userRole) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("userRole", userRole);
        // 设置过期时间
        long expireAt = System.currentTimeMillis() + CommonConstants.EXPIRE_TIME;
        payload.put(JWTPayload.EXPIRES_AT, expireAt / 1000); // 秒级时间戳

        return JWTUtil.createToken(payload, SECRET_KEY);
    }

    /**
     * 2. 校验 Token 是否合法且未过期
     */
    public static boolean validateToken(String token) {
        try {
            // 校验密钥签名
            if (!JWTUtil.verify(token, SECRET_KEY)) {
                return false;
            }
            // 校验是否过期
            JWT jwt = JWTUtil.parseToken(token);
            long expireTime = Convert.toLong(jwt.getPayload(JWTPayload.EXPIRES_AT), 0L) * 1000;
            return System.currentTimeMillis() <= expireTime;
        } catch (Exception e) {
            log.error("Token 校验异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 3. 解析 Token 获取用户 ID
     */
    public static Long getUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return Convert.toLong(jwt.getPayload("userId"));
    }

    /**
     * 4. 解析 Token 获取用户角色
     */
    public static String getUserRole(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return Convert.toStr(jwt.getPayload("userRole"));
    }
}