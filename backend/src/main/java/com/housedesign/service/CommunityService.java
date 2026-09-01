package com.housedesign.service;

import com.housedesign.common.BusinessException;
import com.housedesign.dto.CommunityDtos;
import com.housedesign.entity.Post;
import com.housedesign.entity.PostComment;
import com.housedesign.entity.PostLike;
import com.housedesign.entity.User;
import com.housedesign.repository.PostCommentRepository;
import com.housedesign.repository.PostLikeRepository;
import com.housedesign.repository.PostRepository;
import com.housedesign.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 装修小圈业务服务：负责帖子、点赞、评论的增删查与归属校验。
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;

    /** 发布帖子：校验内容与图片不能同时为空，落库后组装响应。 */
    public CommunityDtos.PostResponse create(Long userId, CommunityDtos.CreatePostRequest req) {
        List<String> images = req.getImages() == null ? List.of() : req.getImages();
        String content = req.getContent() == null ? "" : req.getContent().strip();
        if (content.isBlank() && images.isEmpty()) {
            throw new BusinessException("内容和图片不能同时为空");
        }
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(images);
        post = postRepository.save(post);

        Map<Long, User> users = new HashMap<>();
        userRepository.findById(userId).ifPresent(u -> users.put(userId, u));
        return toResponse(post, users, List.of(), false);
    }

    /** 帖子列表：mine=true 仅当前用户，否则全部；批量补作者信息与当前用户点赞状态。 */
    public List<CommunityDtos.PostResponse> list(Long userId, boolean mine) {
        List<Post> posts = mine
                ? postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : postRepository.findAllByOrderByCreatedAtDesc();

        Set<Long> userIds = new HashSet<>();
        Map<Long, List<PostComment>> commentsByPost = new HashMap<>();
        for (Post p : posts) {
            userIds.add(p.getUserId());
            List<PostComment> cs = postCommentRepository.findByPostIdOrderByCreatedAtAsc(p.getId());
            commentsByPost.put(p.getId(), cs);
            cs.forEach(c -> userIds.add(c.getUserId()));
        }

        Map<Long, User> users = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> users.put(u.getId(), u));

        // 当前用户点过赞的帖子集合，用于标记 likedByMe
        Set<Long> likedPostIds = postLikeRepository.findByUserId(userId).stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toSet());

        return posts.stream()
                .map(p -> toResponse(p, users, commentsByPost.get(p.getId()), likedPostIds.contains(p.getId())))
                .toList();
    }

    /** 点赞/取消点赞切换，并同步更新帖子点赞计数。 */
    @Transactional
    public CommunityDtos.LikeResult toggleLike(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        if (liked) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        postRepository.save(post);
        CommunityDtos.LikeResult r = new CommunityDtos.LikeResult();
        r.setLikeCount(post.getLikeCount());
        r.setLiked(!liked);
        return r;
    }

    /** 发表评论：校验内容非空，落库后同步帖子评论计数。 */
    @Transactional
    public CommunityDtos.CommentResponse addComment(Long userId, Long postId, String content) {
        if (content == null || content.strip().isBlank()) {
            throw new BusinessException("评论内容不能为空");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setContent(content.strip());
        c = postCommentRepository.save(c);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        Map<Long, User> users = new HashMap<>();
        userRepository.findById(userId).ifPresent(u -> users.put(userId, u));
        return toCommentResponse(c, users.get(userId));
    }

    /** 删除帖子：校验归属后级联删除其点赞与评论。 */
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该帖子");
        }
        postLikeRepository.deleteByPostId(postId);
        postCommentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    /** 删除评论：校验归属后删除，并回退帖子评论计数。 */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        PostComment c = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该评论");
        }
        postCommentRepository.delete(c);
        postRepository.findById(c.getPostId()).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            postRepository.save(p);
        });
    }

    /** 将帖子实体组装为响应 DTO，补充作者信息与评论列表。 */
    private CommunityDtos.PostResponse toResponse(Post p, Map<Long, User> users,
                                                  List<PostComment> comments, boolean likedByMe) {
        User author = users.get(p.getUserId());
        CommunityDtos.PostResponse r = new CommunityDtos.PostResponse();
        r.setId(p.getId());
        r.setUserId(p.getUserId());
        r.setAuthorName(authorName(author, p.getUserId()));
        r.setAuthorAvatar(author == null ? null : author.getAvatar());
        r.setContent(p.getContent());
        r.setImages(p.getImages());
        r.setLikeCount(p.getLikeCount());
        r.setCommentCount(p.getCommentCount());
        r.setLikedByMe(likedByMe);
        r.setCreatedAt(p.getCreatedAt());
        if (comments != null) {
            r.setComments(comments.stream()
                    .map(c -> toCommentResponse(c, users.get(c.getUserId())))
                    .toList());
        }
        return r;
    }

    /** 将评论实体组装为响应 DTO，补充作者信息。 */
    private CommunityDtos.CommentResponse toCommentResponse(PostComment c, User author) {
        CommunityDtos.CommentResponse r = new CommunityDtos.CommentResponse();
        r.setId(c.getId());
        r.setPostId(c.getPostId());
        r.setUserId(c.getUserId());
        r.setAuthorName(authorName(author, c.getUserId()));
        r.setAuthorAvatar(author == null ? null : author.getAvatar());
        r.setContent(c.getContent());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }

    /** 计算作者展示名：昵称为空时回退用户名，用户不存在时显示"用户+ID"。 */
    private String authorName(User u, Long fallbackId) {
        if (u == null) {
            return "用户" + fallbackId;
        }
        return (u.getNickname() == null || u.getNickname().isBlank()) ? u.getUsername() : u.getNickname();
    }
}
