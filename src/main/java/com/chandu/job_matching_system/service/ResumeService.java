package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.ResumeResponse;
import com.chandu.job_matching_system.dto.ResumeReviewRequest;
import com.chandu.job_matching_system.entity.JobApplication;
import com.chandu.job_matching_system.entity.JobMatch;
import com.chandu.job_matching_system.entity.Resume;
import com.chandu.job_matching_system.entity.ResumeOptimization;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobApplicationRepository;
import com.chandu.job_matching_system.repository.JobMatchRepository;
import com.chandu.job_matching_system.repository.ResumeOptimizationRepository;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeParserService resumeParserService;
    private final Path uploadDirectory;
    private JobMatchRepository jobMatchRepository;
    private ResumeOptimizationRepository resumeOptimizationRepository;
    private JobApplicationRepository jobApplicationRepository;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeParserService resumeParserService,
            @Value("${app.resume.upload-dir:uploads/resumes}") String uploadDir) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeParserService = resumeParserService;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Autowired(required = false)
    public void setDependentRepositories(
            JobMatchRepository jobMatchRepository,
            ResumeOptimizationRepository resumeOptimizationRepository,
            JobApplicationRepository jobApplicationRepository) {
        this.jobMatchRepository = jobMatchRepository;
        this.resumeOptimizationRepository = resumeOptimizationRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public ResumeResponse uploadResume(MultipartFile file, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);

        ResumeParserService.ParsedResumeData parsed = resumeParserService.validateAndParse(file);

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = originalName.toLowerCase(Locale.ROOT).endsWith(".pdf") ? ".pdf" : ".docx";
        String storedFileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(uploadDirectory);
            Path targetPath = uploadDirectory.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(uploadDirectory)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid file destination"
                );
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store uploaded resume safely"
            );
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setOriginalFileName(originalName);
        resume.setStoredFileName(storedFileName);
        resume.setFileType(parsed.fileType());
        resume.setFileSize(file.getSize());
        resume.setExtractedText(parsed.extractedText());
        resume.setExtractedSkills(parsed.extractedSkills());
        resume.setExtractedEducation(parsed.extractedEducation());
        resume.setExtractedExperience(parsed.extractedExperience());
        resume.setExtractedProjects(parsed.extractedProjects());
        resume.setExtractionStatus(parsed.extractionStatus());
        resume.setReviewedByCandidate(false);
        resume.setUploadedAt(Instant.now());
        resume.setUpdatedAt(Instant.now());

        Resume saved = resumeRepository.save(resume);

        String message = "NEEDS_REVIEW".equals(saved.getExtractionStatus())
                ? "Resume uploaded. Please review and complete the extracted details."
                : "Resume uploaded and parsed successfully. Please review the extracted information.";

        return toResponse(saved, message);
    }

    @Transactional
    public List<ResumeResponse> getMyResumes(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(this::repairLegacyParsedResumeIfNeeded)
                .map(r -> toResponse(r, "Resume retrieved"))
                .toList();
    }

    @Transactional
    public ResumeResponse getMyResumeById(Long resumeId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));
        resume = repairLegacyParsedResumeIfNeeded(resume);
        return toResponse(resume, "Resume retrieved successfully");
    }

    private Resume repairLegacyParsedResumeIfNeeded(Resume resume) {
        String edu = resume.getExtractedEducation() != null ? resume.getExtractedEducation() : "";
        String text = resume.getExtractedText() != null ? resume.getExtractedText() : "";
        boolean hasLegacyArtifacts = edu.contains("\\050")
                || edu.contains("PR OJECTS")
                || edu.contains("CER TIFICA")
                || text.contains("\\050")
                || ((resume.getExtractedProjects() == null || resume.getExtractedProjects().isBlank())
                    && (edu.toLowerCase(Locale.ROOT).contains("projects") || text.toLowerCase(Locale.ROOT).contains("projects")));

        if (!hasLegacyArtifacts) {
            return resume;
        }

        String sourceText = !text.isBlank() ? text : ("EDUCATION\n" + edu);
        ResumeParserService.ParsedResumeData reparsed =
                resumeParserService.parseStructuredText(resume.getFileType(), sourceText);

        resume.setExtractedText(reparsed.extractedText());
        if (reparsed.extractedSkills() != null && !reparsed.extractedSkills().isBlank()) {
            resume.setExtractedSkills(reparsed.extractedSkills());
        }
        if (reparsed.extractedEducation() != null && !reparsed.extractedEducation().isBlank()) {
            resume.setExtractedEducation(reparsed.extractedEducation());
        }
        if (reparsed.extractedExperience() != null && !reparsed.extractedExperience().isBlank()) {
            resume.setExtractedExperience(reparsed.extractedExperience());
        }
        if (reparsed.extractedProjects() != null && !reparsed.extractedProjects().isBlank()) {
            resume.setExtractedProjects(reparsed.extractedProjects());
        }
        resume.setUpdatedAt(Instant.now());
        return resumeRepository.save(resume);
    }

    @Transactional
    public ResumeResponse reviewAndCorrectResume(
            Long resumeId,
            ResumeReviewRequest request,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        resume.setExtractedSkills(trimToNull(request.getExtractedSkills()));
        resume.setExtractedEducation(trimToNull(request.getExtractedEducation()));
        resume.setExtractedExperience(trimToNull(request.getExtractedExperience()));
        resume.setExtractedProjects(trimToNull(request.getExtractedProjects()));
        if (request.getExtractedText() != null && !request.getExtractedText().isBlank()) {
            resume.setExtractedText(request.getExtractedText().trim());
        }
        resume.setReviewedByCandidate(true);
        resume.setExtractionStatus("REVIEWED");
        resume.setUpdatedAt(Instant.now());

        Resume saved = resumeRepository.save(resume);
        return toResponse(saved, "Resume details reviewed and updated successfully");
    }

    @Transactional
    public void deleteMyResume(Long resumeId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        if (jobMatchRepository != null) {
            List<JobMatch> matches = jobMatchRepository.findByUserIdOrderByCalculatedAtDesc(user.getId());
            for (JobMatch m : matches) {
                if (m.getResume() != null && resumeId.equals(m.getResume().getId())) {
                    m.setResume(null);
                    jobMatchRepository.save(m);
                }
            }
        }

        if (resumeOptimizationRepository != null) {
            List<ResumeOptimization> opts = resumeOptimizationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            for (ResumeOptimization opt : opts) {
                if (opt.getResume() != null && resumeId.equals(opt.getResume().getId())) {
                    resumeOptimizationRepository.delete(opt);
                }
            }
        }

        if (jobApplicationRepository != null) {
            List<JobApplication> apps = jobApplicationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
            for (JobApplication app : apps) {
                if (app.getResume() != null && resumeId.equals(app.getResume().getId())) {
                    app.setResume(null);
                    jobApplicationRepository.save(app);
                }
            }
        }

        try {
            Path filePath = uploadDirectory.resolve(resume.getStoredFileName()).normalize();
            if (filePath.startsWith(uploadDirectory)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
            // Proceed with database record removal even if file was already cleaned up
        }

        resumeRepository.delete(resume);
        resumeRepository.flush();
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "resume";
        }
        return Paths.get(filename).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResumeResponse toResponse(Resume resume, String message) {
        return new ResumeResponse(
                resume.getId(),
                resume.getOriginalFileName(),
                resume.getFileType(),
                resume.getFileSize(),
                resume.getExtractedText(),
                resume.getExtractedSkills(),
                resume.getExtractedEducation(),
                resume.getExtractedExperience(),
                resume.getExtractedProjects(),
                resume.getExtractionStatus(),
                resume.isReviewedByCandidate(),
                resume.getUploadedAt(),
                resume.getUpdatedAt(),
                message
        );
    }
}
