package com.housedesign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用自定义配置项，对应 application.yml 中的 app.* 节点。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Storage storage = new Storage();
    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();

    @Data
    public static class Storage {
        private String location = "./storage";
        private String publicBaseUrl = "http://localhost:8080/files";
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 604800000L;
    }

    @Data
    public static class Ai {
        private String provider = "mock";
        private String apiKey = "";
        private String baseUrl = "";
        /** 文生图模型（智谱用 cogview-4 / cogview-3-flash / glm-image）；provider=meshy 时忽略 */
        private String imageModel = "cogview-4";
        private int timeoutSeconds = 300;
    }
}
