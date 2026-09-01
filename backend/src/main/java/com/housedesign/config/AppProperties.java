package com.housedesign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用自定义配置项，绑定 application.yml 中的 {@code app.*} 节点。
 * 通过 {@code @ConfigurationProperties(prefix = "app")} 自动注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文件存储相关配置。 */
    private Storage storage = new Storage();
    /** JWT 相关配置。 */
    private Jwt jwt = new Jwt();
    /** AI 图生 3D 相关配置。 */
    private Ai ai = new Ai();

    /** 文件存储配置，对应 app.storage.*。 */
    @Data
    public static class Storage {
        /** 存储根目录（默认 ./storage），可通过 app.storage.location 配置。 */
        private String location = "./storage";
        /** 对外访问的基础 URL（前端拼接资源地址用），默认 http://localhost:8080/files。 */
        private String publicBaseUrl = "http://localhost:8080/files";
    }

    /** JWT 配置，对应 app.jwt.*。 */
    @Data
    public static class Jwt {
        /** 签名密钥，生产环境务必通过环境变量注入，切勿硬编码。 */
        private String secret;
        /** 令牌有效期（毫秒），默认 7 天。 */
        private long expirationMs = 604800000L;
    }

    /** AI 配置，对应 app.ai.*。 */
    @Data
    public static class Ai {
        /** AI 提供方：mock（内置程序化生成）/ zhipu（智谱）/ meshy / tripo / custom。 */
        private String provider = "mock";
        /** API Key，通过环境变量注入。 */
        private String apiKey = "";
        /** 接口基础地址。 */
        private String baseUrl = "";
        /** 文生图模型（智谱用 cogview-4 / cogview-3-flash / glm-image）；provider=meshy 时忽略。 */
        private String imageModel = "cogview-4";
        /** 生成结果轮询超时（秒）。 */
        private int timeoutSeconds = 300;
    }
}
