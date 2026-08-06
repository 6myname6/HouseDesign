package com.housedesign.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.housedesign.config.AppProperties;
import com.housedesign.entity.DesignStyle;
import com.housedesign.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 真实外部 AI 图生3D 服务（以 Meshy image-to-3d 接口契约为默认实现，可通过配置切换 base-url）。
 *
 * 流程：读取设计图 -> 转 base64 data URI -> 创建任务 -> 轮询 -> 下载 glb 落地 -> 返回可访问 URL。
 *
 * 说明：该实现需要在 application.yml 中配置 app.ai.provider=meshy 及有效的 api-key。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalImageTo3DService implements ImageTo3DService {

    private final AppProperties appProperties;
    private final FileStorageService fileStorageService;

    @Override
    public String provider() {
        // 该实现服务于所有非 mock 的 provider
        return "external";
    }

    @Override
    public GenerationOutput generate(GenerationContext context) throws Exception {
        AppProperties.Ai ai = appProperties.getAi();
        if (ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 app.ai.api-key，无法调用外部 AI 服务");
        }

        WebClient client = WebClient.builder()
                .baseUrl(ai.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ai.getApiKey())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024 * 1024))
                .build();

        // 1) 图片转 data URI
        byte[] imageBytes = Files.readAllBytes(context.getImageAbsolutePath());
        String mime = Files.probeContentType(context.getImageAbsolutePath());
        if (mime == null) {
            mime = "image/png";
        }
        String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        // 2) 创建任务（携带风格贴图提示，引导生成对应装修风格的材质与配色）
        DesignStyle style = DesignStyle.fromCode(context.getStyle());
        log.info("外部 AI 生成, 风格={}, texturePrompt={}", style.getDisplayName(), style.getTexturePrompt());
        JsonNode createResp = client.post()
                .uri("/openapi/v1/image-to-3d")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "image_url", dataUri,
                        "enable_pbr", true,
                        "texture_prompt", style.getTexturePrompt()
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(60));

        if (createResp == null || !createResp.has("result")) {
            throw new IllegalStateException("创建 AI 生成任务失败: " + createResp);
        }
        String taskId = createResp.get("result").asText();
        log.info("外部 AI 任务已创建: {}", taskId);

        // 3) 轮询任务状态
        long deadline = System.currentTimeMillis() + ai.getTimeoutSeconds() * 1000L;
        String glbUrl = null;
        String thumbnailUrl = null;
        while (System.currentTimeMillis() < deadline) {
            JsonNode status = client.get()
                    .uri("/openapi/v1/image-to-3d/{id}", taskId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));
            if (status == null) {
                Thread.sleep(3000);
                continue;
            }
            String state = status.path("status").asText("");
            if ("SUCCEEDED".equalsIgnoreCase(state)) {
                glbUrl = status.path("model_urls").path("glb").asText(null);
                thumbnailUrl = status.path("thumbnail_url").asText(null);
                break;
            } else if ("FAILED".equalsIgnoreCase(state) || "EXPIRED".equalsIgnoreCase(state)) {
                throw new IllegalStateException("AI 生成任务失败, 状态=" + state);
            }
            Thread.sleep(4000);
        }

        if (glbUrl == null) {
            throw new IllegalStateException("AI 生成超时或未返回模型地址");
        }

        // 4) 下载 glb 落地到本地存储，避免外链失效
        byte[] glbBytes = WebClient.create().get()
                .uri(glbUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(120));
        FileStorageService.StoredFile stored = fileStorageService.storeBytes(glbBytes, "models", ".glb");

        return GenerationOutput.builder()
                .provider(appProperties.getAi().getProvider())
                .modelUrl(stored.url())
                .previewImageUrl(thumbnailUrl != null ? thumbnailUrl : context.getImageUrl())
                .sceneConfig(null)
                .build();
    }
}
