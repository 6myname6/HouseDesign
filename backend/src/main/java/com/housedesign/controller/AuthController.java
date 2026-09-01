package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.dto.AuthDtos;
import com.housedesign.entity.User;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：提供注册、登录、获取/更新当前用户信息。
 * 路径前缀 /api/auth；注册与登录无需鉴权，其余需携带 JWT。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 用户注册：校验用户名/密码后创建账号并签发 JWT。无需鉴权。 */
    @PostMapping("/register")
    public Result<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return Result.success(userService.register(req));
    }

    /** 用户登录：校验凭据后签发 JWT。无需鉴权。 */
    @PostMapping("/login")
    public Result<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return Result.success(userService.login(req));
    }

    /** 获取当前登录用户的基本信息（由 @CurrentUserId 注入用户 ID）。 */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(@CurrentUserId Long userId) {
        User user = userService.getById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        return Result.success(data);
    }

    /** 更新当前用户的昵称与头像（字段均可选）。 */
    @PutMapping("/me")
    public Result<AuthDtos.AuthResponse> updateProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest req) {
        return Result.success(userService.updateProfile(userId, req));
    }
}
