package com.kun.oj.constant;

public interface RedisConstants {

    /**
     * 用户登录token String user:login:token:{userId}
     */
    String USER_LOGIN_TOKEN = "user:login:token:%s";

    /**
     * 题目信息缓存 String cache:question:info:{questionId}
     */
    String CACHE_QUESTION_INFO = "cache:question:info:%s";
    /**
     * 题目信息缓存缓存击穿锁 String cache:lock:question:info:{questionId}
     */
    String CACHE_LOCK_QUESTION_INFO = "cache:lock:question:info:%s";

    /**
     * 提交答案锁 String lock:question:submit:{questionId}:{userId}
     */
    String LOCK_QUESTION_SUBMIT = "lock:question:submit:%s:%s";

}
