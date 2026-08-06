package com.housedesign.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.housedesign.config.AppProperties;
import com.housedesign.entity.DesignStyle;
import com.housedesign.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱（BigModel / open.bigmodel.cn）文生写实装修效果图服务。
 *
 * 对每个房间分别调用 cogview 文生图，得到一张符合正常显示比例的写实室内照片（1344x768，约 16:9），
 * 下载落盘后写入 GeneratedModel.panoramaUrl，并把「房间 -> 效果图」的映射
 * 写入 sceneConfig（type=photo-tour），前端按真实比例以平面照片展示并支持多房间切换。
 *
 * 需在 application.yml 配置：
 *   app.ai.provider=zhipu
 *   app.ai.api-key=<智谱 Key>
 *   app.ai.base-url=https://open.bigmodel.cn/api/paas/v4
 *   app.ai.image-model=cogview-4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuImageService implements ImageTo3DService {

    private final AppProperties appProperties;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    /** 默认房间布局（与 MockImageTo3DService 一致），用于生成多房间漫游图。 */
    private static final List<RoomSpec> ROOMS = List.of(
            new RoomSpec(0, "客厅", List.of(1, 2, 3)),
            new RoomSpec(1, "主卧", List.of(0)),
            new RoomSpec(2, "厨房", List.of(0, 3)),
            new RoomSpec(3, "卫生间", List.of(0, 2))
    );

    @Override
    public String provider() {
        return "zhipu";
    }

    @Override
    public GenerationOutput generate(GenerationContext ctx) throws Exception {
        AppProperties.Ai ai = appProperties.getAi();
        if (ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 app.ai.api-key，无法调用智谱 AI 服务");
        }
        String baseUrl = (ai.getBaseUrl() == null || ai.getBaseUrl().isBlank())
                ? DEFAULT_BASE_URL : ai.getBaseUrl();
        String model = (ai.getImageModel() == null || ai.getImageModel().isBlank())
                ? "cogview-4" : ai.getImageModel();

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ai.getApiKey())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024 * 1024))
                .build();

        DesignStyle style = DesignStyle.fromCode(ctx.getStyle());

        List<Map<String, Object>> roomOut = new ArrayList<>();
        String firstPanorama = null;

        for (RoomSpec rs : ROOMS) {
            String prompt = buildPrompt(style, rs.name);
            log.info("智谱文生图, 房间={}, 风格={}", rs.name, style.getDisplayName());

            JsonNode resp = callImagesApi(client, model, prompt);

            if (resp == null || !resp.has("data") || resp.get("data").isEmpty()) {
                throw new IllegalStateException("智谱文生图返回异常: " + resp);
            }
            JsonNode item = resp.get("data").get(0);
            String imgUrl = item.path("url").asText(null);
            byte[] imgBytes;
            if (imgUrl != null && !imgUrl.isBlank()) {
                imgBytes = downloadImage(imgUrl);
            } else {
                String b64 = item.path("b64_json").asText(null);
                if (b64 == null || b64.isBlank()) {
                    throw new IllegalStateException("智谱返回的图片既无 url 也无 b64_json");
                }
                imgBytes = Base64.getDecoder().decode(b64);
            }

            FileStorageService.StoredFile stored =
                    fileStorageService.storeBytes(imgBytes, "panoramas", ".jpg");
            if (firstPanorama == null) {
                firstPanorama = stored.url();
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.id);
            m.put("name", rs.name);
            m.put("connections", rs.connections);
            m.put("panoramaUrl", stored.url());
            roomOut.add(m);
        }

        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("type", "photo-tour");
        scene.put("styleLabel", style.getDisplayName());
        scene.put("rooms", roomOut);

        return GenerationOutput.builder()
                .provider("zhipu")
                .panoramaUrl(firstPanorama)
                .previewImageUrl(firstPanorama)
                .sceneConfig(objectMapper.writeValueAsString(scene))
                .build();
    }

    /** 调用智谱文生图接口，遇到 429/5xx 时指数退避重试。 */
    private JsonNode callImagesApi(WebClient client, String model, String prompt) {
        int maxAttempts = 5;
        long backoffBase = 4000L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.post()
                        .uri("/images/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "model", model,
                                "prompt", prompt,
                                "size", "1344x768"
                        ))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(150));
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                String body = safeBody(e);
                if (status == 429 || status >= 500) {
                    if (attempt == maxAttempts) {
                        throw new IllegalStateException(String.format(
                                "智谱接口限流或暂不可用(HTTP %d)，已重试 %d 次仍失败。请稍后重试；若持续 429 多为免费额度耗尽，请到 open.bigmodel.cn 充值。",
                                status, maxAttempts), e);
                    }
                    long wait = backoffBase * attempt;
                    log.warn("智谱返回 HTTP {}（第{}次/共{}次），{}ms 后重试", status, attempt, maxAttempts, wait);
                    sleepQuietly(wait);
                } else if (status == 401 || status == 403) {
                    throw new IllegalStateException("智谱 API Key 无效或无权访问（HTTP " + status + "），请检查 application.yml 的 api-key。", e);
                } else if (status == 402 || body.contains("quota") || body.contains("额度")
                        || body.contains("insufficient") || body.contains("balance")) {
                    throw new IllegalStateException("智谱账号额度不足/已欠费（HTTP " + status + "），请到 open.bigmodel.cn 充值后重试。", e);
                } else {
                    throw new IllegalStateException("智谱接口错误(HTTP " + status + "): " + body, e);
                }
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("智谱文生图请求失败（重试 " + maxAttempts + " 次仍失败）: " + e.getMessage(), e);
                }
                long wait = backoffBase * attempt;
                log.warn("智谱请求异常（第{}次），{}ms 后重试: {}", attempt, wait, e.getMessage());
                sleepQuietly(wait);
            }
        }
        throw new IllegalStateException("智谱文生图调用失败（重试次数用尽）");
    }

    /** 下载生成的图片，遇到 429/5xx 时退避重试。 */
    private byte[] downloadImage(String imgUrl) {
        int maxAttempts = 4;
        long backoffBase = 3000L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return WebClient.create().get()
                        .uri(imgUrl)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block(Duration.ofSeconds(120));
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                if ((status == 429 || status >= 500) && attempt < maxAttempts) {
                    log.warn("下载全景图返回 HTTP {}，{}ms 后重试", status, backoffBase * attempt);
                    sleepQuietly(backoffBase * attempt);
                } else {
                    throw new IllegalStateException("下载智谱图片失败(HTTP " + status + ")", e);
                }
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("下载智谱图片失败: " + e.getMessage(), e);
                }
                sleepQuietly(backoffBase * attempt);
            }
        }
        throw new IllegalStateException("下载智谱图片失败（重试次数用尽）");
    }

    private static String safeBody(WebClientResponseException e) {
        try {
            return e.getResponseBodyAsString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildPrompt(DesignStyle style, String roomName) {

        return String.format(
                "写实风格的室内装修效果图，%s的%s，%s，柔和自然采光，精致的家具与软装搭配，" +
                        "材质真实，细节丰富，标准单点透视，正常空间比例，墙面与地面比例自然协调，" +
                        "摄影级质感，8K超高清照片",
                style.getDisplayName(), roomName, style.getTexturePrompt());
    }

    private record RoomSpec(int id, String name, List<Integer> connections) {}
}
