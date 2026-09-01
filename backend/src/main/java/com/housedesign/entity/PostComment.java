package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子评论实体，对应数据库表 {@code t_post_comment}。
 * 采用扁平结构，暂不区分楼中楼回复。
 */
@Data
@Entity
@Table(name = "t_post_comment")
public class PostComment {

    /** 评论主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属帖子 ID（逻辑外键，指向 t_post.id）。 */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** 评论者用户 ID（逻辑外键，指向 t_user.id）。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 评论内容，非空。 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 创建时间，插入后不可更新。 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 持久化前回调：插入时填充创建时间。 */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
