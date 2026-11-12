package org.king.smartquiz.common.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类：封装常用操作。
 * 依赖 framework 模块中配置的 RedisTemplate（String key + JSON value 序列化）。
 */
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setCacheObject(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setCacheObject(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /** SETNX：仅当 key 不存在时设置，原子操作，用于防重复提交 / 限流 */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(ok);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /** 获取 key 剩余过期时间（秒）；-2 表示 key 不存在，-1 表示未设置过期 */
    public long getExpire(String key) {
        Long t = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return t == null ? -2 : t;
    }

    public boolean deleteObject(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long increment(String key, long delta) {
        Long v = redisTemplate.opsForValue().increment(key, delta);
        return v == null ? 0 : v;
    }
}
