package com.chandu.job_matching_system.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ResumeParserService {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L; // 5 MB

    private static final List<String> KNOWN_SKILLS = List.of(
            "Java", "Python", "Spring Boot", "Spring Security", "Spring Data JPA", "Hibernate",
            "FastAPI", "Flask", "Django", "MySQL", "PostgreSQL", "MongoDB", "SQL",
            "REST APIs", "Microservices", "NLP", "LLMs", "Generative AI",
            "Machine Learning", "Deep Learning", "PyTorch", "NLTK", "Scikit-learn", "TensorFlow",
            "JavaScript", "TypeScript", "React", "HTML", "CSS", "Tailwind CSS", "Node.js",
            "Docker", "Kubernetes", "AWS", "Cloud Computing", "Git", "GitHub",
            "Maven", "JUnit", "Mockito", "Pytest", "Problem Solving", "Leadership"
    );

    private static final List<String> ALL_SECTION_HEADERS = List.of(
            "objective", "career objective", "professional summary", "summary", "profile",
            "skills", "technical skills",
            "education", "academic background", "qualifications",
            "work experience", "professional experience", "experience", "employment history", "internships",
            "projects", "academic projects", "personal projects", "key projects",
            "certifications", "certificates",
            "achievements", "accomplishments", "awards",
            "other languages and interests", "languages and interests", "interests", "languages"
    );

    public record ParsedResumeData(
            String fileType,
            String extractedText,
            String extractedSkills,
            String extractedEducation,
            String extractedExperience,
            String extractedProjects,
            String extractionStatus
    ) {}

    public ParsedResumeData validateAndParse(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uploaded resume file cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume file size exceeds the 5 MB limit"
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume filename is required"
            );
        }

        String lowerName = originalName.trim().toLowerCase(Locale.ROOT);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded file"
            );
        }

        String rawText;
        String normalizedType;

        if (lowerName.endsWith(".pdf")) {
            validatePdfBytes(bytes);
            normalizedType = "PDF";
            rawText = extractTextFromPdf(bytes);
        } else if (lowerName.endsWith(".docx")) {
            normalizedType = "DOCX";
            rawText = extractTextFromDocx(bytes);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Only PDF and DOCX files are allowed"
            );
        }

        return parseStructuredText(normalizedType, rawText);
    }

    public ParsedResumeData parseStructuredText(String normalizedType, String rawText) {
        String cleanedText = cleanText(rawText);
        String status = cleanedText.isBlank() ? "NEEDS_REVIEW" : "PARSED";

        String skills = extractSkills(cleanedText);
        String education = extractSection(
                cleanedText,
                List.of("education", "academic background", "qualifications")
        );
        String experience = extractSection(
                cleanedText,
                List.of("work experience", "professional experience", "experience", "employment history", "internships")
        );
        if (experience == null || experience.isBlank()) {
            String certs = extractSection(cleanedText, List.of("certifications", "certificates"));
            String achievements = extractSection(cleanedText, List.of("achievements", "accomplishments", "awards"));
            StringBuilder fallbackExp = new StringBuilder();
            if (certs != null && !certs.isBlank()) {
                fallbackExp.append("Certifications:\n").append(certs);
            }
            if (achievements != null && !achievements.isBlank()) {
                if (!fallbackExp.isEmpty()) fallbackExp.append("\n\n");
                fallbackExp.append("Achievements:\n").append(achievements);
            }
            if (!fallbackExp.isEmpty()) {
                experience = fallbackExp.toString();
            }
        }

        String projects = extractSection(
                cleanedText,
                List.of("projects", "academic projects", "personal projects", "key projects")
        );

        return new ParsedResumeData(
                normalizedType,
                cleanedText,
                skills,
                education,
                experience,
                projects,
                status
        );
    }

    private void validatePdfBytes(byte[] bytes) {
        if (bytes.length < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed or corrupted PDF document"
            );
        }
        String header = new String(bytes, 0, 5, StandardCharsets.ISO_8859_1);
        if (!header.startsWith("%PDF-")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed or corrupted PDF document"
            );
        }
    }

    private String extractTextFromDocx(byte[] bytes) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed or corrupted DOCX document"
            );
        }

        byte[] documentXmlBytes = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    documentXmlBytes = zis.readAllBytes();
                    break;
                }
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed or corrupted DOCX archive"
            );
        }

        if (documentXmlBytes == null || documentXmlBytes.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed DOCX document: missing word/document.xml"
            );
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(documentXmlBytes));

            StringBuilder sb = new StringBuilder();
            NodeList paragraphs = doc.getElementsByTagName("w:p");
            if (paragraphs.getLength() > 0) {
                for (int i = 0; i < paragraphs.getLength(); i++) {
                    String paraText = paragraphs.item(i).getTextContent();
                    if (paraText != null && !paraText.isBlank()) {
                        sb.append(paraText.trim()).append("\n");
                    }
                }
            } else {
                String fallback = doc.getDocumentElement().getTextContent();
                if (fallback != null) {
                    sb.append(fallback.trim());
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Malformed XML inside DOCX document"
            );
        }
    }

    public String extractTextFromPdf(byte[] bytes) {
        String rawIso = new String(bytes, StandardCharsets.ISO_8859_1);
        List<String> contentStreams = new ArrayList<>();

        Pattern streamPattern = Pattern.compile("stream\\r?\\n([\\s\\S]*?)\\r?\\nendstream");
        Matcher streamMatcher = streamPattern.matcher(rawIso);
        while (streamMatcher.find()) {
            String streamBody = streamMatcher.group(1);
            byte[] streamBytes = streamBody.getBytes(StandardCharsets.ISO_8859_1);
            String inflated = tryInflate(streamBytes);
            String candidate = (inflated != null && !inflated.isBlank()) ? inflated : streamBody;

            if (candidate.contains("%!PS-AdobeFont")
                    || candidate.contains("%!FontType1")
                    || candidate.contains("begincmap")
                    || candidate.contains("/CIDInit")) {
                continue;
            }
            if (candidate.contains("BT") && candidate.contains("ET")) {
                contentStreams.add(candidate);
            }
        }

        if (contentStreams.isEmpty() && rawIso.contains("BT") && rawIso.contains("ET")) {
            contentStreams.add(rawIso);
        }

        StringBuilder fullText = new StringBuilder();
        Pattern btEtPattern = Pattern.compile("BT([\\s\\S]*?)ET");

        Pattern opPattern = Pattern.compile(
                "(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+T[dD]"
                + "|T\\*"
                + "|\\[([^\\]]*)\\]\\s*TJ"
                + "|\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*(?:Tj|'|\\\")"
        );

        for (String stream : contentStreams) {
            Matcher btMatcher = btEtPattern.matcher(stream);
            while (btMatcher.find()) {
                String btBlock = btMatcher.group(1);
                Matcher opMatcher = opPattern.matcher(btBlock);
                while (opMatcher.find()) {
                    if (opMatcher.group(1) != null && opMatcher.group(2) != null) {
                        double dx = Double.parseDouble(opMatcher.group(1));
                        double dy = Double.parseDouble(opMatcher.group(2));
                        if (Math.abs(dy) > 1.0) {
                            fullText.append("\n");
                        } else if (dx > 5.0) {
                            fullText.append(" ");
                        }
                    } else if (opMatcher.group(0).equals("T*")) {
                        fullText.append("\n");
                    } else if (opMatcher.group(3) != null) {
                        String tjArray = opMatcher.group(3);
                        fullText.append(parseTjArray(tjArray));
                    } else if (opMatcher.group(4) != null) {
                        fullText.append(decodePdfString(opMatcher.group(4)));
                    }
                }
                fullText.append("\n");
            }
        }

        return fullText.toString();
    }

    private String parseTjArray(String tjArray) {
        StringBuilder sb = new StringBuilder();
        Pattern itemPattern = Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)|(-?\\d+(?:\\.\\d+)?)");
        Matcher m = itemPattern.matcher(tjArray);
        while (m.find()) {
            if (m.group(1) != null) {
                sb.append(decodePdfString(m.group(1)));
            } else if (m.group(2) != null) {
                double kern = Double.parseDouble(m.group(2));
                if (kern <= -150.0) {
                    if (sb.isEmpty() || sb.charAt(sb.length() - 1) != ' ') {
                        sb.append(' ');
                    }
                }
            }
        }
        return sb.toString();
    }

    public String decodePdfString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String decoded = raw
                .replace("\\050", "(")
                .replace("\\051", ")")
                .replace("\\014", "fi")
                .replace("\\013", "ff")
                .replace("\\015", "fl")
                .replace("\\210", "• ")
                .replace("\\n", "\n")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\\\", "\\");

        Matcher octal = Pattern.compile("\\\\([0-7]{3})").matcher(decoded);
        StringBuilder sb = new StringBuilder();
        while (octal.find()) {
            int code = Integer.parseInt(octal.group(1), 8);
            char replacement = (code >= 32 && code <= 126) ? (char) code : ' ';
            octal.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
        }
        octal.appendTail(sb);
        return sb.toString();
    }

    private String tryInflate(byte[] compressed) {
        for (boolean nowrap : new boolean[]{false, true}) {
            Inflater inflater = new Inflater(nowrap);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                inflater.setInput(compressed);
                byte[] buffer = new byte[1024];
                while (!inflater.finished() && !inflater.needsInput()) {
                    int count = inflater.inflate(buffer);
                    if (count == 0) {
                        break;
                    }
                    out.write(buffer, 0, count);
                }
                if (out.size() > 0) {
                    return out.toString(StandardCharsets.ISO_8859_1);
                }
            } catch (Exception ignored) {
                // Ignore non-FlateDecode streams
            } finally {
                inflater.end();
            }
        }
        return null;
    }

    public String cleanText(String input) {
        if (input == null) {
            return "";
        }
        String s = decodePdfString(input);

        // Repair common LaTeX kerning-split words from older or inline extractions
        s = s.replaceAll("(?i)\\bPer\\s+c\\s+entage\\b", "Percentage")
             .replaceAll("(?i)\\bT\\s+elangana\\b", "Telangana")
             .replaceAll("(?i)\\bAI-P\\s*o\\s*w\\s*ered\\b", "AI-Powered")
             .replaceAll("(?i)\\bP\\s*o\\s*w\\s*ered\\b", "Powered")
             .replaceAll("(?i)\\bMatc\\s+hing\\b", "Matching")
             .replaceAll("(?i)\\bmatc\\s+h\\b", "match")
             .replaceAll("(?i)\\bSpring\\s+Bo\\s+ot\\b", "Spring Boot")
             .replaceAll("(?i)\\bBo\\s+ot\\b", "Boot")
             .replaceAll("(?i)\\bF\\s+astAPI\\b", "FastAPI")
             .replaceAll("(?i)\\bJa\\s*v\\s*a\\b", "Java")
             .replaceAll("(?i)\\bNL\\s+TK\\b", "NLTK")
             .replaceAll("(?i)\\bPy\\s+Torch\\b", "PyTorch")
             .replaceAll("(?i)\\bDev\\s+elop\\s*ed\\b", "Developed")
             .replaceAll("(?i)\\bDev\\s+elopmen\\s*t?\\b", "Development")
             .replaceAll("(?i)\\brelev\\s+an\\s*t?\\b", "relevant")
             .replaceAll("(?i)\\bcandi\\s+dates\\b", "candidates")
             .replaceAll("(?i)\\bop\\s+timization\\b", "optimization")
             .replaceAll("(?i)\\bplat\\s+form\\b", "platform")
             .replaceAll("(?i)\\bIn\\s+telligent\\b", "Intelligent");

        // Ensure major resume section headers always sit on their own lines
        s = s.replaceAll("(?i)\\bEDUCA\\s*TION\\b", "\nEDUCATION\n")
             .replaceAll("(?i)\\bPR\\s*OJECTS\\b", "\nPROJECTS\n")
             .replaceAll("(?i)\\bCER\\s*TIFICA\\s*TIONS\\b", "\nCERTIFICATIONS\n")
             .replaceAll("(?i)\\bWORK\\s+EXPERIENCE\\b", "\nWORK EXPERIENCE\n")
             .replaceAll("(?i)\\bPROFESSIONAL\\s+EXPERIENCE\\b", "\nPROFESSIONAL EXPERIENCE\n")
             .replaceAll("(?i)\\bTECHNICAL\\s+SKILLS\\b", "\nTECHNICAL SKILLS\n")
             .replaceAll("(?i)\\bACHIEVEMENTS\\b", "\nACHIEVEMENTS\n")
             .replaceAll("(?i)\\bOTHER\\s+LANGUAGES\\s+AND\\s+INTERESTS\\b", "\nOTHER LANGUAGES AND INTERESTS\n");

        return s
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
    }

    public String extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        Set<String> matched = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            String pattern = "(?i)(?:^|[^a-z0-9])" + Pattern.quote(skill.toLowerCase(Locale.ROOT)) + "(?:$|[^a-z0-9])";
            if (Pattern.compile(pattern).matcher(lowerText).find()) {
                matched.add(skill);
            }
        }
        return matched.isEmpty() ? null : String.join(", ", matched);
    }

    public String extractSection(String text, List<String> targetHeaders) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] lines = text.split("\\r?\\n");
        int startLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String cleanLine = lines[i].trim().toLowerCase(Locale.ROOT).replaceAll("[:\\-]+$", "").trim();
            if (targetHeaders.contains(cleanLine)) {
                startLine = i + 1;
                break;
            }
        }

        if (startLine == -1 || startLine >= lines.length) {
            return null;
        }

        StringBuilder sectionBuilder = new StringBuilder();
        for (int i = startLine; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            String normalizedHeaderCheck = trimmedLine.toLowerCase(Locale.ROOT).replaceAll("[:\\-]+$", "").trim();
            if (!targetHeaders.contains(normalizedHeaderCheck)
                    && ALL_SECTION_HEADERS.contains(normalizedHeaderCheck)) {
                break;
            }
            if (!trimmedLine.isEmpty()) {
                if (!sectionBuilder.isEmpty()) {
                    sectionBuilder.append("\n");
                }
                sectionBuilder.append(trimmedLine);
            }
        }

        String result = sectionBuilder.toString().trim();
        return result.isBlank() ? null : result;
    }
}
