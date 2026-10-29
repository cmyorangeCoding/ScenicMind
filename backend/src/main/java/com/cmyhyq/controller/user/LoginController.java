package com.cmyhyq.controller.user;

import com.cmyhyq.common.RedisKeyConstants;
import com.cmyhyq.common.Result;
import com.cmyhyq.pojo.dto.LoginDTO;
import com.cmyhyq.pojo.dto.SmsSendDTO;
import com.cmyhyq.pojo.vo.LoginVO;
import com.cmyhyq.service.AuthService;
import com.cmyhyq.util.SpugSmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final SpugSmsUtil spugSmsUtil;
    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 用户登录
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            return Result.success(authService.login(loginDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "登录失败" : e.getMessage());
        }
    }

    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @PostMapping("/sms/send")
    public Result<String> send(@RequestParam String phone) {


        if (phone == null || phone.isBlank() || !phone.trim().matches("^1\\d{10}$")) {
            return Result.error("手机号格式不正确");
        }


        // 限流：60秒内只能发送一次
        String rateKey = RedisKeyConstants.loginSmsRateKey(phone);
        Boolean allowed = stringRedisTemplate.opsForValue().setIfAbsent(rateKey, "1", 60, TimeUnit.SECONDS);
        if (allowed == null || !allowed) {
            return Result.error("发送太频繁，请60秒后再试");
        }

        String code = String.valueOf((int)((Math.random()*9+1)*100000));

        log.info("开始发送验证码...");
        boolean ok = spugSmsUtil.send(phone, code);


        if (ok) {
            log.info("开始redis.....");
            String redisKey = RedisKeyConstants.loginSmsCodeKey(phone);
            stringRedisTemplate.opsForValue().set(redisKey, code, 300, TimeUnit.SECONDS);
            log.info("验证码已存入Redis: phone={}, key={}", phone, redisKey);
            return Result.success("验证码发送成功");
        } else {
            // 发送失败，删除限流标记，允许立即重试
            stringRedisTemplate.delete(rateKey);
            return Result.error("短信发送失败");
        }
    }

    /**
     * 用户登出
     * @param token 从请求头获取的 Token
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String token) {
        try {
            // 去掉 "Bearer " 前缀（如果有）
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            authService.logout(token);
            return Result.success("登出成功");
        } catch (Exception e) {
            return Result.error(e.getMessage() == null ? "登出失败" : e.getMessage());
        }
    }

}
