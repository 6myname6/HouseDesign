package com.housedesign.repository;

import com.housedesign.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 帖子点赞数据访问层。
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /** 判断某用户是否已赞某帖子。 */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /** 统计某帖子的点赞总数。 */
    long countByPostId(Long postId);

    /** 删除某用户对某帖子的点赞记录（取消赞用）。 */
    void deleteByPostIdAndUserId(Long postId, Long userId);

    /** 删除某帖子的全部点赞记录（删帖时级联清理用）。 */
    void deleteByPostId(Long postId);

    /** 查询某用户点过赞的所有记录。 */
    List<PostLike> findByUserId(Long userId);
}
