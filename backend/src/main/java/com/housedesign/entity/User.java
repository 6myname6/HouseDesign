package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库表 {@code t_user}。
 * 存储平台注册用户的基础信息与登录凭证；密码经 BCrypt 加密后存储，绝不保存明文。
 */
@Data
@Entity
@Table(name = "t_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {

    /** 用户主键，由数据库自增生成。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录用户名，全局唯一（唯一约束），长度 3-32 字符。 */
    @Column(nullable = false, length = 64)
    private String username;

    /** 登录密码，BCrypt 加密后的密文。 */
    @Column(nullable = false)
    private String password;

    /** 展示昵称，默认与用户名相同，可在个人信息中修改。 */
    @Column(length = 64)
    private String nickname;

    /** 头像图片的可访问 URL，可为空。 */
    @Column(length = 255)
    private String avatar;

    /** 账号创建时间，插入后不可更新。 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 持久化前回调：在首次插入数据库之前执行。
     * 用于自动填充创建时间，并在昵称为空时回退为用户名。
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.nickname == null || this.nickname.isBlank()) {
            this.nickname = this.username;
        }
    }
}
