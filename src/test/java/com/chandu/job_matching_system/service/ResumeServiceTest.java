package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.ResumeResponse;
import com.chandu.job_matching_system.dto.ResumeReviewRequest;
import com.chandu.job_matching_system.entity.Resume;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    private ResumeParserService resumeParserService;
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeParserService = new ResumeParserService();
        resumeService = new ResumeService(
                resumeRepository,
                userRepository,
                resumeParserService,
                tempDir.toString()
        );
    }

    private byte[] createValidDocxBytes(String text) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>";
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    void uploadResumeShouldRejectEmptyFile() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[0]
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> resumeService.uploadResume(emptyFile, "chandu@example.com")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void uploadResumeShouldRejectUnsupportedFileExtension() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "binary".getBytes()
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> resumeService.uploadResume(exeFile, "chandu@example.com")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void uploadResumeShouldRejectMalformedPdf() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));

        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "NOT_A_PDF_HEADER".getBytes()
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> resumeService.uploadResume(fakePdf, "chandu@example.com")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void uploadResumeShouldParseValidDocxAndExtractSkills() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));

        byte[] docxBytes = createValidDocxBytes(
                "Chandu Software Engineer\nSkills: Java, Spring Boot, MySQL, React\nEducation: B.Tech CSE\nWork Experience: Backend Developer Intern"
        );

        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "Chandu_Resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        ResumeResponse response = resumeService.uploadResume(docxFile, "chandu@example.com");

        assertEquals(10L, response.getId());
        assertEquals("DOCX", response.getFileType());
        assertEquals("PARSED", response.getExtractionStatus());
        assertNotNull(response.getExtractedSkills());
        assertTrue(response.getExtractedSkills().contains("Java"));
        assertTrue(response.getExtractedSkills().contains("Spring Boot"));
        assertFalse(response.isReviewedByCandidate());
    }

    @Test
    void parseResumeShouldHandleLatexPdfKerningArraysForSkillsAndProjects() {
        String latexPdfContent = "%PDF-1.5\n"
                + "1 0 obj\n<< /Length 400 >>\nstream\n"
                + "BT\n"
                + "[(SKILLS)]TJ\n0 -14 Td\n"
                + "[(Ja)32(v)64(a,)-334(Python,)-334(Spring)-333(Bo)-28(ot,)-334(F)65(astAPI,)-334(MySQL)]TJ\n0 -20 Td\n"
                + "[(PR)32(OJECTS)]TJ\n0 -14 Td\n"
                + "[(AI-P)28(o)28(w)28(ered)-383(Job)-384(Matc)28(hing)-383(System)]TJ\n"
                + "ET\nendstream\nendobj\n%%EOF";

        MockMultipartFile latexPdfFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                latexPdfContent.getBytes(StandardCharsets.ISO_8859_1)
        );

        ResumeParserService.ParsedResumeData parsed = resumeParserService.validateAndParse(latexPdfFile);

        assertEquals("PARSED", parsed.extractionStatus());
        assertTrue(parsed.extractedSkills().contains("Java"));
        assertTrue(parsed.extractedSkills().contains("Python"));
        assertTrue(parsed.extractedSkills().contains("Spring Boot"));
        assertTrue(parsed.extractedSkills().contains("FastAPI"));
        assertTrue(parsed.extractedSkills().contains("MySQL"));
        assertTrue(parsed.extractedProjects().contains("AI-Powered Job Matching System"));
    }

    @Test
    void reviewAndCorrectResumeShouldEnforceOwnershipAndUpdateFields() {
        User user = new User();
        user.setId(1L);
        user.setEmail("chandu@example.com");
        when(userRepository.findByEmail("chandu@example.com")).thenReturn(Optional.of(user));

        Resume existing = new Resume();
        existing.setId(10L);
        existing.setUser(user);
        existing.setOriginalFileName("Chandu_Resume.pdf");
        existing.setFileType("PDF");
        existing.setFileSize(2048L);

        when(resumeRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeReviewRequest review = new ResumeReviewRequest();
        review.setExtractedSkills("Java, Spring Boot, MySQL, Docker");
        review.setExtractedEducation("B.Tech Computer Science (2025)");
        review.setExtractedExperience("Java Intern at TechNova");
        review.setExtractedProjects("AI Job Matching System");

        ResumeResponse updated = resumeService.reviewAndCorrectResume(10L, review, "chandu@example.com");

        assertTrue(updated.isReviewedByCandidate());
        assertEquals("REVIEWED", updated.getExtractionStatus());
        assertEquals("Java, Spring Boot, MySQL, Docker", updated.getExtractedSkills());
    }
}
