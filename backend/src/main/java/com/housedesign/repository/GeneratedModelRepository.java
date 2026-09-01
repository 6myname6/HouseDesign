package com.housedesign.repository;

import com.housedesign.entity.GeneratedModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 生成任务数据访问层。
 */
public interface GeneratedModelRepository extends JpaRepository<GeneratedModel, Long> {

    /** 查询某项目下的所有生成记录，按创建时间倒序。 */
    List<GeneratedModel> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /** 查询某用户的所有生成记录，按创建时间倒序。 */
    List<GeneratedModel> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按任务 ID 与用户 ID 联合查询，用于归属校验。 */
    Optional<GeneratedModel> findByIdAndUserId(Long id, Long userId);
}
