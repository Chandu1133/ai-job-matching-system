package com.chandu.job_matching_system.config;

import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.service.JobService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Configuration
public class JobDataSeeder {

    @Bean
    public CommandLineRunner seedInitialJobs(
            JobRepository jobRepository,
            JobService jobService,
            @Value("${app.jsearch.api-key:}") String jsearchApiKey) {
        return args -> {
            if (jobRepository.count() == 0) {
                Job backendJob = new Job();
                backendJob.setTitle("Java Spring Boot Backend Engineer");
                backendJob.setCompany("TechNova Solutions");
                backendJob.setLocation("Hyderabad, India");
                backendJob.setEmploymentType("FULL_TIME");
                backendJob.setMinExperienceYears(1);
                backendJob.setMaxExperienceYears(4);
                backendJob.setSalaryRange("8 - 14 LPA");
                backendJob.setDescription("Design and build scalable REST APIs using Java, Spring Boot, Spring Security, and MySQL. Collaborate with AI and frontend teams to deliver high-reliability microservices.");
                backendJob.setRequiredSkills("Java, Spring Boot, Spring Security, REST APIs, MySQL, Git");
                backendJob.setPreferredQualifications("Docker, AWS, Microservices, JUnit, Mockito");
                backendJob.setSourceUrl("https://careers.example.com/technova/java-backend");
                backendJob.setPostedDate(LocalDate.now().minusDays(2));
                backendJob.setExpiryDate(LocalDate.now().plusDays(30));
                backendJob.setActive(true);
                backendJob.setSeedData(true);

                Job fullStackJob = new Job();
                fullStackJob.setTitle("Full Stack Developer (React + Java)");
                fullStackJob.setCompany("CloudScale Systems");
                fullStackJob.setLocation("Bengaluru, India");
                fullStackJob.setEmploymentType("FULL_TIME");
                fullStackJob.setMinExperienceYears(0);
                fullStackJob.setMaxExperienceYears(3);
                fullStackJob.setSalaryRange("6 - 12 LPA");
                fullStackJob.setDescription("Develop responsive web applications using React, JavaScript, and Spring Boot REST backends. Implement secure authentication and interactive dashboards.");
                fullStackJob.setRequiredSkills("Java, Spring Boot, React, JavaScript, MySQL, HTML, CSS");
                fullStackJob.setPreferredQualifications("TypeScript, Tailwind CSS, Docker, CI/CD");
                fullStackJob.setSourceUrl("https://careers.example.com/cloudscale/fullstack-react-java");
                fullStackJob.setPostedDate(LocalDate.now().minusDays(1));
                fullStackJob.setExpiryDate(LocalDate.now().plusDays(25));
                fullStackJob.setActive(true);
                fullStackJob.setSeedData(true);

                Job aiEngineerJob = new Job();
                aiEngineerJob.setTitle("AI / NLP Python Engineer");
                aiEngineerJob.setCompany("DataMinds AI");
                aiEngineerJob.setLocation("Remote");
                aiEngineerJob.setEmploymentType("REMOTE");
                aiEngineerJob.setMinExperienceYears(1);
                aiEngineerJob.setMaxExperienceYears(5);
                aiEngineerJob.setSalaryRange("10 - 18 LPA");
                aiEngineerJob.setDescription("Build NLP pipelines for resume parsing, skill extraction, and semantic job matching using Python and FastAPI.");
                aiEngineerJob.setRequiredSkills("Python, FastAPI, NLP, Machine Learning, REST APIs, SQL");
                aiEngineerJob.setPreferredQualifications("Scikit-learn, Transformers, Docker, Pytest");
                aiEngineerJob.setSourceUrl("https://careers.example.com/dataminds/ai-nlp-engineer");
                aiEngineerJob.setPostedDate(LocalDate.now());
                aiEngineerJob.setExpiryDate(LocalDate.now().plusDays(45));
                aiEngineerJob.setActive(true);
                aiEngineerJob.setSeedData(true);

                jobRepository.saveAll(List.of(backendJob, fullStackJob, aiEngineerJob));
            }

            if (jsearchApiKey != null && !jsearchApiKey.isBlank()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        jobService.syncExternalJobs("Java Spring Boot Python Developer in India", null);
                    } catch (Exception ignored) {
                        // Background startup sync should never block application startup
                    }
                });
            }
        };
    }
}
