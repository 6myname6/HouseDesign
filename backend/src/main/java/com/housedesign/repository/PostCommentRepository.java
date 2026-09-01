package com.housedesign.repository;

import com.housedesign.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 帖子评论数据访问层。
 */
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /** 查询某帖子的全部评论，按创建时间正序。 */
    List<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /** 统计某帖子的评论数。 */
    long countByPostId(Long postId);

    /** 删除某帖子的全部评论（删帖时级联清理用）。 */
    void deleteByPostId(Long postId);
}
