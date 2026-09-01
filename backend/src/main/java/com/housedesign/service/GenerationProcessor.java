package com.housedesign.service;

import com.housedesign.entity.DesignProject;
import com.housedesign.entity.GeneratedModel;
import com.housedesign.entity.GenerationStatus;
import com.housedesign.repository.DesignProjectRepository;
import com.housedesign.repository.GeneratedModelRepository;
import com.housedesign.service.ai.GenerationContext;
import com.housedesign.service.ai.GenerationOutput;
import com.housedesign.service.ai.ImageTo3DService;
import com.housedesign.service.ai.ImageTo3DServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步生成处理器（单独 bean，避免 @Async 自调用失效）。
 * 在独立线程中执行 AI 生成，并更新任务状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationProcessor {

    private final GeneratedModelRepository generatedModelRepository;
    private final DesignProjectRepository projectRepository;
    private final ImageTo3DServiceFactory serviceFactory;
    private final FileStorageService fileStorageService;

    /** 异步执行一次生成：标记 PROCESSING → 调 AI → 写回结果或 FAILED。 */
    @Async("generationExecutor")
    public void process(Long generatedModelId) {
        GeneratedModel model = generatedModelRepository.findById(generatedModelId).orElse(null);
        if (model == null) {
            log.warn("生成任务不存在: {}", generatedModelId);
            return;
        }
        model.setStatus(GenerationStatus.PROCESSING);
        generatedModelRepository.save(model);

        try {
            DesignProject project = projectRepository.findById(model.getProjectId())
                    .orElseThrow(() -> new IllegalStateException("项目不存在"));

            ImageTo3DService service = serviceFactory.resolve();
            GenerationContext ctx = GenerationContext.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .style(project.getStyle())
                    .imageAbsolutePath(fileStorageService.resolve(project.getDesignImagePath()))
                    .imageUrl(project.getDesignImageUrl())
                    .build();

            log.info("开始生成 3D 效果, taskId={}, provider={}", generatedModelId, service.provider());
            GenerationOutput output = service.generate(ctx);

            model.setStatus(GenerationStatus.SUCCESS);
            model.setProvider(output.getProvider());
            model.setModelUrl(output.getModelUrl());
            model.setPreviewImageUrl(output.getPreviewImageUrl());
            model.setPanoramaUrl(output.getPanoramaUrl());
            model.setSceneConfig(output.getSceneConfig());
            model.setErrorMessage(null);
            generatedModelRepository.save(model);
            log.info("生成完成, taskId={}", generatedModelId);
        } catch (Exception e) {
            log.error("生成失败, taskId={}", generatedModelId, e);
            model.setStatus(GenerationStatus.FAILED);
            String msg = e.getMessage();
            model.setErrorMessage(msg != null && msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            generatedModelRepository.save(model);
        }
    }
}
