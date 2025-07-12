package org.example.aiedabackend.interceptor;

import org.example.aiedabackend.util.JwtUtil;
import org.example.aiedabackend.vo.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 登录和注册路径不需要验证
        if ("/api/accounts/login".equals(uri) || ("/api/accounts".equals(uri) && "POST".equalsIgnoreCase(method))) {
            return true;
        }

        // 验证Token
        String token = request.getHeader("token");
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            Integer userId = jwtUtil.getUserIdFromToken(token);
            request.setAttribute("username", username);
            request.setAttribute("userId", userId);
            log.info("拦截器设置了用户ID: {} 用户名: {}", userId, username);
            return true;
        }

        // 验证失败，返回401
        handleUnauthorized(response);
        return false;
    }

    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(Response.buildFailure("未授权", "401")));
    }
}