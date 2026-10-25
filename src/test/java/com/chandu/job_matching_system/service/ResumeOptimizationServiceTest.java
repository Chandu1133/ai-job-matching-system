package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.ResumeOptimizationApprovalRequest;
import com.chandu.job_matching_system.dto.ResumeOptimizationResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.Resume;
import com.chandu.job_matching_system.entity.ResumeOptimization;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobMatchRepository;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.ResumeOptimizationRepository;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeOptimizationServiceTest {

    @Mock
    private ResumeOptimizationRepository optimizationRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobMatchRepository jobMatchRepository;

    private ResumeOptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        JobMatchingService matchingService = new JobMatchingService(
                jobMatchRepository,
                jobRepository,
                resumeRepository,
                userRepository,
                JsonMapper.builder().build(),
                ""
        );

        optimizationService = new ResumeOptimizationService(
                optimizationRepository,
                resumeRepository,
                jobRepository,
                userRepository,
                matchingService
        );
    }

    @Test
    void generateOptimizationPreviewShouldNeverFabricateMissingSkillsIntoPreview() {
        User user = new User();
        user.setId(1L);
        user.setName("Chandu");
        user.setEmail("chandu@example.com");
        user.setSkills("Java, Spring Boot, MySQL");
        user.setExperienceYears(2);

        Resume resume = new Resume();
        resume.setId(5L);
        resume.setUser(user);
        resume.setOriginalFileName("Chandu_Resume.docx");
        resume.setExtractedText("Original Resume Text");
        resume.setExtractedSkills("Java, Spring Boot, MySQL");
        resume.setExtractedEducation("B.Tech Computer Science");
        resume.setExtractedExperience("Backend Developer at TechNova");

        Job job = new Job();
        job.setId(20L);
        job.setTitle("Cloud Backend Engineer");
        job.setCompany("CloudScale");
        job.setDescription("Requires Java, Spring Boot, MySQL, and Kubernetes.");
        job.setRequiredSkills("Java, Spring Boot, MySQL, Kubernetes");

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(optimizationRepository.save(any(ResumeOptimization.class))).thenAnswer(inv -> {
            ResumeOptimization opt = inv.getArgument(0);
            opt.setId(77L);
            return opt;
        });

        ResumeOptimizationResponse response = optimizationService.generateOptimizationPreview(
                5L, 20L, "chandu@example.com"
        );

        assertEquals(77L, response.getId());
        assertEquals(List.of("Java", "Spring Boot", "MySQL"), response.getSupportedKeywords());
        assertEquals(List.of("Kubernetes"), response.getMissingSkills());
        assertFalse(response.isApprovedByCandidate());
        assertEquals("PREVIEW_PENDING_APPROVAL", response.getStatus());

        // Crucial check: Kubernetes must NOT be fabricated into the revised resume preview!
        assertFalse(response.getRevisedResumePreview().contains("Kubernetes"));
        // Original resume must remain unchanged until candidate approves
        assertEquals("Original Resume Text", resume.getExtractedText());
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void approveOptimizationShouldMarkApprovedAndOptionallyUpdateResume() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");

        Resume resume = new Resume();
        resume.setId(5L);
        resume.setOriginalFileName("Chandu_Resume.docx");
        resume.setExtractedText("Old text");

        Job job = new Job();
        job.setId(20L);
        job.setTitle("Backend Engineer");
        job.setCompany("TechNova");

        ResumeOptimization opt = new ResumeOptimization();
        opt.setId(77L);
        opt.setUser(user);
        opt.setResume(resume);
        opt.setJob(job);
        opt.setSupportedKeywords("Java, Spring Boot");
        opt.setMissingSkills("Docker");
        opt.setSuggestions("Suggestion 1");
        opt.setRevisedResumePreview("Tailored Preview Text");

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(optimizationRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(opt));
        when(optimizationRepository.save(any(ResumeOptimization.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeOptimizationApprovalRequest approvalRequest = new ResumeOptimizationApprovalRequest();
        approvalRequest.setRevisedResumePreview("Candidate Approved Resume Text");
        approvalRequest.setApplyToOriginalResume(true);

        ResumeOptimizationResponse approved = optimizationService.approveOptimization(
                77L, approvalRequest, "chandu@example.com"
        );

        assertTrue(approved.isApprovedByCandidate());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("Candidate Approved Resume Text", approved.getRevisedResumePreview());
        assertEquals("Candidate Approved Resume Text", resume.getExtractedText());
        verify(resumeRepository).save(resume);
    }
}
