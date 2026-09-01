package com.housedesign.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 装修小圈（社区）相关 DTO 集合，封装帖子、评论、点赞的请求与响应对象。
 */
public class CommunityDtos {

    /** 发帖请求。 */
    @Data
    public static class CreatePostRequest {
        /** 帖子正文。 */
        private String content;
        /** 帖子图片 URL 列表。 */
        private List<String> images = new ArrayList<>();
    }

    /** 评论请求。 */
    @Data
    public static class CommentRequest {
        /** 评论内容，非空。 */
        @jakarta.validation.constraints.NotBlank(message = "评论内容不能为空")
        private String content;
    }

    /** 点赞操作结果。 */
    @Data
    public static class LikeResult {
        /** 帖子当前总点赞数。 */
        private int likeCount;
        /** 当前用户是否已赞（true 表示已赞）。 */
        private boolean liked;
    }

    /** 单条评论的响应。 */
    @Data
    public static class CommentResponse {
        /** 评论 ID。 */
        private Long id;
        /** 所属帖子 ID。 */
        private Long postId;
        /** 评论者用户 ID。 */
        private Long userId;
        /** 评论者昵称。 */
        private String authorName;
        /** 评论者头像 URL。 */
        private String authorAvatar;
        /** 评论内容。 */
        private String content;
        /** 评论时间。 */
        private LocalDateTime createdAt;
    }

    /** 帖子详情响应，内嵌评论列表。 */
    @Data
    public static class PostResponse {
        /** 帖子 ID。 */
        private Long id;
        /** 作者用户 ID。 */
        private Long userId;
        /** 作者昵称。 */
        private String authorName;
        /** 作者头像 URL。 */
        private String authorAvatar;
        /** 帖子正文。 */
        private String content;
        /** 帖子图片 URL 列表。 */
        private List<String> images = new ArrayList<>();
        /** 点赞数。 */
        private int likeCount;
        /** 评论数。 */
        private int commentCount;
        /** 当前用户是否已赞。 */
        private boolean likedByMe;
        /** 发布时间。 */
        private LocalDateTime createdAt;
        /** 评论列表（扁平结构）。 */
        private List<CommentResponse> comments = new ArrayList<>();
    }
}
