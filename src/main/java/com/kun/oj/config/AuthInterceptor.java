package com.kun.oj.config;

import com.kun.oj.common.ResultCode;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.utils.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 强制登录鉴权拦截器：
 * 针对非白名单路由，要求必须已在 TokenInterceptor 中成功解析用户登录态。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 2. 如果不是映射到 Controller 方法（如静态资源等），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 3. 校验登录态
        if (UserContext.getUserId() == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return true;
    }
}
