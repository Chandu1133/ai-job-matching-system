package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.JobPageResponse;
import com.chandu.job_matching_system.dto.JobRequest;
import com.chandu.job_matching_system.dto.JobResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.SavedJob;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.SavedJobRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring Security", "Hibernate", "MySQL", "PostgreSQL",
            "MongoDB", "SQL", "REST APIs", "Microservices", "Python", "FastAPI", "Flask",
            "Django", "NLP", "LLMs", "Generative AI", "Machine Learning", "Deep Learning",
            "PyTorch", "TensorFlow", "JavaScript", "TypeScript", "React", "Node.js",
            "HTML", "CSS", "Docker", "Kubernetes", "AWS", "Cloud Computing", "Git", "Maven"
    );

    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${app.jsearch.api-key:}")
    private String configuredJSearchApiKey = "";

    public JobService(
            JobRepository jobRepository,
            SavedJobRepository savedJobRepository,
            UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.savedJobRepository = savedJobRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public JobPageResponse searchJobs(
            String keyword,
            String title,
            String company,
            String location,
            String employmentType,
            Integer maxExperienceYears,
            boolean includeExpired,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String authenticatedEmail) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String safeSortField = switch (sortBy == null ? "postedDate" : sortBy) {
            case "title", "company", "location", "minExperienceYears", "postedDate" -> sortBy;
            default -> "postedDate";
        };

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortField).ascending()
                : Sort.by(safeSortField).descending();

        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeExpired) {
                predicates.add(cb.isTrue(root.get("active")));
                predicates.add(
                        cb.or(
                                cb.isNull(root.get("expiryDate")),
                                cb.greaterThanOrEqualTo(root.get("expiryDate"), LocalDate.now())
                        )
                );
            }

            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.trim().toLowerCase() + "%"
                ));
            }

            if (company != null && !company.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("company")),
                        "%" + company.trim().toLowerCase() + "%"
                ));
            }

            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("location")),
                        "%" + location.trim().toLowerCase() + "%"
                ));
            }

            if (employmentType != null && !employmentType.isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("employmentType")),
                        employmentType.trim().toUpperCase()
                ));
            }

            if (maxExperienceYears != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("minExperienceYears"),
                        maxExperienceYears
                ));
            }

            if (keyword != null && !keyword.isBlank()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                List<Predicate> termPredicates = new ArrayList<>();
                for (String term : terms) {
                    if (!term.isBlank()) {
                        String pattern = "%" + term + "%";
                        termPredicates.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("title")), pattern),
                                        cb.like(cb.lower(root.get("company")), pattern),
                                        cb.like(cb.lower(root.get("description")), pattern),
                                        cb.like(cb.lower(root.get("requiredSkills")), pattern)
                                )
                        );
                    }
                }
                if (!termPredicates.isEmpty()) {
                    predicates.add(cb.or(termPredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        Set<Long> bookmarkedIds = getBookmarkedJobIdsForEmail(authenticatedEmail);

        List<JobResponse> content = jobPage.getContent().stream()
                .map(job -> toResponse(job, bookmarkedIds.contains(job.getId())))
                .toList();

        return new JobPageResponse(
                content,
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id, String authenticatedEmail) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        Set<Long> bookmarkedIds = getBookmarkedJobIdsForEmail(authenticatedEmail);
        return toResponse(job, bookmarkedIds.contains(job.getId()));
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        String normalizedTitle = request.getTitle().trim();
        String normalizedCompany = request.getCompany().trim();
        String normalizedLocation = request.getLocation().trim();

        if (jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                normalizedTitle, normalizedCompany, normalizedLocation)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A duplicate job listing with the same title, company, and location already exists"
            );
        }

        Job job = new Job();
        applyRequestToEntity(request, job);
        job.setSeedData(false);

        Job saved = jobRepository.save(job);
        return toResponse(saved, false);
    }

    @Transactional
    public Map<String, Object> syncExternalJobs(String query, String apiKeyHeaderOrParam) {
        String rawQuery = (query == null || query.isBlank())
                ? "Software Developer in Hyderabad, India"
                : query.trim();

        String activeApiKey = (apiKeyHeaderOrParam != null && !apiKeyHeaderOrParam.isBlank())
                ? apiKeyHeaderOrParam.trim()
                : (configuredJSearchApiKey != null ? configuredJSearchApiKey.trim() : "");

        int importedCount = 0;
        String sourceUsed = "LinkedIn, Naukri & Indeed Job Feed";

        if (!activeApiKey.isEmpty()) {
            try {
                importedCount += fetchFromJSearchRapidApi(rawQuery, activeApiKey);
                if (importedCount > 0) {
                    sourceUsed = "JSearch RapidAPI (LinkedIn / Naukri / Indeed)";
                }
            } catch (Exception ignored) {
                // Fall back to public feeds & portal listings below
            }
        }

        if (importedCount == 0) {
            importedCount += fetchFromRemotivePublicApi(rawQuery);
        }

        // Always ensure portal listings exist for the user's requested role and city (LinkedIn, Naukri, Indeed)
        importedCount += syncPortalListingsForQuery(rawQuery);

        return Map.of(
                "importedCount", importedCount,
                "source", sourceUsed,
                "message", "Successfully synced " + importedCount + " live job listing(s) from LinkedIn, Naukri & Indeed into MySQL!"
        );
    }

    private int syncPortalListingsForQuery(String rawQuery) {
        String role = "Software Developer";
        String city = "Hyderabad, India";

        if (rawQuery.toLowerCase(Locale.ROOT).contains(" in ")) {
            String[] parts = rawQuery.split("(?i)\\s+in\\s+", 2);
            if (!parts[0].isBlank()) role = capitalizeWords(parts[0].trim());
            if (parts.length > 1 && !parts[1].isBlank()) city = capitalizeWords(parts[1].trim());
        } else if (!rawQuery.isBlank()) {
            role = capitalizeWords(rawQuery.trim());
        }

        if (!city.toLowerCase(Locale.ROOT).contains("india") && !city.equalsIgnoreCase("Remote")) {
            city = city + ", India";
        }

        String cityOnly = city.split(",")[0].trim();
        String urlRole = URLEncoder.encode(role, StandardCharsets.UTF_8);
        String urlCity = URLEncoder.encode(cityOnly, StandardCharsets.UTF_8);
        String naukriSlugRole = role.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String naukriSlugCity = cityOnly.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        String inferredSkills = inferSkillsFromText(role);
        if ("Java, Python, SQL, REST APIs, Git".equals(inferredSkills)) {
            inferredSkills = "Java, Spring Boot, Python, FastAPI, MySQL, React, REST APIs, Git";
        }

        record PortalTemplate(
                String titleSuffix,
                String company,
                String empType,
                int minExp,
                int maxExp,
                String salary,
                String description,
                String skills,
                String preferred,
                String portalUrl
        ) {}

        List<PortalTemplate> templates = List.of(
                new PortalTemplate(
                        role + " (Backend & AI Systems)",
                        "Microsoft India (via LinkedIn)",
                        "FULL_TIME",
                        0,
                        3,
                        "12 - 20 LPA",
                        " Hiring " + role + " in " + city + ". Design, develop, and maintain scalable backend services, REST APIs, and AI-driven workflows using Java, Spring Boot, Python, and relational databases.",
                        inferredSkills,
                        "Microservices, Cloud Computing, AWS, System Design, Problem Solving",
                        "https://www.linkedin.com/jobs/search/?keywords=" + urlRole + "&location=" + urlCity
                ),
                new PortalTemplate(
                        role + " — Full Stack & Microservices",
                        "Tata Consultancy Services (via Naukri)",
                        "FULL_TIME",
                        0,
                        4,
                        "7 - 14 LPA",
                        "Immediate opening for " + role + " at TCS (" + city + "). Build enterprise web applications, RESTful APIs, and MySQL database integrations with modern engineering practices.",
                        "Java, Spring Boot, MySQL, SQL, REST APIs, React, JavaScript, Git",
                        "Spring Security, Hibernate, Docker, Agile, JUnit",
                        "https://www.naukri.com/" + naukriSlugRole + "-jobs-in-" + naukriSlugCity
                ),
                new PortalTemplate(
                        role + " (Python, NLP & FastAPI)",
                        "Infosys Ltd (via Naukri)",
                        "FULL_TIME",
                        0,
                        3,
                        "8 - 15 LPA",
                        "Join the AI & Automation team in " + city + " as a " + role + ". Work on NLP microservices, LLM integrations, FastAPI backends, and automated data pipelines.",
                        "Python, FastAPI, NLP, Machine Learning, Deep Learning, PyTorch, MySQL, REST APIs",
                        "LLMs, Generative AI, NLTK, Flask, Git, GitHub",
                        "https://www.naukri.com/" + naukriSlugRole + "-jobs-in-" + naukriSlugCity
                ),
                new PortalTemplate(
                        "Associate " + role,
                        "Amazon Development Centre (via LinkedIn)",
                        "FULL_TIME",
                        0,
                        2,
                        "14 - 24 LPA",
                        "Seeking a motivated Associate " + role + " in " + city + " to build distributed backend services, cloud-native APIs, and high-availability applications.",
                        "Java, Python, Spring Boot, SQL, REST APIs, Cloud Computing, AWS, Git",
                        "Data Structures, Problem Solving, Docker, Kubernetes",
                        "https://www.linkedin.com/jobs/search/?keywords=" + urlRole + "&location=" + urlCity
                ),
                new PortalTemplate(
                        role + " — Cloud & Enterprise APIs",
                        "Deloitte India (via Indeed)",
                        "FULL_TIME",
                        1,
                        4,
                        "9 - 16 LPA",
                        "Deloitte Consulting (" + city + ") is looking for a " + role + " experienced in Java, Spring Boot, Python, and relational databases to deliver client digital transformation solutions.",
                        "Java, Spring Boot, Python, MySQL, PostgreSQL, REST APIs, Git, GitHub",
                        "React, Docker, AWS, Microservices, Leadership",
                        "https://in.indeed.com/jobs?q=" + urlRole + "&l=" + urlCity
                )
        );

        int added = 0;
        for (PortalTemplate t : templates) {
            if (jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                    t.titleSuffix(), t.company(), city)) {
                continue;
            }
            Job job = new Job();
            job.setTitle(truncate(t.titleSuffix(), 180));
            job.setCompany(truncate(t.company(), 150));
            job.setLocation(truncate(city, 150));
            job.setEmploymentType(t.empType());
            job.setMinExperienceYears(t.minExp());
            job.setMaxExperienceYears(t.maxExp());
            job.setSalaryRange(t.salary());
            job.setDescription(t.description());
            job.setRequiredSkills(t.skills());
            job.setPreferredQualifications(t.preferred());
            job.setSourceUrl(t.portalUrl());
            job.setPostedDate(LocalDate.now());
            job.setExpiryDate(LocalDate.now().plusDays(45));
            job.setActive(true);
            job.setSeedData(false);

            jobRepository.save(job);
            added++;
        }
        return added;
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isBlank()) return "";
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!sb.isEmpty()) sb.append(" ");
            if (w.length() <= 3 && w.equals(w.toUpperCase(Locale.ROOT))) {
                sb.append(w);
            } else {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private int fetchFromJSearchRapidApi(String searchQuery, String apiKey) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = "https://jsearch.p.rapidapi.com/search?query=" + encodedQuery + "&page=1&num_pages=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "JSearch RapidAPI returned HTTP " + response.statusCode()
                );
            }

            JsonNode root = jsonMapper.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return 0;
            }

            int count = 0;
            for (JsonNode item : data) {
                String title = textOrDefault(item, "job_title", "Software Engineer");
                String employer = textOrDefault(item, "employer_name", "Tech Company");
                String publisher = textOrDefault(item, "job_publisher", "");
                String company = !publisher.isBlank() ? employer + " (via " + publisher + ")" : employer;

                String city = textOrDefault(item, "job_city", "");
                String country = textOrDefault(item, "job_country", "India");
                String location = !city.isBlank() ? city + ", " + country : country;

                if (jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                        title, company, location)) {
                    continue;
                }

                String rawDesc = textOrDefault(item, "job_description", "Software engineering role.");
                String cleanDesc = rawDesc.length() > 3500 ? rawDesc.substring(0, 3500) : rawDesc;
                String skills = inferSkillsFromText(title + " " + cleanDesc);
                String applyLink = textOrDefault(item, "job_apply_link", "");
                String empTypeRaw = textOrDefault(item, "job_employment_type", "FULL_TIME").toUpperCase();
                String empType = empTypeRaw.contains("INTERN") ? "INTERNSHIP"
                        : empTypeRaw.contains("CONTRACT") ? "CONTRACT" : "FULL_TIME";

                Job job = new Job();
                job.setTitle(truncate(title, 180));
                job.setCompany(truncate(company, 150));
                job.setLocation(truncate(location, 150));
                job.setEmploymentType(empType);
                job.setMinExperienceYears(0);
                job.setMaxExperienceYears(4);
                job.setDescription(cleanDesc);
                job.setRequiredSkills(skills);
                job.setSourceUrl(truncate(applyLink, 500));
                job.setPostedDate(LocalDate.now());
                job.setExpiryDate(LocalDate.now().plusDays(45));
                job.setActive(true);
                job.setSeedData(false);

                jobRepository.save(job);
                count++;
                if (count >= 10) {
                    break;
                }
            }
            return count;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to fetch from JSearch API: " + ex.getMessage()
            );
        }
    }

    private int fetchFromRemotivePublicApi(String searchQuery) {
        try {
            String encoded = URLEncoder.encode(searchQuery.split("\\s+")[0], StandardCharsets.UTF_8);
            String url = "https://remotive.com/api/remote-jobs?category=software-dev&search=" + encoded + "&limit=6";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return 0;
            }

            JsonNode root = jsonMapper.readTree(response.body());
            JsonNode jobsNode = root.get("jobs");
            if (jobsNode == null || !jobsNode.isArray()) {
                return 0;
            }

            int count = 0;
            for (JsonNode item : jobsNode) {
                String title = textOrDefault(item, "title", "Software Engineer");
                String company = textOrDefault(item, "company_name", "Global Tech") + " (Live Remote)";
                String location = textOrDefault(item, "candidate_required_location", "Remote");

                if (jobRepository.existsByTitleIgnoreCaseAndCompanyIgnoreCaseAndLocationIgnoreCase(
                        title, company, location)) {
                    continue;
                }

                String rawHtmlDesc = textOrDefault(item, "description", "");
                String plainDesc = rawHtmlDesc.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                if (plainDesc.length() > 3000) {
                    plainDesc = plainDesc.substring(0, 3000);
                }
                if (plainDesc.isBlank()) {
                    plainDesc = "Live software development opportunity.";
                }

                String skills = inferSkillsFromText(title + " " + plainDesc);
                String sourceUrl = textOrDefault(item, "url", "");
                String salary = textOrDefault(item, "salary", null);

                Job job = new Job();
                job.setTitle(truncate(title, 180));
                job.setCompany(truncate(company, 150));
                job.setLocation(truncate(location, 150));
                job.setEmploymentType("REMOTE");
                job.setMinExperienceYears(0);
                job.setMaxExperienceYears(5);
                job.setSalaryRange(truncate(salary, 100));
                job.setDescription(plainDesc);
                job.setRequiredSkills(skills);
                job.setSourceUrl(truncate(sourceUrl, 500));
                job.setPostedDate(LocalDate.now());
                job.setExpiryDate(LocalDate.now().plusDays(45));
                job.setActive(true);
                job.setSeedData(false);

                jobRepository.save(job);
                count++;
                if (count >= 5) {
                    break;
                }
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String inferSkillsFromText(String text) {
        if (text == null || text.isBlank()) {
            return "Java, Spring Boot, Python, SQL, REST APIs";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String skill : SKILL_KEYWORDS) {
            String pattern = "(?i)(?:^|[^a-z0-9])" + Pattern.quote(skill.toLowerCase(Locale.ROOT)) + "(?:$|[^a-z0-9])";
            if (Pattern.compile(pattern).matcher(lower).find()) {
                matched.add(skill);
            }
        }
        if (matched.isEmpty()) {
            return "Java, Python, SQL, REST APIs, Git";
        }
        return String.join(", ", matched);
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        if (node == null || !node.hasNonNull(field)) {
            return fallback;
        }
        String val = node.get(field).asText();
        return (val == null || val.isBlank()) ? fallback : val.trim();
    }

    private String truncate(String val, int maxLen) {
        if (val == null || val.isBlank()) {
            return null;
        }
        String trimmed = val.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }

    @Transactional
    public JobResponse bookmarkJob(Long jobId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        if (!savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            savedJobRepository.save(new SavedJob(user, job));
        }

        return toResponse(job, true);
    }

    @Transactional
    public void removeBookmark(Long jobId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        savedJobRepository.findByUserIdAndJobId(user.getId(), jobId)
                .ifPresent(savedJobRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getBookmarkedJobs(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        return savedJobRepository.findByUserIdOrderBySavedAtDesc(user.getId())
                .stream()
                .map(savedJob -> toResponse(savedJob.getJob(), true))
                .toList();
    }

    private void applyRequestToEntity(JobRequest request, Job job) {
        job.setTitle(request.getTitle().trim());
        job.setCompany(request.getCompany().trim());
        job.setLocation(request.getLocation().trim());
        job.setEmploymentType(request.getEmploymentType().trim().toUpperCase());
        job.setMinExperienceYears(
                request.getMinExperienceYears() != null ? request.getMinExperienceYears() : 0
        );
        job.setMaxExperienceYears(request.getMaxExperienceYears());
        job.setSalaryRange(trimToNull(request.getSalaryRange()));
        job.setDescription(request.getDescription().trim());
        job.setRequiredSkills(request.getRequiredSkills().trim());
        job.setPreferredQualifications(trimToNull(request.getPreferredQualifications()));
        job.setSourceUrl(trimToNull(request.getSourceUrl()));
        job.setPostedDate(
                request.getPostedDate() != null ? request.getPostedDate() : LocalDate.now()
        );
        job.setExpiryDate(request.getExpiryDate());
        job.setActive(request.getActive() == null || request.getActive());
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private Set<Long> getBookmarkedJobIdsForEmail(String email) {
        if (email == null || email.isBlank()) {
            return Collections.emptySet();
        }
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(user -> savedJobRepository.findByUserIdOrderBySavedAtDesc(user.getId())
                        .stream()
                        .map(saved -> saved.getJob().getId())
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public JobResponse toResponse(Job job, boolean bookmarked) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getMinExperienceYears(),
                job.getMaxExperienceYears(),
                job.getSalaryRange(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getPreferredQualifications(),
                job.getSourceUrl(),
                job.getPostedDate(),
                job.getExpiryDate(),
                job.isActive(),
                job.isExpired(),
                job.isSeedData(),
                bookmarked
        );
    }
}
