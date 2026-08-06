package com.housedesign.service;

import com.housedesign.common.BusinessException;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
import com.housedesign.repository.DesignProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计项目服务。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final DesignProjectRepository projectRepository;
    private final FileStorageService fileStorageService;

    public DesignProject create(Long userId, String name, String description, String style, MultipartFile designImage) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }
        FileStorageService.StoredFile stored = fileStorageService.store(designImage, "designs");
        DesignProject project = new DesignProject();
        project.setUserId(userId);
        project.setName(name);
        project.setDescription(description);
        // 规范化风格 code（非法/为空时回退到默认风格）
        project.setStyle(DesignStyle.fromCode(style).getCode());
        project.setDesignImagePath(stored.relativePath());
        project.setDesignImageUrl(stored.url());
        return projectRepository.save(project);
    }

    public List<DesignProject> list(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public DesignProject get(Long userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(404, "项目不存在"));
    }

    public void delete(Long userId, Long projectId) {
        DesignProject project = get(userId, projectId);
        projectRepository.delete(project);
    }
}
