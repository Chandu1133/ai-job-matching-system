package com.chandu.job_matching_system.repository;

import com.chandu.job_matching_system.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    List<JobApplication> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
