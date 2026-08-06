package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传接口，返回可访问的 URL。用于帖子图片、头像等。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<String> upload(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) {
        FileStorageService.StoredFile stored = fileStorageService.store(file, "uploads");
        return Result.success(stored.url());
    }
}
