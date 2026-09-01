package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.dto.GenerationDtos;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.GenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 3D 施工效果生成接口：发起生成、查询任务、查询记录。
 * 路径前缀 /api；所有接口需鉴权，且操作基于当前登录用户 ID（含归属校验）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationService generationService;

    /** 针对某项目发起一次生成：创建 PENDING 任务后异步处理，前端需轮询状态。 */
    @PostMapping("/projects/{projectId}/generate")
    public Result<GenerationDtos.GenerationResponse> generate(
            @CurrentUserId Long userId, @PathVariable Long projectId) {
        return Result.success(GenerationDtos.GenerationResponse.from(
                generationService.start(userId, projectId)));
    }

    /** 查询单个生成任务（前端轮询状态用）。 */
    @GetMapping("/generations/{id}")
    public Result<GenerationDtos.GenerationResponse> get(
            @CurrentUserId Long userId, @PathVariable Long id) {
        return Result.success(GenerationDtos.GenerationResponse.from(
                generationService.get(userId, id)));
    }

    /** 查询某项目下的生成记录（按创建时间倒序）。 */
    @GetMapping("/projects/{projectId}/generations")
    public Result<List<GenerationDtos.GenerationResponse>> listByProject(
            @CurrentUserId Long userId, @PathVariable Long projectId) {
        List<GenerationDtos.GenerationResponse> list = generationService.listByProject(userId, projectId)
                .stream().map(GenerationDtos.GenerationResponse::from).toList();
        return Result.success(list);
    }

    /** 查询当前用户所有生成记录。 */
    @GetMapping("/generations")
    public Result<List<GenerationDtos.GenerationResponse>> listByUser(@CurrentUserId Long userId) {
        List<GenerationDtos.GenerationResponse> list = generationService.listByUser(userId)
                .stream().map(GenerationDtos.GenerationResponse::from).toList();
        return Result.success(list);
    }
}
