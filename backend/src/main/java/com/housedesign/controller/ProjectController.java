package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.dto.ProjectDtos;
import com.housedesign.entity.DesignProject;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计项目接口：创建（含设计图上传与风格选择）、列表、详情、删除。
 * 路径前缀 /api/projects；所有接口需鉴权，仅能操作自己的项目。
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 创建项目并上传设计图（multipart 表单），作者为当前用户。 */
    @PostMapping(consumes = "multipart/form-data")
    public Result<ProjectDtos.ProjectResponse> create(
            @CurrentUserId Long userId,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam("designImage") MultipartFile designImage) {
        DesignProject project = projectService.create(userId, name, description, style, designImage);
        return Result.success(ProjectDtos.ProjectResponse.from(project));
    }

    /** 当前用户的所有项目列表。 */
    @GetMapping
    public Result<List<ProjectDtos.ProjectResponse>> list(@CurrentUserId Long userId) {
        List<ProjectDtos.ProjectResponse> list = projectService.list(userId).stream()
                .map(ProjectDtos.ProjectResponse::from)
                .toList();
        return Result.success(list);
    }

    /** 项目详情（含归属校验）。 */
    @GetMapping("/{id}")
    public Result<ProjectDtos.ProjectResponse> get(@CurrentUserId Long userId, @PathVariable Long id) {
        return Result.success(ProjectDtos.ProjectResponse.from(projectService.get(userId, id)));
    }

    /** 删除项目（仅作者可删，含归属校验）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        projectService.delete(userId, id);
        return Result.success();
    }
}
