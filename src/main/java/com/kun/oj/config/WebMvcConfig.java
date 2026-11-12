package com.kun.oj.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;
    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 全局 Token 解析拦截器（弱鉴权）：优先执行，解析登录态存入 UserContext
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**"
                )
                .order(1);

        // 2. 强鉴权拦截器：拦截需要登录的接口，未登录则抛出异常
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 排除不需要强制登录的公开白名单
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/question/list",
                        "/question/{questionId}",
                        "/question/search",
                        "/question-submit/{submitId}",
                        "/question-submit/list",
                        "/question-answer/list",
                        "/question-answer/{answerId}",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**"
                )
                .order(2);
    }
}
