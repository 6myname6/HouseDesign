package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传接口：接收上传文件并持久化到本地存储，返回可访问 URL。
 * 用于帖子图片、用户头像等。路径前缀 /api/files；需鉴权。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 上传单个文件（字段名 file），存入 uploads 子目录并返回访问 URL。 */
    @PostMapping("/upload")
    public Result<String> upload(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) {
        FileStorageService.StoredFile stored = fileStorageService.store(file, "uploads");
        return Result.success(stored.url());
    }
}
