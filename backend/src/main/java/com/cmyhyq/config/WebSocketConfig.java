package com.cmyhyq.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketConfig {

    //启动开关
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }


    /**
     * 配置 WebSocket 容器参数（解决断开问题）
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置文本消息缓冲区大小（AI 回答可能很长）
        container.setMaxTextMessageBufferSize(8192 * 100); // 800KB
        // 设置二进制消息缓冲区大小
        container.setMaxBinaryMessageBufferSize(8192 * 100);
        // 设置异步发送超时时间（防止阻塞）
        container.setAsyncSendTimeout(10000L);
        // 设置空闲超时时间（10 分钟，防止移动端断连）
        container.setMaxSessionIdleTimeout(600000L);
        return container;
    }
}
