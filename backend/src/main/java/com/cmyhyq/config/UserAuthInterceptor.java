package com.cmyhyq.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cmyhyq.util.TokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "AUTH_USER_ID";

    private final String secret;

    public UserAuthInterceptor(@Value("${app.auth.token-secret:dev-secret-change-me}") String secret) {
        this.secret = secret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractToken(request);
        Claims claims = TokenUtil.verifyAndParse(token, secret);
        if (claims == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        Long userId = Long.parseLong(subject);
        String paramUserId = request.getParameter("userId");
        if (paramUserId != null && !paramUserId.isBlank() && !String.valueOf(userId).equals(paramUserId)) {
            writeUnauthorized(response, "用户身份校验失败");
            return false;
        }

        request.setAttribute(ATTR_USER_ID, String.valueOf(userId));
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring("Bearer ".length()).trim();
        }
        String token = request.getHeader("token");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        token = request.getParameter("token");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        JSONObject res = new JSONObject();
        res.put("code", 401);
        res.put("msg", msg);
        res.put("data", null);
        response.getWriter().write(JSON.toJSONString(res));
    }
}

