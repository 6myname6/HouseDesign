package com.housedesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 认证相关 DTO 集合，封装注册/登录/个人信息等接口的请求与响应对象。
 */
public class AuthDtos {

    /** 注册请求：用户名、密码必填，昵称选填。 */
    @Data
    public static class RegisterRequest {
        /** 用户名，3-32 字符，全局唯一。 */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
        private String username;

        /** 登录密码，6-64 字符。 */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        private String password;

        /** 展示昵称，选填，默认取用户名。 */
        private String nickname;
    }

    /** 登录请求：用户名 + 密码。 */
    @Data
    public static class LoginRequest {
        /** 登录用户名。 */
        @NotBlank(message = "用户名不能为空")
        private String username;

        /** 登录密码。 */
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /** 认证响应：返回 JWT 与用户基本信息。 */
    @Data
    public static class AuthResponse {
        /** 登录/注册后签发的 JWT 令牌。 */
        private String token;
        /** 用户 ID。 */
        private Long userId;
        /** 用户名。 */
        private String username;
        /** 昵称。 */
        private String nickname;
        /** 头像 URL。 */
        private String avatar;

        /** 全参构造，用于业务层组装响应。 */
        public AuthResponse(String token, Long userId, String username, String nickname, String avatar) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.avatar = avatar;
        }
    }

    /** 更新个人信息请求：昵称与头像均可选。 */
    @Data
    public static class UpdateProfileRequest {
        /** 新昵称，最长 64 字符。 */
        @Size(max = 64, message = "昵称长度不能超过 64")
        private String nickname;

        /** 新头像 URL，最长 512 字符。 */
        @Size(max = 512, message = "头像地址过长")
        private String avatar;
    }
}
