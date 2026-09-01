package com.housedesign.security;

import com.housedesign.common.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 参数解析器：识别带 {@link CurrentUserId} 注解的 Long 参数，
 * 从请求属性中取出 JWT 拦截器写入的 userId 并注入；未登录则抛 401。
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /** 仅支持标注了 @CurrentUserId 且类型为 Long 的参数。 */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    /** 解析参数：从请求属性读取 userId，缺失则视为未登录。 */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object userId = request != null ? request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID) : null;
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
