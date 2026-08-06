package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计项目：一个用户上传的一份房屋设计图对应一个项目。
 */
@Data
@Entity
@Table(name = "t_design_project")
public class DesignProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    /** 装修风格 code，见 {@link DesignStyle} */
    @Column(length = 32)
    private String style;

    /** 上传的设计图相对路径（storage 下） */
    @Column(length = 512)
    private String designImagePath;

    /** 设计图可访问 URL */
    @Column(length = 512)
    private String designImageUrl;

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
