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
 * 装修小圈接口。
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping
    public Result<CommunityDtos.PostResponse> create(
            @CurrentUserId Long userId, @Valid @RequestBody CommunityDtos.CreatePostRequest req) {
        return Result.success(communityService.create(userId, req));
    }

    @GetMapping
    public Result<List<CommunityDtos.PostResponse>> list(
            @CurrentUserId Long userId,
            @RequestParam(value = "mine", defaultValue = "false") boolean mine) {
        return Result.success(communityService.list(userId, mine));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        communityService.deletePost(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result<CommunityDtos.LikeResult> like(@CurrentUserId Long userId, @PathVariable Long id) {
        return Result.success(communityService.toggleLike(userId, id));
    }

    @PostMapping("/{id}/comments")
    public Result<CommunityDtos.CommentResponse> comment(
            @CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody CommunityDtos.CommentRequest req) {
        return Result.success(communityService.addComment(userId, id, req.getContent()));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@CurrentUserId Long userId, @PathVariable Long id) {
        communityService.deleteComment(userId, id);
        return Result.success();
    }
}
