package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.*;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.JobApplication;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private UserRepository userRepository;

    private JobApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new JobApplicationService(
                applicationRepository,
                jobRepository,
                resumeRepository,
                savedJobRepository,
                jobMatchRepository,
                userRepository
        );
    }

    @Test
    void createApplicationShouldRejectDirectAppliedStatusWithoutConfirmation() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setCompany("TechNova");
        job.setActive(true);

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(false);

        ApplicationCreateRequest req = new ApplicationCreateRequest();
        req.setJobId(10L);
        req.setStatus("APPLIED");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.createApplication(req, "chandu@example.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createAndConfirmSubmissionShouldTrackHistoryAndTimestamp() {
        User user = new User();
        user.setId(1L);
        user.setName("Chandu");
        user.setEmail("chandu@example.com");
        user.setSkills("Java, Spring Boot");

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setCompany("TechNova");
        job.setLocation("Hyderabad");
        job.setSourceUrl("https://careers.example.com/10");
        job.setActive(true);

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(false);
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(Collections.emptyList());

        JobApplication savedEntity = new JobApplication();
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication a = inv.getArgument(0);
            a.setId(55L);
            return a;
        });

        ApplicationCreateRequest createReq = new ApplicationCreateRequest();
        createReq.setJobId(10L);
        createReq.setStatus("READY_TO_APPLY");

        JobApplicationResponse created = applicationService.createApplication(createReq, "chandu@example.com");
        assertEquals("READY_TO_APPLY", created.getStatus());
        assertNull(created.getSubmittedAt());
        assertFalse(created.getStatusHistory().isEmpty());

        // Now confirm submission
        savedEntity.setId(55L);
        savedEntity.setUser(user);
        savedEntity.setJob(job);
        savedEntity.setStatus("READY_TO_APPLY");
        savedEntity.setStatusHistoryLog(String.join("\n", created.getStatusHistory()));

        when(applicationRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(savedEntity));

        ApplicationSubmissionConfirmRequest confirmReq = new ApplicationSubmissionConfirmRequest();
        confirmReq.setConfirmationDetails("Submitted on TechNova portal - Ref #TN-9821");

        JobApplicationResponse confirmed = applicationService.confirmSubmission(
                55L, confirmReq, "chandu@example.com"
        );

        assertEquals("APPLIED", confirmed.getStatus());
        assertEquals(LocalDate.now(), confirmed.getApplicationDate());
        assertNotNull(confirmed.getSubmittedAt());
        assertEquals("Submitted on TechNova portal - Ref #TN-9821", confirmed.getSubmissionConfirmation());
        assertEquals(2, confirmed.getStatusHistory().size());
    }
}
