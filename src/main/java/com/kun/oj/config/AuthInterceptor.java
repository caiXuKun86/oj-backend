package com.kun.oj.config;

import com.kun.oj.constant.UserConstant;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 白名单路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/**/v2/api-docs",
            "/**/swagger-resources/**",
            "/**/swagger-ui.html",
            "/**/webjars/**",
            "/**/doc.html",
            "/health"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 由于 context-path 是 /api，我们需要获取 /api 之后的路径进行白名单匹配
        String contextPath = request.getContextPath();
        String matchPath = path;
        if (path.startsWith(contextPath)) {
            matchPath = path.substring(contextPath.length());
        }

        // 白名单放行
        for (String whitePath : WHITE_LIST) {
            if (antPathMatcher.match(whitePath, matchPath)) {
                return true;
            }
        }

        // 判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 40100, \"message\": \"未登录，请先登录\"}");
            return false;
        }

        return true;
    }
}
