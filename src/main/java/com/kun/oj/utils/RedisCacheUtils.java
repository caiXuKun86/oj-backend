package com.kun.oj.utils;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
public class RedisCacheUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据范式 pattern 批量删除 key（使用 SCAN 游标，安全不阻塞）
     *
     * @param pattern 匹配范式，例如 "oj:question:list:*"
     */
    public void deleteByPattern(String pattern) {
        // 1. 使用 execute 执行低级 API 进行 SCAN 扫描
        Set<String> keysToDelete = stringRedisTemplate.execute(connection -> {
            Set<String> binaryKeys = new HashSet<>();
            // 每次扫描 1000 条
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    binaryKeys.add(new String(cursor.next()));
                }
            }
            return binaryKeys;
        }, true);

        // 2. 批量删除匹配到的 keys
        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            stringRedisTemplate.delete(keysToDelete);
        }
    }
}