package com.housedesign.controller;

import com.housedesign.common.Result;
import com.housedesign.dto.CommunityDtos;
import com.housedesign.security.CurrentUserId;
import com.housedesign.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 装修小圈（社区）接口：发帖、列表、删除、点赞切换、评论、删评论。
 * 路径前缀 /api/posts；所有接口需鉴权，且操作均基于当前登录用户 ID。
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /** 发布帖子（图文），作者为当前用户。 */
    @PostMapping
    public Result<CommunityDtos.PostResponse> create(
            @CurrentUserId Long userId, @Valid @RequestBody CommunityDtos.CreatePostRequest req) {
        return Result.success(communityService.create(userId, req));
    }

    /** 帖子列表：mine=false 取全部，mine=true 仅取当前用户帖子。 */
    @GetMapping
    public Result<List<CommunityDtos.PostResponse>> list(
            @CurrentUserId Long userId,
            @RequestParam(value = "mine", defaultValue = "false") boolean mine) {
        return Result.success(communityService.list(userId, mine));
    }

    /** 删除帖子（仅作者可删，由 service 做归属校验）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        communityService.deletePost(userId, id);
        return Result.success();
    }

    /** 点赞/取消点赞切换，返回最新点赞数与当前用户是否已赞。 */
    @PostMapping("/{id}/like")
    public Result<CommunityDtos.LikeResult> like(@CurrentUserId Long userId, @PathVariable Long id) {
        return Result.success(communityService.toggleLike(userId, id));
    }

    /** 对帖子发表评论（扁平结构），作者为当前用户。 */
    @PostMapping("/{id}/comments")
    public Result<CommunityDtos.CommentResponse> comment(
            @CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody CommunityDtos.CommentRequest req) {
        return Result.success(communityService.addComment(userId, id, req.getContent()));
    }

    /** 删除评论（仅作者可删）。 */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@CurrentUserId Long userId, @PathVariable Long id) {
        communityService.deleteComment(userId, id);
        return Result.success();
    }
}
