package com.cmyhyq.common;

public class RedisKeyConstants {

    public static final String LOGIN_SMS_CODE_PREFIX = "login:sms:";
    public static final String LOGIN_SMS_RATE_PREFIX = "login:sms:rate:";
    public static final String AI_MEMORY_PREFIX = "ai:memory:";
    public static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";


    //手机号+验证码
    public static String loginSmsCodeKey(String phone) {
        return LOGIN_SMS_CODE_PREFIX + phone;
    }

    //手机号+频繁操作时间（60s）
    public static String loginSmsRateKey(String phone) {
        return LOGIN_SMS_RATE_PREFIX + phone;
    }

    //会话记忆(userId+sessionId)
    public static String aiMemoryKey(Long userId, String sessionId) {
        return AI_MEMORY_PREFIX + userId + ":" + sessionId;
    }

    // Token 黑名单（token 本身作为唯一标识）
    public static String tokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }
}
