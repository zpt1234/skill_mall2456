package com.seckill.interceptor;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.seckill.util.JwtUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    //白名单
    private static final String[] WHITE_LIST = {
            "/api/user/login",
            "/api/user/register",
            "/error",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/admin/login",
            "/api/admin/register"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. 放行跨域预检请求 OPTIONS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setHeader("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "token,Authorization,Content-Type");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 2. 白名单放行
        for (String url : WHITE_LIST) {
            if (path.contains(url)) {
                return true;
            }
        }

        // 3. AI接口特殊处理：尝试解析token，但不强制要求
        if (path.contains("/api/ai")) {
            String token = request.getHeader("token");
            if (token == null) {
                token = request.getHeader("Authorization");
            }
            if (token != null && token.startsWith("Bearer ")) {
                token = token.replace("Bearer ", "").trim();
            }
            
            if (token != null && !token.isEmpty()) {
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    boolean isExpired = jwtUtil.isExpired(token);
                    
                    if (claims != null && !isExpired) {
                        Long userId = jwtUtil.getUserId(token);
                        request.setAttribute("userId", userId);
                    }
                } catch (Exception e) {
                    // Token 无效，作为未登录用户处理
                }
            }
            return true;
        }

        // 4. 其他接口：获取Token
        String token = request.getHeader("token");
        if (token == null) {
            token = request.getHeader("Authorization");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.replace("Bearer ", "").trim();
        }

        // 5. 无Token → 401
        if (token == null || token.isEmpty()) {
            log.warn("请求被拦截: 未提供 Token, {} {}", method, path);
            response.setStatus(401);
            return false;
        }

        // 6. Token校验
        if (jwtUtil.parseToken(token) == null || jwtUtil.isExpired(token)) {
            log.warn("请求被拦截: Token 无效或已过期, {} {}", method, path);
            response.setStatus(401);
            return false;
        }

        // 7. 存入用户ID
        Long userId = jwtUtil.getUserId(token);
        request.setAttribute("userId", userId);

        return true;
    }
}