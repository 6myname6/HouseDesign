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
 * 认证接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return Result.success(userService.register(req));
    }

    @PostMapping("/login")
    public Result<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return Result.success(userService.login(req));
    }

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

    @PutMapping("/me")
    public Result<AuthDtos.AuthResponse> updateProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest req) {
        return Result.success(userService.updateProfile(userId, req));
    }
}
