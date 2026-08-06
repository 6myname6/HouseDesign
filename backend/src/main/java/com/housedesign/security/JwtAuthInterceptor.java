package com.housedesign.security;

import com.housedesign.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器：校验 token 并把 userId 放入请求属性。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "currentUserId";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        String token = header.substring(7);
        try {
            Long userId = jwtUtil.parseUserId(token);
            request.setAttribute(ATTR_USER_ID, userId);
            return true;
        } catch (Exception e) {
            throw new BusinessException(401, "登录凭证无效");
        }
    }
}
