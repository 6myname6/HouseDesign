package com.housedesign.service.ai;

import com.housedesign.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 根据配置选择具体的 AI 图生3D 实现。
 * provider=mock -> 内置降级；其它 -> 外部真实服务（未配置 key 时自动回退到 mock）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageTo3DServiceFactory {

    private final AppProperties appProperties;
    private final MockImageTo3DService mockService;
    private final ExternalImageTo3DService externalService;
    private final ZhipuImageService zhipuService;

    /** 解析当前应使用的实现：mock 直接返回；zhipu 返回智谱；其余走通用外部实现；无 key 回退 mock。 */
    public ImageTo3DService resolve() {
        String provider = appProperties.getAi().getProvider();
        if (provider == null || provider.isBlank() || "mock".equalsIgnoreCase(provider)) {
            return mockService;
        }
        String key = appProperties.getAi().getApiKey();
        if (key == null || key.isBlank()) {
            log.warn("provider={} 但未配置 api-key，自动回退到内置 mock 生成", provider);
            return mockService;
        }
        if ("zhipu".equalsIgnoreCase(provider)) {
            return zhipuService;
        }
        // meshy / tripo / custom 等：走通用外部图生3D 实现
        return externalService;
    }
}
