package com.housedesign.config;

import com.housedesign.security.CurrentUserIdArgumentResolver;
import com.housedesign.security.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

/**
 * Web 层配置：集中配置跨域(CORS)、静态资源映射、JWT 鉴权拦截器与 @CurrentUserId 参数解析器。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    /** 配置跨域：允许所有来源与常见方法，并允许携带凭据（注意：生产环境应收紧来源）。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 将 /files/** 映射到本地存储目录，使上传图片可通过 URL 访问。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(appProperties.getStorage().getLocation())
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }

    /** 注册 JWT 拦截器：拦截 /api/**，放行登录与注册接口。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register"
                );
    }

    /** 注册 @CurrentUserId 参数解析器，使控制器可直接注入当前登录用户 ID。 */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
