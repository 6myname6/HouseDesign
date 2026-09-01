package com.housedesign.dto;

import com.housedesign.entity.GeneratedModel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成结果相关 DTO，封装 3D 生成任务的响应对象。
 */
public class GenerationDtos {

    /** 生成任务响应，字段与 GeneratedModel 对应，status 以字符串形式暴露。 */
    @Data
    public static class GenerationResponse {
        /** 生成任务 ID。 */
        private Long id;
        /** 所属项目 ID。 */
        private Long projectId;
        /** 任务状态（PENDING/PROCESSING/SUCCESS/FAILED）。 */
        private String status;
        /** 实际使用的 AI 提供方。 */
        private String provider;
        /** 3D 模型 URL。 */
        private String modelUrl;
        /** 预览缩略图 URL。 */
        private String previewImageUrl;
        /** 全景图 URL。 */
        private String panoramaUrl;
        /** 3D 场景描述 JSON。 */
        private String sceneConfig;
        /** 失败原因。 */
        private String errorMessage;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 更新时间。 */
        private LocalDateTime updatedAt;

        /** 由实体对象转换为响应 DTO。 */
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
