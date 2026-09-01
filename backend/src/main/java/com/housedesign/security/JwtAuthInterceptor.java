package com.housedesign.security;

import com.housedesign.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器：在请求进入控制器前校验 Authorization 头中的 Bearer Token，
 * 解析出 userId 并放入请求属性，供 {@link CurrentUserIdArgumentResolver} 使用。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 存放 userId 的请求属性名。 */
    public static final String ATTR_USER_ID = "currentUserId";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 非控制器方法（如静态资源）直接放行
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
