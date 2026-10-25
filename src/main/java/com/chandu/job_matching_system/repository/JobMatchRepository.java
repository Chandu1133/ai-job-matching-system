package com.chandu.job_matching_system.repository;

import com.chandu.job_matching_system.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    List<JobMatch> findByUserIdOrderByCalculatedAtDesc(Long userId);

    Optional<JobMatch> findFirstByUserIdAndJobIdOrderByCalculatedAtDesc(Long userId, Long jobId);
}
