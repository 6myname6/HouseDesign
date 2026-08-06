package com.housedesign.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户。
 */
@Data
@Entity
@Table(name = "t_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 64)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.nickname == null || this.nickname.isBlank()) {
            this.nickname = this.username;
        }
    }
}
