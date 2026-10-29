package com.cmyhyq.config;

import com.cmyhyq.util.TokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketAuthConfigurator extends ServerEndpointConfig.Configurator {
    
    private static String tokenSecret;
    
    @Value("${app.auth.token-secret}")
    public void setTokenSecret(String secret) {
        WebSocketAuthConfigurator.tokenSecret = secret;
    }
    
    public WebSocketAuthConfigurator() {
        super();
    }

    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        try {
            String token = extractToken(request);
            if (token == null || token.isBlank()) {
                log.warn("WebSocket 握手失败：缺少 Token");
                throw new RuntimeException("缺少 Token");
            }
            
            Claims claims = TokenUtil.verifyAndParse(token, tokenSecret);
            if (claims == null) {
                log.warn("WebSocket 握手失败：Token 无效或已过期");
                throw new RuntimeException("Token 无效");
            }
            
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new RuntimeException("Token 主体为空");
            }
            
            Long userId = Long.parseLong(subject);
            sec.getUserProperties().put("AUTH_USER_ID", userId);
            
            log.info("WebSocket 握手成功：userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket 握手异常", e);
            throw new RuntimeException("身份验证失败: " + e.getMessage());
        }
    }
    
    private String extractToken(HandshakeRequest request) {
        var authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String auth = authHeaders.get(0);
            if (auth != null && auth.startsWith("Bearer ")) {
                return auth.substring("Bearer ".length()).trim();
            }
        }
        
        var tokenHeaders = request.getHeaders().get("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0).trim();
        }
        
        var params = request.getParameterMap();
        var tokenParams = params.get("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            return tokenParams.get(0).trim();
        }
        
        return null;
    }
}
