package com.chandu.job_matching_system.repository;

import com.chandu.job_matching_system.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    boolean existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
            String title,
            String company,
            String location
    );
}
