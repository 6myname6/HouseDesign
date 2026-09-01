package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞实体，对应数据库表 {@code t_post_like}。
 * 一条记录代表「某用户对某帖子点过赞」；(post_id, user_id) 唯一，保证一人一帖只赞一次。
 */
@Data
@Entity
@Table(name = "t_post_like", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostLike {

    /** 主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被赞帖子 ID（逻辑外键，指向 t_post.id）。 */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** 点赞用户 ID（逻辑外键，指向 t_user.id）。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 点赞时间，插入后不可更新。 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 持久化前回调：插入时填充点赞时间。 */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
