package com.housedesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 认证相关 DTO 集合。
 */
public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        private String password;

        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;

        public AuthResponse(String token, Long userId, String username, String nickname, String avatar) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.avatar = avatar;
        }
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 64, message = "昵称长度不能超过 64")
        private String nickname;

        @Size(max = 512, message = "头像地址过长")
        private String avatar;
    }
}
