package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计项目实体，对应数据库表 {@code t_design_project}。
 * 一个项目 = 用户上传的一份房屋设计图 + 选定的一种装修风格，是生成 3D 效果的基本单位。
 */
@Data
@Entity
@Table(name = "t_design_project")
public class DesignProject {

    /** 项目主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目归属用户 ID（逻辑外键，指向 t_user.id），用于数据隔离与归属校验。 */
    @Column(nullable = false)
    private Long userId;

    /** 项目名称，必填。 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 项目描述，选填。 */
    @Column(length = 512)
    private String description;

    /** 装修风格 code，取值见 {@link DesignStyle}，非法或为空时回退「现代简约」。 */
    @Column(length = 32)
    private String style;

    /** 上传的设计图在服务器 storage 目录下的相对存储路径。 */
    @Column(length = 512)
    private String designImagePath;

    /** 设计图对外可访问的 URL。 */
    @Column(length = 512)
    private String designImageUrl;

    /** 创建时间，插入后不可更新。 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最后更新时间，每次更新自动刷新。 */
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
