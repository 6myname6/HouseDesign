package com.housedesign.repository;

import com.housedesign.entity.DesignProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignProjectRepository extends JpaRepository<DesignProject, Long> {

    List<DesignProject> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DesignProject> findByIdAndUserId(Long id, Long userId);
}
