package com.housedesign.repository;

import com.housedesign.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 帖子数据访问层。
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 查询全部帖子，按创建时间倒序（社区广场用）。 */
    List<Post> findAllByOrderByCreatedAtDesc();

    /** 查询某用户的全部帖子，按创建时间倒序（我的帖子用）。 */
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
}
