package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.JobMatchResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.JobMatch;
import com.chandu.job_matching_system.entity.Resume;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobMatchRepository;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class JobMatchingService {

    private static final Map<String, String> SKILL_ALIASES = Map.ofEntries(
            Map.entry("java", "Java"),
            Map.entry("core java", "Java"),
            Map.entry("spring", "Spring Boot"),
            Map.entry("springboot", "Spring Boot"),
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("spring security", "Spring Security"),
            Map.entry("spring data jpa", "Spring Data JPA"),
            Map.entry("jpa", "Spring Data JPA"),
            Map.entry("hibernate", "Hibernate"),
            Map.entry("mysql", "MySQL"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("mongodb", "MongoDB"),
            Map.entry("sql", "SQL"),
            Map.entry("rest", "REST APIs"),
            Map.entry("rest api", "REST APIs"),
            Map.entry("rest apis", "REST APIs"),
            Map.entry("microservices", "Microservices"),
            Map.entry("python", "Python"),
            Map.entry("fastapi", "FastAPI"),
            Map.entry("flask", "Flask"),
            Map.entry("django", "Django"),
            Map.entry("nlp", "NLP"),
            Map.entry("natural language processing", "NLP"),
            Map.entry("llm", "LLMs"),
            Map.entry("llms", "LLMs"),
            Map.entry("generative ai", "Generative AI"),
            Map.entry("machine learning", "Machine Learning"),
            Map.entry("ml", "Machine Learning"),
            Map.entry("deep learning", "Deep Learning"),
            Map.entry("pytorch", "PyTorch"),
            Map.entry("tensorflow", "TensorFlow"),
            Map.entry("nltk", "NLTK"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("js", "JavaScript"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("react", "React"),
            Map.entry("reactjs", "React"),
            Map.entry("html", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("tailwind css", "Tailwind CSS"),
            Map.entry("node.js", "Node.js"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("docker", "Docker"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("aws", "AWS"),
            Map.entry("cloud computing", "Cloud Computing"),
            Map.entry("git", "Git"),
            Map.entry("github", "GitHub"),
            Map.entry("maven", "Maven"),
            Map.entry("junit", "JUnit"),
            Map.entry("mockito", "Mockito"),
            Map.entry("pytest", "Pytest")
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has",
            "in", "is", "it", "of", "on", "or", "that", "the", "to", "was", "were",
            "will", "with", "we", "you", "your", "our", "their", "this", "these"
    );

    public record MatchCalculationResult(
            int matchScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            String explanation
    ) {}

    private final JobMatchRepository jobMatchRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;
    private final String nlpServiceUrl;
    private final HttpClient httpClient;

    public JobMatchingService(
            JobMatchRepository jobMatchRepository,
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            JsonMapper jsonMapper,
            @Value("${app.nlp.service-url:http://localhost:8000}") String nlpServiceUrl) {
        this.jobMatchRepository = jobMatchRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
        this.nlpServiceUrl = nlpServiceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Transactional
    public JobMatchResponse calculateAndSaveMatch(
            Long jobId,
            Long resumeId,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        Resume resume = null;
        if (resumeId != null) {
            resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Resume not found"
                    ));
        } else {
            List<Resume> userResumes = resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
            if (!userResumes.isEmpty()) {
                resume = userResumes.get(0);
            }
        }

        String combinedSkills = combineCsv(
                user.getSkills(),
                resume != null ? resume.getExtractedSkills() : null
        );
        String combinedEducation = combineText(
                user.getEducation(),
                resume != null ? resume.getExtractedEducation() : null
        );
        String resumeText = resume != null
                ? combineText(resume.getExtractedText(), resume.getExtractedExperience(), resume.getExtractedProjects())
                : "";

        MatchCalculationResult calc = callFastApiOrFallback(
                combinedSkills,
                user.getExperienceYears(),
                combinedEducation,
                resumeText,
                job
        );

        JobMatch matchEntity = jobMatchRepository
                .findFirstByUserIdAndJobIdOrderByCalculatedAtDesc(user.getId(), job.getId())
                .orElseGet(JobMatch::new);

        matchEntity.setUser(user);
        matchEntity.setJob(job);
        matchEntity.setResume(resume);
        matchEntity.setMatchScore(calc.matchScore());
        matchEntity.setMatchedSkills(String.join(", ", calc.matchedSkills()));
        matchEntity.setMissingSkills(String.join(", ", calc.missingSkills()));
        matchEntity.setExplanation(calc.explanation());
        matchEntity.setCalculatedAt(Instant.now());

        JobMatch saved = jobMatchRepository.save(matchEntity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMyMatches(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        return jobMatchRepository.findByUserIdOrderByCalculatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobMatchResponse getMatchForJob(Long jobId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        JobMatch match = jobMatchRepository
                .findFirstByUserIdAndJobIdOrderByCalculatedAtDesc(user.getId(), jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No match calculated yet for this job"
                ));
        return toResponse(match);
    }

    private MatchCalculationResult callFastApiOrFallback(
            String candidateSkills,
            Integer candidateExperienceYears,
            String candidateEducation,
            String resumeText,
            Job job) {

        if (nlpServiceUrl != null && !nlpServiceUrl.isBlank()) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("candidateSkills", candidateSkills != null ? candidateSkills : "");
                payload.put("candidateExperienceYears", candidateExperienceYears != null ? candidateExperienceYears : 0);
                payload.put("candidateEducation", candidateEducation != null ? candidateEducation : "");
                payload.put("resumeText", resumeText != null ? resumeText : "");
                payload.put("jobTitle", job.getTitle());
                payload.put("jobDescription", job.getDescription());
                payload.put("jobRequiredSkills", job.getRequiredSkills());
                payload.put("jobPreferredQualifications", job.getPreferredQualifications() != null ? job.getPreferredQualifications() : "");
                payload.put("jobMinExperienceYears", job.getMinExperienceYears() != null ? job.getMinExperienceYears() : 0);

                String requestBody = jsonMapper.writeValueAsString(payload);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(nlpServiceUrl.replaceAll("/+$", "") + "/api/nlp/match"))
                        .timeout(Duration.ofSeconds(3))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = jsonMapper.readTree(response.body());
                    int score = root.get("matchScore").asInt();
                    List<String> matched = new ArrayList<>();
                    root.get("matchedSkills").forEach(n -> matched.add(n.asText()));
                    List<String> missing = new ArrayList<>();
                    root.get("missingSkills").forEach(n -> missing.add(n.asText()));
                    String explanation = root.get("explanation").asText();
                    return new MatchCalculationResult(score, matched, missing, explanation);
                }
            } catch (Exception ignored) {
                // Gracefully fall back to deterministic local matching engine if FastAPI service is offline
            }
        }

        return calculateLocally(
                candidateSkills,
                candidateExperienceYears,
                candidateEducation,
                resumeText,
                job
        );
    }

    public MatchCalculationResult calculateLocally(
            String candidateSkills,
            Integer candidateExperienceYears,
            String candidateEducation,
            String resumeText,
            Job job) {

        String combinedCandidateText = combineText(resumeText, candidateEducation);
        List<String> candSkills = extractAndNormalizeSkills(candidateSkills, combinedCandidateText);
        List<String> reqSkills = extractAndNormalizeSkills(job.getRequiredSkills(), null);

        Map<String, String> candMap = new LinkedHashMap<>();
        for (String s : candSkills) {
            candMap.put(s.toLowerCase(Locale.ROOT), s);
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String req : reqSkills) {
            if (candMap.containsKey(req.toLowerCase(Locale.ROOT))) {
                matchedSkills.add(req);
            } else {
                missingSkills.add(req);
            }
        }

        double skillRatio = reqSkills.isEmpty() ? 0.0 : (double) matchedSkills.size() / reqSkills.size();

        int reqExp = job.getMinExperienceYears() != null ? Math.max(0, job.getMinExperienceYears()) : 0;
        int candExp = candidateExperienceYears != null ? Math.max(0, candidateExperienceYears) : 0;

        boolean hasCandidateData = !candSkills.isEmpty() || !combinedCandidateText.isBlank();
        double expRatio;
        if (reqExp == 0) {
            expRatio = hasCandidateData ? 1.0 : 0.0;
        } else if (candExp >= reqExp) {
            expRatio = 1.0;
        } else {
            expRatio = (double) candExp / reqExp;
        }

        String candidateCorpus = combineText(candidateSkills, candidateEducation, resumeText);
        String jobCorpus = combineText(
                job.getTitle(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getPreferredQualifications()
        );
        double similarity = computeCosineSimilarity(candidateCorpus, jobCorpus);
        double textRatio = Math.min(1.0, similarity * 1.8);

        int matchScore = 0;
        if (hasCandidateData) {
            double raw = (skillRatio * 65.0) + (expRatio * 20.0) + (textRatio * 15.0);
            matchScore = (int) Math.round(Math.max(0.0, Math.min(100.0, raw)));
        }

        String explanation = buildExplanation(
                matchedSkills,
                missingSkills,
                candExp,
                reqExp,
                hasCandidateData
        );

        return new MatchCalculationResult(matchScore, matchedSkills, missingSkills, explanation);
    }

    public List<String> extractAndNormalizeSkills(String skillsCsv, String extraText) {
        Map<String, String> normalized = new LinkedHashMap<>();

        if (skillsCsv != null && !skillsCsv.isBlank()) {
            String[] tokens = skillsCsv.split("[,;/|\\n]+");
            for (String token : tokens) {
                String cleaned = token.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                if (!cleaned.isEmpty()) {
                    String canonical = SKILL_ALIASES.getOrDefault(cleaned, token.trim());
                    normalized.put(canonical.toLowerCase(Locale.ROOT), canonical);
                }
            }
        }

        if (extraText != null && !extraText.isBlank()) {
            String lower = extraText.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : SKILL_ALIASES.entrySet()) {
                String pattern = "\\b" + Pattern.quote(entry.getKey()) + "\\b";
                if (Pattern.compile(pattern).matcher(lower).find()) {
                    String canonical = entry.getValue();
                    normalized.put(canonical.toLowerCase(Locale.ROOT), canonical);
                }
            }
        }

        return new ArrayList<>(normalized.values());
    }

    private double computeCosineSimilarity(String textA, String textB) {
        Map<String, Integer> freqA = tokenize(textA);
        Map<String, Integer> freqB = tokenize(textB);
        if (freqA.isEmpty() || freqB.isEmpty()) {
            return 0.0;
        }

        double dot = 0.0;
        for (Map.Entry<String, Integer> entry : freqA.entrySet()) {
            Integer countB = freqB.get(entry.getKey());
            if (countB != null) {
                dot += entry.getValue() * countB;
            }
        }

        double normA = Math.sqrt(freqA.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(freqB.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (normA * normB);
    }

    private Map<String, Integer> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counts = new HashMap<>();
        String[] words = text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+");
        for (String w : words) {
            if (w.length() > 1 && !STOP_WORDS.contains(w)) {
                counts.merge(w, 1, Integer::sum);
            }
        }
        return counts;
    }

    private String buildExplanation(
            List<String> matchedSkills,
            List<String> missingSkills,
            int candExp,
            int reqExp,
            boolean hasCandidateData) {

        if (!hasCandidateData) {
            return "No candidate skills or resume details were provided. Please update your profile or upload a resume to compute a meaningful skill alignment score.";
        }

        List<String> parts = new ArrayList<>();
        if (!matchedSkills.isEmpty() && missingSkills.isEmpty()) {
            parts.add("Strong skill alignment: the candidate matches all " + matchedSkills.size()
                    + " required skills (" + String.join(", ", matchedSkills) + ").");
        } else if (!matchedSkills.isEmpty()) {
            parts.add("The candidate matches " + matchedSkills.size() + " required skill(s) ("
                    + String.join(", ", matchedSkills) + "), while "
                    + String.join(", ", missingSkills) + " experience is not identified in the supplied profile/resume.");
        } else {
            parts.add("None of the required technical skills (" + String.join(", ", missingSkills)
                    + ") were identified in the supplied profile or resume.");
        }

        if (reqExp > 0) {
            if (candExp >= reqExp) {
                parts.add("Reported experience (" + candExp + " yr(s)) meets the minimum requirement (" + reqExp + " yr(s)).");
            } else {
                parts.add("Reported experience (" + candExp + " yr(s)) is below the preferred " + reqExp + " yr(s).");
            }
        }

        parts.add("Note: This score reflects profile-to-job requirement alignment and is not a guarantee or probability of being hired.");
        return String.join(" ", parts);
    }

    private String combineCsv(String first, String second) {
        if ((first == null || first.isBlank()) && (second == null || second.isBlank())) {
            return "";
        }
        if (first == null || first.isBlank()) {
            return second.trim();
        }
        if (second == null || second.isBlank()) {
            return first.trim();
        }
        return first.trim() + ", " + second.trim();
    }

    private String combineText(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(val.trim());
            }
        }
        return sb.toString();
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

    private List<String> splitCsvToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private JobMatchResponse toResponse(JobMatch match) {
        return new JobMatchResponse(
                match.getId(),
                match.getJob().getId(),
                match.getJob().getTitle(),
                match.getJob().getCompany(),
                match.getResume() != null ? match.getResume().getId() : null,
                match.getMatchScore(),
                splitCsvToList(match.getMatchedSkills()),
                splitCsvToList(match.getMissingSkills()),
                match.getExplanation(),
                match.getCalculatedAt()
        );
    }
}
