package com.kun.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class ScheduledThreadPoolConfig {

    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor(){
        return new ScheduledThreadPoolExecutor(17);
    }
}
