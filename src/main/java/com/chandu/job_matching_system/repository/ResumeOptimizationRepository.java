package com.chandu.job_matching_system.repository;

import com.chandu.job_matching_system.entity.ResumeOptimization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeOptimizationRepository extends JpaRepository<ResumeOptimization, Long> {

    List<ResumeOptimization> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ResumeOptimization> findByIdAndUserId(Long id, Long userId);
}
