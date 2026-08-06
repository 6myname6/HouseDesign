package com.housedesign.repository;

import com.housedesign.entity.GeneratedModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedModelRepository extends JpaRepository<GeneratedModel, Long> {

    List<GeneratedModel> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<GeneratedModel> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GeneratedModel> findByIdAndUserId(Long id, Long userId);
}
