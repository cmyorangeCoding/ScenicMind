package com.cmyhyq.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SpugSmsUtil {

    // 你自己复制的 URL
    private static final String SPUG_URL = "https://push.spug.cc/send/bQ2Aomq9KemRK3da";

    private final RestTemplate restTemplate;

    public SpugSmsUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean send(String phone, String code) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "测试");
            params.put("code", code);
            params.put("number", "5");
            params.put("targets", phone);

            String res = restTemplate.postForObject(SPUG_URL, params, String.class);
            log.info("Spug 返回：{}", res);
            return res != null && (res.contains("\"code\": 200") || res.contains("\"code\":200"));
        } catch (Exception e) {
            log.error("发送短信失败，手机号: {}", phone, e);
            return false;
        }
    }
}