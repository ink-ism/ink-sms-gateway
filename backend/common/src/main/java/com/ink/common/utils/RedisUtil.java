package com.ink.common.utils;

import com.ink.common.constant.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ========== 通用操作 ==========

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    // ========== 用户 Token ==========

    public void setUserToken(String userId, String token) {
        String key = RedisConstants.USER_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public Object getUserToken(String userId) {
        return redisTemplate.opsForValue().get(RedisConstants.USER_TOKEN_PREFIX + userId);
    }

    public void removeUserToken(String userId) {
        redisTemplate.delete(RedisConstants.USER_TOKEN_PREFIX + userId);
    }

    // ========== 用户信息 ==========

    public void setUserInfo(String userId, Object userInfo) {
        String key = RedisConstants.USER_INFO_PREFIX + userId;
        redisTemplate.opsForValue().set(key, userInfo, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public Object getUserInfo(String userId) {
        return redisTemplate.opsForValue().get(RedisConstants.USER_INFO_PREFIX + userId);
    }

    public void removeUserInfo(String userId) {
        redisTemplate.delete(RedisConstants.USER_INFO_PREFIX + userId);
    }

    // ========== 短信验证码 ==========

    public void setSmsCode(String phone, String code) {
        String key = RedisConstants.SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, RedisConstants.SMS_CODE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public Object getSmsCode(String phone) {
        return redisTemplate.opsForValue().get(RedisConstants.SMS_CODE_PREFIX + phone);
    }

    public void removeSmsCode(String phone) {
        redisTemplate.delete(RedisConstants.SMS_CODE_PREFIX + phone);
    }
}
