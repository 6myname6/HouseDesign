package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成任务与结果实体，对应数据库表 {@code t_generated_model}。
 * 每次用户对某个项目发起「生成 3D 效果」都会创建一条记录，状态从
 * PENDING → PROCESSING → SUCCESS/FAILED 流转，最终产物（模型/全景图/场景配置）落在此处。
 */
@Data
@Entity
@Table(name = "t_generated_model")
public class GeneratedModel {

    /** 生成任务主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属项目 ID（逻辑外键，指向 t_design_project.id）。 */
    @Column(nullable = false)
    private Long projectId;

    /** 发起生成任务的用户 ID（逻辑外键，指向 t_user.id），冗余存储便于按用户查询。 */
    @Column(nullable = false)
    private Long userId;

    /** 任务状态，枚举字符串，默认 PENDING；取值见 {@link GenerationStatus}。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GenerationStatus status = GenerationStatus.PENDING;

    /** 实际使用的 AI 提供方（如 mock / zhipu）。 */
    @Column(length = 32)
    private String provider;

    /** 生成的 3D 模型（glb/gltf）可访问 URL；程序化场景时为 null。 */
    @Column(length = 512)
    private String modelUrl;

    /** 预览缩略图 URL。 */
    @Column(length = 512)
    private String previewImageUrl;

    /** 全景图（等距柱状 equirectangular）URL，真实 AI 出图时填充，前端以 720° 球体内贴图展示。 */
    @Column(length = 512)
    private String panoramaUrl;

    /** 3D 场景描述 JSON（前端程序化渲染时使用），数据量较大故用 LONGTEXT。 */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sceneConfig;

    /** 失败原因描述，任务状态为 FAILED 时填充（如 AI 限流/额度不足/Key 无效）。 */
    @Column(length = 1024)
    private String errorMessage;

    /** 创建时间，插入后不可更新。 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最后更新时间，每次状态变更自动刷新。 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 持久化前回调：插入时填充创建/更新时间。 */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** 更新前回调：每次更新时刷新更新时间。 */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
