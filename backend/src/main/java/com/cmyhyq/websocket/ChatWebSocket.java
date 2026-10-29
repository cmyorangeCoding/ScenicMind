package com.cmyhyq.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cmyhyq.ai.MasterAgent;
import com.cmyhyq.service.ChatHistoryService;
import com.cmyhyq.util.OssService;
import com.cmyhyq.util.SpringContextUtil;
import com.cmyhyq.util.TokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@ServerEndpoint(value = "/ws/chat", configurator = com.cmyhyq.config.WebSocketAuthConfigurator.class)
@Slf4j
public class ChatWebSocket {

//    长连接：一次握手，全程通信，省资源。
//    异步流式：AI 生成和前端渲染并行，体验像真人打字。
//    ID 绑定：用 msgId 解决多线程下的消息归属问题，防止串台。
//    容错机制：心跳保活 + 断线重连，适应移动端不稳定网络。
//    先让 AI 开始流式回答，等回答结束后再异步上传图片到 OSS

    private MasterAgent aiManager;
    private OssService ossService;
    private ChatHistoryService chatHistoryService;


    public ChatWebSocket() {
        this.aiManager = SpringContextUtil.getBean(MasterAgent.class);
        this.ossService = SpringContextUtil.getBean(OssService.class);
        this.chatHistoryService = SpringContextUtil.getBean(ChatHistoryService.class);
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        Long userId = (Long) config.getUserProperties().get("AUTH_USER_ID");
        if (userId == null) {
            log.warn("WebSocket 连接拒绝：Token 验证失败");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "身份验证失败"));
            } catch (IOException e) {
                log.error("关闭非法连接失败", e);
            }
            return;
        }

        session.getUserProperties().put("userId", userId);
        log.info("WebSocket 连接建立: sessionId={}, userId={}", session.getId(), userId);
    }



    @OnMessage
    public void onMessage(String message, Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) {
            log.warn("收到未认证消息，关闭连接: {}", session.getId());
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭连接失败", e);
            }
            return;
        }

        try {
            // 1. 处理心跳包
            if (message.contains("\"type\":\"ping\"")) {
                if (session.isOpen()) session.getAsyncRemote().sendText("{\"type\":\"pong\"}");
                return;
            }

            // 2. 解析 JSON 参数
            JSONObject json = JSON.parseObject(message);
            String msgId = json.getString("msgId");
            String sessionId = json.getString("sessionId");
            String question = json.getString("message");
            String imageBase64 = json.getString("imageBase64");
            String currentAttraction = json.getString("currentAttraction");

            log.info("收到消息 msgId={}, 内容={}, 有图={}", msgId, question, imageBase64 != null && imageBase64.length() > 50);

            // 3. 提取有效图片
            String finalImageBase64 = (imageBase64 != null && imageBase64.length() > 50) ? imageBase64 : null;

            // 定义 final 变量供 Lambda 表达式使用
            final String finalMsgId = msgId;
            final Long finalUserId = userId;
            final String finalSessionId = sessionId;
            final String finalQuestion = question;

            Flux<String> flux = aiManager.handleRequest(userId, sessionId, question, currentAttraction, finalImageBase64, null);

            // 4. 订阅流式响应
            flux.subscribe(
                    chunk -> {
                        if (session.isOpen()) {
                            try {
                                session.getBasicRemote().sendText("msgId=" + finalMsgId + "|" + chunk);
                            } catch (Exception e) {
                                log.error("发送消息失败", e);
                            }
                        }
                    },
                    error -> {
                        // 阿里云 SDK 的 response_error 是“假异常”，忽略它并触发上传
                        if (error.getMessage() != null && error.getMessage().contains("response_error")) {
                            log.warn("捕获到阿里云流式结束假异常（已忽略）");
                            uploadImageIfPresent(finalImageBase64, finalMsgId, finalUserId, finalSessionId, finalQuestion);
                        } else {
                            log.error("AI 处理异常 msgId={}", finalMsgId, error);
                        }
                    },
                    () -> {
                        // 正常结束流程，触发上传
                        try {
                            session.getBasicRemote().sendText("[DONE]");
                            log.info("消息处理完成 msgId={}", finalMsgId);
                            uploadImageIfPresent(finalImageBase64, finalMsgId, finalUserId, finalSessionId, finalQuestion);
                        } catch (IOException e) {
                            log.error("发送 [DONE] 失败", e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("WebSocket 处理失败", e);
        }
    }


    private void uploadImageIfPresent(String imageBase64, String msgId, Long userId, String sessionId, String question) {
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                // 1. 处理 Base64 数据
                String base64Data = imageBase64.contains(",") ? imageBase64.split(",")[1] : imageBase64;
                byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);

                // 2. 同步上传到 OSS
                String imageUrl = ossService.uploadFile(inputStream, msgId + ".jpg");
                log.info("图片上传 OSS 成功: {}", imageUrl);

                // 3. 同步更新数据库（此时 MasterAgent 的 INSERT 已提交，必能查到）
                chatHistoryService.updateImageUrlBySession(userId, sessionId, question, imageUrl);
                log.info("图片 URL 已存入数据库, userId={}, sessionId={}", userId, sessionId);

            } catch (Exception e) {
                log.error("图片上传或入库失败", e);
            }
        }
    }


    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("连接断开: {}, 原因: {}", session.getId(), closeReason.getReasonPhrase());
    }


}




