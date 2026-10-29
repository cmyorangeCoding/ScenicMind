package com.cmyhyq.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cmyhyq.common.RedisKeyConstants;
import com.cmyhyq.util.SnowflakeIdGenerator;
import com.cmyhyq.util.TokenUtil;
import com.cmyhyq.pojo.dto.LoginDTO;
import com.cmyhyq.pojo.entity.User;
import com.cmyhyq.pojo.vo.LoginVO;
import com.cmyhyq.service.AuthService;
import com.cmyhyq.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.token-secret:dev-secret-change-me}")
    private String tokenSecret;

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;


    /**
     * 登录
     * @param loginDTO
     * @return
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = loginByPhone(loginDTO);

        //生成令牌
        long expireAt = System.currentTimeMillis() + tokenExpireSeconds * 1000L;
        String token = TokenUtil.generateToken(user.getUserId(), expireAt, tokenSecret);

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getUserId());
        vo.setToken(token);
        vo.setExpireAt(expireAt);
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        return vo;
    }


    /**
     * 通过手机号登录
     * @param dto
     * @return
     */
    private User loginByPhone(LoginDTO dto) {
        String phone = require(dto.getPhone(), "phone 不能为空");
        String smsCode = require(dto.getSmsCode(), "smsCode 不能为空");
        verifySmsCode(phone, smsCode);

        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("phone", phone);
        User user = userService.getOne(qw);
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            userService.updateById(user);
            return user;
        }

        user = new User();
        user.setUserId(snowflakeIdGenerator.nextId());
        user.setPhone(phone);
        user.setNickname("游客" + last4(phone));
        user.setAvatarUrl(defaultAvatar(phone));
        user.setLastLoginTime(LocalDateTime.now());
        userService.save(user);
        return user;
    }

    /**
     * 检验验证码
     * @param phone
     * @param smsCode
     */
    private void verifySmsCode(String phone, String smsCode) {
        String key = RedisKeyConstants.loginSmsCodeKey(phone);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null || cached.isBlank()) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        if (!cached.equals(smsCode)) {
            throw new IllegalArgumentException("验证码错误");
        }
        stringRedisTemplate.delete(key);
    }


    private String require(String value, String msg) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(msg);
        }
        return value.trim();
    }

    /**
     * 手机号后四位
     * @param phone
     * @return
     */
    private String last4(String phone) {
        String p = phone.trim();
        return p.length() >= 4 ? p.substring(p.length() - 4) : p;
    }

    /**
     * 默认头像
     * @param seed
     * @return
     */
    private String defaultAvatar(String seed) {
        String safeSeed = seed == null ? "user" : seed.replaceAll("[^a-zA-Z0-9_-]", "");
        return "https://api.dicebear.com/7.x/identicon/svg?seed=" + safeSeed;
    }


    /**
     * 用户登出
     * @param token
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token 不能为空");
        }

        try {
            // 1. 解析 Token 获取剩余过期时间
            Claims claims = TokenUtil.verifyAndParse(token, tokenSecret);
            if (claims == null) {
                log.warn("登出时 Token 解析失败，可能已过期或非法");
                return;
            }

            // 2. 计算剩余有效时间（秒）
            long expirationTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            long remainingTimeSeconds = (expirationTime - currentTime) / 1000;

            // 3. 如果 Token 已经过期，无需加入黑名单
            if (remainingTimeSeconds <= 0) {
                log.info("Token 已过期，无需加入黑名单");
                return;
            }

            // 4. 将 Token 加入 Redis 黑名单，过期时间与 JWT 剩余有效期一致
            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(token);
            redisTemplate.opsForValue().set(blacklistKey, "blacklisted", remainingTimeSeconds, TimeUnit.SECONDS);

            log.info("用户登出成功，Token 已加入黑名单，剩余有效期: {} 秒", remainingTimeSeconds);

        } catch (Exception e) {
            log.error("登出处理异常", e);
            throw new RuntimeException("登出失败，请稍后重试");
        }
    }




}
