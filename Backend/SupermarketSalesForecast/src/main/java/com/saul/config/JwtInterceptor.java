package com.saul.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saul.common.Result;
import com.saul.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 拦截器：用于拦截未登录请求，并校验 Token 合法性。
 * <p>
 * 约定：
 * - 前端可通过 Authorization: Bearer <token> 或 header token: <token> 传递 token
 * - 校验通过后，会把 claims 放到 request attribute，便于后续业务使用（可选）
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行（CORS 必需）
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = jwtUtils.resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "未登录：缺少 Token");
            return false;
        }

        try {
            Claims claims = jwtUtils.parseClaims(token);
            // 把解析后的 claims 保存起来，后续需要时可直接取
            request.setAttribute("jwtClaims", claims);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            writeUnauthorized(response, "未登录：Token 无效或已过期");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> body = Result.error(401, msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
