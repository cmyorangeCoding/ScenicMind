package com.cmyhyq.service;

/**
 * 天气服务接口
 */
public interface WeatherService {

    /**
     * 获取无锡实时天气数据
     * @return 格式化后的天气字符串
     */
    String getRealtimeWeather();
}
