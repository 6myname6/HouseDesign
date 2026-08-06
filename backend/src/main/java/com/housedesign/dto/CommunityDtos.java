package com.housedesign.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommunityDtos {

    @Data
    public static class CreatePostRequest {
        private String content;
        private List<String> images = new ArrayList<>();
    }

    @Data
    public static class CommentRequest {
        @jakarta.validation.constraints.NotBlank(message = "评论内容不能为空")
        private String content;
    }

    @Data
    public static class LikeResult {
        private int likeCount;
        private boolean liked;
    }

    @Data
    public static class CommentResponse {
        private Long id;
        private Long postId;
        private Long userId;
        private String authorName;
        private String authorAvatar;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PostResponse {
        private Long id;
        private Long userId;
        private String authorName;
        private String authorAvatar;
        private String content;
        private List<String> images = new ArrayList<>();
        private int likeCount;
        private int commentCount;
        private boolean likedByMe;
        private LocalDateTime createdAt;
        private List<CommentResponse> comments = new ArrayList<>();
    }
}
