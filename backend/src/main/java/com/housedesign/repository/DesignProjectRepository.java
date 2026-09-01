package com.housedesign.repository;

import com.housedesign.entity.DesignProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 设计项目数据访问层。
 */
public interface DesignProjectRepository extends JpaRepository<DesignProject, Long> {

    /** 查询某用户的所有项目，按创建时间倒序。 */
    List<DesignProject> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按项目 ID 与用户 ID 联合查询，用于归属校验（防止越权访问他人项目）。 */
    Optional<DesignProject> findByIdAndUserId(Long id, Long userId);
}
