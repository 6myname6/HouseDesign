package com.housedesign.service;

import com.housedesign.common.BusinessException;
import com.housedesign.dto.AuthDtos;
import com.housedesign.entity.User;
import com.housedesign.repository.UserRepository;
import com.housedesign.security.JwtUtil;
import com.housedesign.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务：负责注册、登录、个人信息更新与按 ID 查询。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /** 注册：校验用户名唯一 → 加盐哈希密码 → 落库 → 签发 JWT。 */
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(PasswordUtil.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    /** 登录：校验用户名存在与密码匹配 → 签发 JWT。 */
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!PasswordUtil.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    /** 更新个人资料（昵称 / 头像），仅更新非空字段。 */
    public AuthDtos.AuthResponse updateProfile(Long userId, AuthDtos.UpdateProfileRequest req) {
        User user = getById(userId);
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        user = userRepository.save(user);
        return new AuthDtos.AuthResponse(null, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    /** 按 ID 查询用户（含存在性校验）。 */
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
