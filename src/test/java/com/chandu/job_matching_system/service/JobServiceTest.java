package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.JobRequest;
import com.chandu.job_matching_system.dto.JobResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.SavedJob;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.SavedJobRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private UserRepository userRepository;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, savedJobRepository, userRepository);
    }

    @Test
    void createJobShouldRejectDuplicateJobListing() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setCompany("Acme Corp");
        request.setLocation("Hyderabad");
        request.setDescription("Build Java services");
        request.setRequiredSkills("Java, Spring Boot");

        when(jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                "Java Developer", "Acme Corp", "Hyderabad"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> jobService.createJob(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldSaveLiveJobWithSeedDataFalse() {
        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setCompany("TechNova");
        request.setLocation("Bengaluru");
        request.setEmploymentType("full_time");
        request.setDescription("Spring Boot API development");
        request.setRequiredSkills("Java, Spring Boot, MySQL");

        when(jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                "Backend Developer", "TechNova", "Bengaluru"))
                .thenReturn(false);

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(inv -> {
                    Job job = inv.getArgument(0);
                    job.setId(100L);
                    return job;
                });

        JobResponse response = jobService.createJob(request);

        assertEquals(100L, response.getId());
        assertEquals("FULL_TIME", response.getEmploymentType());
        assertFalse(response.isSeedData());
        assertFalse(response.isExpired());
    }

    @Test
    void bookmarkJobShouldSaveBookmarkForAuthenticatedUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");

        Job job = new Job();
        job.setId(50L);
        job.setTitle("Java Engineer");
        job.setCompany("TechNova");
        job.setLocation("Hyderabad");
        job.setDescription("Java role");
        job.setRequiredSkills("Java");
        job.setPostedDate(LocalDate.now());
        job.setActive(true);

        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(50L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByUserIdAndJobId(1L, 50L)).thenReturn(false);

        JobResponse response = jobService.bookmarkJob(50L, "chandu@example.com");

        assertTrue(response.isBookmarked());
        verify(savedJobRepository).save(any(SavedJob.class));
    }
}
