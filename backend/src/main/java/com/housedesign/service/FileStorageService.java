package com.housedesign.service;

import com.housedesign.common.BusinessException;
import com.housedesign.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 本地文件存储服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    private Path root;

    @PostConstruct
    public void init() {
        this.root = Paths.get(appProperties.getStorage().getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("文件存储目录: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建存储目录: " + root, e);
        }
    }

    /**
     * 保存上传文件。
     *
     * @param subDir 子目录，如 "designs"
     * @return 相对路径（相对 storage 根）
     */
    public StoredFile store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = subDir + "/" + filename;
        Path target = root.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
        return new StoredFile(relativePath, buildPublicUrl(relativePath));
    }

    /**
     * 写入字节内容（如生成的模型文件）。
     */
    public StoredFile storeBytes(byte[] content, String subDir, String ext) {
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = subDir + "/" + filename;
        Path target = root.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException("文件写入失败: " + e.getMessage());
        }
        return new StoredFile(relativePath, buildPublicUrl(relativePath));
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath).normalize();
    }

    public String buildPublicUrl(String relativePath) {
        String base = appProperties.getStorage().getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + relativePath;
    }

    private String getExtension(String originalFilename) {
        String ext = StringUtils.getFilenameExtension(originalFilename);
        return ext == null ? "" : "." + ext.toLowerCase();
    }

    public record StoredFile(String relativePath, String url) {
    }
}
