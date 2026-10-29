package com.cmyhyq.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cmyhyq.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class WeatherServiceImpl implements WeatherService {

    // 为了支持gzip，我们创建一个带解压配置的RestTemplate
    private static final RestTemplate restTemplate;

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        restTemplate = new RestTemplate(factory);
        // 设置字符编码
        restTemplate.getMessageConverters().stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        // 不处理错误状态码
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

            protected boolean hasError(HttpStatus statusCode) {
                return false;
            }
        });
    }

    @Value("${app.weather.api-key:}")
    private String apiKey;

    // 灵山胜境经纬度
    private static final String LOCATION = "120.09,31.43";
    // 你的专属API地址
    private static final String API_URL = "https://np5khwpb8r.re.qweatherapi.com/v7/weather/now?location=%s&key=%s";

    @Override
    public String getRealtimeWeather() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("天气API Key未配置");
            return "暂时无法查询实时天气";
        }

        try {
            String url = String.format(API_URL, LOCATION, apiKey);
            log.info("请求和风天气API：{}", url);

            // 添加Accept-Encoding: gzip头，让服务端知道我们支持解压
            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept-Encoding", "gzip");

            ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(url, byte[].class);
            byte[] responseBody = responseEntity.getBody();

            if (responseBody == null || responseBody.length == 0) {
                log.warn("和风天气API响应为空");
                return "暂时无法查询实时天气";
            }

            // 判断是否是gzip压缩（前两个字节为0x1f8b）
            String rawJson;
            if (responseBody.length >= 2 && (responseBody[0] & 0xFF) == 0x1f && (responseBody[1] & 0xFF) == 0x8b) {
                // 解压gzip数据
                try (GZIPInputStream gzipInputStream = new GZIPInputStream(new java.io.ByteArrayInputStream(responseBody))) {
                    rawJson = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                // 非压缩数据，直接转字符串
                rawJson = new String(responseBody, StandardCharsets.UTF_8);
            }

            log.info("和风天气API解压后响应：{}", rawJson);

            JSONObject response = JSON.parseObject(rawJson);
            String code = response.getString("code");

            if ("200".equals(code)) {
                JSONObject now = response.getJSONObject("now");
                String text = now.getString("text");
                String temp = now.getString("temp");
                String windDir = now.getString("windDir");
                String humidity = now.getString("humidity");

                return String.format("当前天气：%s，温度：%s℃，风向：%s，湿度：%s%%。", text, temp, windDir, humidity);
            } else {
                log.error("和风天气API返回错误，code:{}", code);
                return "天气服务暂时不可用（错误码：" + code + "）";
            }

        } catch (Exception e) {
            log.error("天气API调用失败", e);
            return "暂时无法查询实时天气";
        }
    }
}