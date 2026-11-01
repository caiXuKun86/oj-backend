package com.kun.oj.utils;

public class UserContext {
    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_HOLDER.get();
    }

    // 极其关键：必须有清理方法
    public static void remove() {
        USER_HOLDER.remove();
    }
}