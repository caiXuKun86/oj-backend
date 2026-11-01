package com.kun.oj.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // 默认拦截所有接口
                // 排除不需要登录的公开白名单
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/question/list/page",
                        "/question/get",
                        "/question/search",
                        "/doc.html",            // Knife4j 接口文档
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }
}
