package com.housedesign.entity;

import com.housedesign.common.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 装修小圈帖子实体，对应数据库表 {@code t_post}。
 * 用户可发布带图文的内容分享装修灵感，支持点赞与评论。
 */
@Data
@Entity
@Table(name = "t_post")
public class Post {

    /** 帖子主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 作者用户 ID（逻辑外键，指向 t_user.id）。 */
    @Column(nullable = false)
    private Long userId;

    /** 帖子正文内容。 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 图片地址列表，以 JSON 字符串形式存储（经 StringListConverter 与 List<String> 互转）。 */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> images = new ArrayList<>();

    /** 点赞数，冗余计数器，避免列表查询时频繁 COUNT。 */
    @Column(nullable = false)
    private int likeCount = 0;

    /** 评论数，冗余计数器，与 t_post_comment 的真实数量应保持同步。 */
    @Column(nullable = false)
    private int commentCount = 0;

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
