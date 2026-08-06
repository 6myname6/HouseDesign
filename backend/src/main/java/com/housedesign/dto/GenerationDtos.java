package com.housedesign.dto;

import com.housedesign.entity.GeneratedModel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成结果相关 DTO。
 */
public class GenerationDtos {

    @Data
    public static class GenerationResponse {
        private Long id;
        private Long projectId;
        private String status;
        private String provider;
        private String modelUrl;
        private String previewImageUrl;
        private String panoramaUrl;
        private String sceneConfig;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static GenerationResponse from(GeneratedModel m) {
            GenerationResponse r = new GenerationResponse();
            r.id = m.getId();
            r.projectId = m.getProjectId();
            r.status = m.getStatus().name();
            r.provider = m.getProvider();
            r.modelUrl = m.getModelUrl();
            r.previewImageUrl = m.getPreviewImageUrl();
            r.panoramaUrl = m.getPanoramaUrl();
            r.sceneConfig = m.getSceneConfig();
            r.errorMessage = m.getErrorMessage();
            r.createdAt = m.getCreatedAt();
            r.updatedAt = m.getUpdatedAt();
            return r;
        }
    }
}
