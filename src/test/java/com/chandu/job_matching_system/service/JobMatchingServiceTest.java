package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.JobMatchResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.JobMatch;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobMatchRepository;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobMatchingServiceTest {

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    private JobMatchingService jobMatchingService;

    @BeforeEach
    void setUp() {
        jobMatchingService = new JobMatchingService(
                jobMatchRepository,
                jobRepository,
                resumeRepository,
                userRepository,
                JsonMapper.builder().build(),
                "" // Empty URL triggers deterministic local calculation in unit tests
        );
    }

    @Test
    void calculateAndSaveMatchShouldReturnStrongMatchScore() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        user.setSkills("Java, Spring Boot, MySQL, REST APIs");
        user.setExperienceYears(2);
        user.setEducation("B.Tech Computer Science");

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Java Backend Engineer");
        job.setCompany("TechNova");
        job.setDescription("Build scalable Java Spring Boot REST APIs with MySQL.");
        job.setRequiredSkills("Java, Spring Boot, MySQL, REST APIs");
        job.setMinExperienceYears(1);

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(jobMatchRepository.findFirstByUserIdAndJobIdOrderByCalculatedAtDesc(1L, 10L))
                .thenReturn(Optional.empty());
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> {
            JobMatch m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });

        JobMatchResponse response = jobMatchingService.calculateAndSaveMatch(10L, null, "chandu@example.com");

        assertEquals(99L, response.getId());
        assertTrue(response.getMatchScore() >= 85);
        assertEquals(List.of("Java", "Spring Boot", "MySQL", "REST APIs"), response.getMatchedSkills());
        assertTrue(response.getMissingSkills().isEmpty());
        assertTrue(response.getExplanation().contains("Strong skill alignment"));
    }

    @Test
    void calculateAndSaveMatchShouldIdentifyMissingSkillsInPartialMatch() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        user.setSkills("Java, Spring Boot, MySQL");
        user.setExperienceYears(2);

        Job job = new Job();
        job.setId(11L);
        job.setTitle("Backend Developer");
        job.setCompany("CloudScale");
        job.setDescription("Java Spring Boot developer with Docker experience.");
        job.setRequiredSkills("Java, Spring Boot, MySQL, Docker");
        job.setMinExperienceYears(2);

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(11L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(jobMatchRepository.findFirstByUserIdAndJobIdOrderByCalculatedAtDesc(1L, 11L))
                .thenReturn(Optional.empty());
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        JobMatchResponse response = jobMatchingService.calculateAndSaveMatch(11L, null, "chandu@example.com");

        assertTrue(response.getMatchScore() >= 60 && response.getMatchScore() <= 90);
        assertEquals(List.of("Java", "Spring Boot", "MySQL"), response.getMatchedSkills());
        assertEquals(List.of("Docker"), response.getMissingSkills());
    }
}
