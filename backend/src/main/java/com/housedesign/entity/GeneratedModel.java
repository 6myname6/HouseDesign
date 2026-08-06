package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成的 3D 施工效果结果。
 */
@Data
@Entity
@Table(name = "t_generated_model")
public class GeneratedModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GenerationStatus status = GenerationStatus.PENDING;

    /** 使用的 AI 提供方 */
    @Column(length = 32)
    private String provider;

    /** 生成的 3D 模型（glb/gltf）可访问 URL，可能为空（程序化场景时） */
    @Column(length = 512)
    private String modelUrl;

    /** 预览缩略图 URL */
    @Column(length = 512)
    private String previewImageUrl;

    /** 全景图（等距柱状 equirectangular）URL，真实 AI 出图时填充，前端以 720° 球体内贴图展示 */
    @Column(length = 512)
    private String panoramaUrl;

    /** 3D 场景描述 JSON（前端程序化渲染时使用） */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sceneConfig;

    /** 失败原因 */
    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
