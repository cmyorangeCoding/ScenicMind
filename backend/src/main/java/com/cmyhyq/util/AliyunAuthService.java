package com.cmyhyq.util;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class AliyunAuthService {

    @Value("${app.aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${app.aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${guide.aliyun.app-key}")
    @Getter
    private String nlsAppKey;

    private IAcsClient client;

    private volatile String cachedToken;
    @Getter
    private volatile long tokenExpireTime;

    //启动时自动执行
    @PostConstruct
    public void init() {
        // 1. 创建阿里云配置对象
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", accessKeyId, accessKeySecret);
        // 2. 【关键】手动注册 NLS 服务的域名映射
        DefaultProfile.addEndpoint("cn-shanghai", "nls-meta", "nls-meta.cn-shanghai.aliyuncs.com");
        // 3. 初始化阿里云 HTTP 客户端
        this.client = new DefaultAcsClient(profile);
    }

    public String getAccessToken() {
        long currentTime = System.currentTimeMillis();
        // 如果 Token 存在，且距离过期还有超过 1 小时，直接返回缓存
        if (cachedToken != null && currentTime < tokenExpireTime - 3600000) {
            return cachedToken;
        }
        synchronized (this) {
            // 进入锁后再次检查，防止多个线程同时进来重复请求
            if (cachedToken != null && currentTime < tokenExpireTime - 3600000) {
                return cachedToken;
            }
            // 只有第一个进来的线程才会执行下面的请求逻辑
            try {
                CommonRequest request = new CommonRequest();
                // 【官方正确方法，无任何不存在/废弃报错】
                request.setSysDomain("nls-meta.cn-shanghai.aliyuncs.com");
                request.setSysVersion("2019-02-28");
                request.setSysAction("CreateToken");
                request.setSysMethod(MethodType.POST);
                request.setSysProtocol(com.aliyuncs.http.ProtocolType.HTTPS);

                CommonResponse response = client.getCommonResponse(request);
                if (response.getHttpStatus() == 200) {
                    String body = response.getData();
                    com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(body);
                    this.cachedToken = json.getJSONObject("Token").getString("Id");
                    this.tokenExpireTime = currentTime + (24 * 60 * 60 * 1000);
                    log.info("阿里云语音Token获取成功");
                    return this.cachedToken;
                } else {
                    throw new RuntimeException("获取Token失败：" + response.getData());
                }
            } catch (ClientException e) {
                log.error("获取阿里云语音Token失败", e);
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}