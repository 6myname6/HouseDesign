package com.housedesign.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 轻量密码加盐哈希工具（SHA-256 + 随机盐），存储格式为 {@code salt$hash}。
 * 注意：生产环境更推荐 BCrypt，此类为项目当前实现。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 对明文密码加盐哈希，返回 salt$hash 形式的编码串。 */
    public static String encode(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);
        String hash = hash(rawPassword, saltStr);
        return saltStr + "$" + hash;
    }

    /** 校验明文密码与编码串是否匹配（恒定时间比较，防时序攻击）。 */
    public static boolean matches(String rawPassword, String encoded) {
        if (encoded == null || !encoded.contains("$")) {
            return false;
        }
        String[] parts = encoded.split("\\$", 2);
        String hash = hash(rawPassword, parts[0]);
        return constantTimeEquals(hash, parts[1]);
    }

    /** 使用盐对明文做 SHA-256 哈希并 Base64 编码。 */
    private static String hash(String raw, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 恒定时间比较两字符串，避免因长度/内容差异导致的时间侧信道。 */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
