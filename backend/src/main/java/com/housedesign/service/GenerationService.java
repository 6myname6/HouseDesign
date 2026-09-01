package com.housedesign.service;

import com.housedesign.common.BusinessException;
import com.housedesign.entity.DesignProject;
import com.housedesign.entity.GeneratedModel;
import com.housedesign.entity.GenerationStatus;
import com.housedesign.repository.GeneratedModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生成任务门面服务：对外提供发起生成、查询任务/记录的接口，
 * 负责归属校验并触发异步处理。
 */
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final GeneratedModelRepository generatedModelRepository;
    private final ProjectService projectService;
    private final GenerationProcessor generationProcessor;

    /** 发起一次生成：校验项目归属 → 落库 PENDING → 触发异步处理。 */
    public GeneratedModel start(Long userId, Long projectId) {
        DesignProject project = projectService.get(userId, projectId);
        if (project.getDesignImagePath() == null) {
            throw new BusinessException("项目缺少设计图，无法生成");
        }
        GeneratedModel model = new GeneratedModel();
        model.setProjectId(project.getId());
        model.setUserId(userId);
        model.setStatus(GenerationStatus.PENDING);
        model = generatedModelRepository.save(model);

        generationProcessor.process(model.getId());
        return model;
    }

    /** 查询单个生成任务（含归属校验）。 */
    public GeneratedModel get(Long userId, Long id) {
        return generatedModelRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(404, "生成任务不存在"));
    }

    /** 查询某项目下的生成记录（含归属校验）。 */
    public List<GeneratedModel> listByProject(Long userId, Long projectId) {
        projectService.get(userId, projectId); // 校验归属
        return generatedModelRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /** 查询当前用户所有生成记录。 */
    public List<GeneratedModel> listByUser(Long userId) {
        return generatedModelRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
