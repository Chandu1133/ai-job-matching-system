"""
Core AI/NLP Job Matching Engine.
Implements:
- Step 1: Text & Skill Normalization (with alias/synonym mapping)
- Step 2: Candidate vs. Required Job Skills Comparison
- Step 3: Experience & Education/Keyword Alignment
- Step 4: Weighted 0-100 Score Calculation (65% Skills, 20% Experience, 15% Text/Qualification Similarity)
- Step 5: Explainable Match Breakdown (Never claims hiring probability)
"""

import math
import re
from collections import Counter
from typing import Dict, List, Optional, Tuple

SKILL_ALIASES: Dict[str, str] = {
    "java": "Java",
    "core java": "Java",
    "spring": "Spring Boot",
    "springboot": "Spring Boot",
    "spring boot": "Spring Boot",
    "spring security": "Spring Security",
    "spring data jpa": "Spring Data JPA",
    "jpa": "Spring Data JPA",
    "hibernate": "Hibernate",
    "mysql": "MySQL",
    "postgresql": "PostgreSQL",
    "postgres": "PostgreSQL",
    "mongodb": "MongoDB",
    "sql": "SQL",
    "rest": "REST APIs",
    "rest api": "REST APIs",
    "rest apis": "REST APIs",
    "restful apis": "REST APIs",
    "microservices": "Microservices",
    "python": "Python",
    "fastapi": "FastAPI",
    "flask": "Flask",
    "django": "Django",
    "nlp": "NLP",
    "natural language processing": "NLP",
    "llm": "LLMs",
    "llms": "LLMs",
    "generative ai": "Generative AI",
    "machine learning": "Machine Learning",
    "ml": "Machine Learning",
    "deep learning": "Deep Learning",
    "pytorch": "PyTorch",
    "tensorflow": "TensorFlow",
    "nltk": "NLTK",
    "javascript": "JavaScript",
    "js": "JavaScript",
    "typescript": "TypeScript",
    "ts": "TypeScript",
    "react": "React",
    "reactjs": "React",
    "react.js": "React",
    "html": "HTML",
    "css": "CSS",
    "tailwind": "Tailwind CSS",
    "tailwind css": "Tailwind CSS",
    "node": "Node.js",
    "nodejs": "Node.js",
    "node.js": "Node.js",
    "docker": "Docker",
    "kubernetes": "Kubernetes",
    "k8s": "Kubernetes",
    "aws": "AWS",
    "cloud computing": "Cloud Computing",
    "git": "Git",
    "github": "GitHub",
    "maven": "Maven",
    "junit": "JUnit",
    "mockito": "Mockito",
    "pytest": "Pytest",
}

STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has",
    "in", "is", "it", "of", "on", "or", "that", "the", "to", "was", "were",
    "will", "with", "we", "you", "your", "our", "their", "this", "these"
}


def normalize_skill_token(raw_token: str) -> str:
    cleaned = re.sub(r"\s+", " ", raw_token.strip().lower())
    if not cleaned:
        return ""
    if cleaned in SKILL_ALIASES:
        return SKILL_ALIASES[cleaned]
    return raw_token.strip()


def extract_and_normalize_skills(skills_csv: Optional[str], extra_text: Optional[str] = None) -> List[str]:
    normalized_set: Dict[str, str] = {}

    if skills_csv:
        tokens = re.split(r"[,;/|\n]+", skills_csv)
        for token in tokens:
            norm = normalize_skill_token(token)
            if norm:
                normalized_set[norm.lower()] = norm

    if extra_text:
        lower_text = extra_text.lower()
        for alias, canonical in SKILL_ALIASES.items():
            pattern = r"\b" + re.escape(alias) + r"\b"
            if re.search(pattern, lower_text):
                normalized_set[canonical.lower()] = canonical

    return list(normalized_set.values())


def compute_cosine_similarity(text_a: str, text_b: str) -> float:
    tokens_a = [
        w for w in re.findall(r"[a-z0-9+#.]+", (text_a or "").lower())
        if len(w) > 1 and w not in STOP_WORDS
    ]
    tokens_b = [
        w for w in re.findall(r"[a-z0-9+#.]+", (text_b or "").lower())
        if len(w) > 1 and w not in STOP_WORDS
    ]
    if not tokens_a or not tokens_b:
        return 0.0

    counts_a = Counter(tokens_a)
    counts_b = Counter(tokens_b)
    common = set(counts_a.keys()) & set(counts_b.keys())
    dot_product = sum(counts_a[w] * counts_b[w] for w in common)

    norm_a = math.sqrt(sum(v * v for v in counts_a.values()))
    norm_b = math.sqrt(sum(v * v for v in counts_b.values()))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0

    return dot_product / (norm_a * norm_b)


def calculate_match(
    candidate_skills: Optional[str],
    candidate_experience_years: Optional[int],
    candidate_education: Optional[str],
    resume_text: Optional[str],
    job_title: str,
    job_description: str,
    job_required_skills: str,
    job_preferred_qualifications: Optional[str] = None,
    job_min_experience_years: Optional[int] = 0,
) -> Dict[str, object]:
    if not job_title or not job_title.strip() or not job_required_skills or not job_required_skills.strip():
        raise ValueError("jobTitle and jobRequiredSkills must not be empty")

    # Step 1: Normalize candidate & job skills
    combined_candidate_text = " ".join(
        filter(None, [resume_text, candidate_education])
    )
    cand_skills_list = extract_and_normalize_skills(candidate_skills, combined_candidate_text)
    req_skills_list = extract_and_normalize_skills(job_required_skills)

    cand_skill_map = {s.lower(): s for s in cand_skills_list}

    # Step 2: Compare candidate skills with required job skills
    matched_skills: List[str] = []
    missing_skills: List[str] = []

    for req in req_skills_list:
        if req.lower() in cand_skill_map:
            matched_skills.append(req)
        else:
            missing_skills.append(req)

    if req_skills_list:
        skill_ratio = len(matched_skills) / len(req_skills_list)
    else:
        skill_ratio = 0.0

    # Step 3: Experience alignment (weight: 20%)
    req_exp = max(job_min_experience_years or 0, 0)
    cand_exp = candidate_experience_years if (candidate_experience_years is not None and candidate_experience_years >= 0) else 0

    if req_exp == 0:
        exp_ratio = 1.0 if (cand_skills_list or combined_candidate_text.strip()) else 0.0
    elif cand_exp >= req_exp:
        exp_ratio = 1.0
    else:
        exp_ratio = max(0.0, cand_exp / float(req_exp))

    # Step 4: Semantic / keyword overlap on description & preferred qualifications (weight: 15%)
    candidate_corpus = " ".join(
        filter(None, [candidate_skills, candidate_education, resume_text])
    )
    job_corpus = " ".join(
        filter(None, [job_title, job_description, job_required_skills, job_preferred_qualifications])
    )
    similarity = compute_cosine_similarity(candidate_corpus, job_corpus)
    text_ratio = min(1.0, similarity * 1.8)

    if not cand_skills_list and not candidate_corpus.strip():
        match_score = 0
    else:
        raw_score = (skill_ratio * 65.0) + (expRatio_val := exp_ratio * 20.0) + (text_ratio * 15.0)
        match_score = int(round(max(0.0, min(100.0, raw_score))))

    # Step 5: Generate human-readable explanation (never claims hiring probability)
    explanation = build_explanation(
        match_score=match_score,
        matched_skills=matched_skills,
        missing_skills=missing_skills,
        cand_exp=cand_exp,
        req_exp=req_exp,
        has_candidate_data=bool(cand_skills_list or candidate_corpus.strip()),
    )

    return {
        "matchScore": match_score,
        "matchedSkills": matched_skills,
        "missingSkills": missing_skills,
        "explanation": explanation,
    }


def build_explanation(
    match_score: int,
    matched_skills: List[str],
    missing_skills: List[str],
    cand_exp: int,
    req_exp: int,
    has_candidate_data: bool,
) -> str:
    if not has_candidate_data:
        return (
            "No candidate skills or resume details were provided. "
            "Please update your profile or upload a resume to compute a meaningful skill alignment score."
        )

    parts: List[str] = []
    if matched_skills and not missing_skills:
        parts.append(
            f"Strong skill alignment: the candidate matches all {len(matched_skills)} required skills "
            f"({', '.join(matched_skills)})."
        )
    elif matched_skills:
        parts.append(
            f"The candidate matches {len(matched_skills)} required skill(s) ({', '.join(matched_skills)}), "
            f"while {', '.join(missing_skills)} experience is not identified in the supplied profile/resume."
        )
    else:
        parts.append(
            f"None of the required technical skills ({', '.join(missing_skills)}) were identified in the supplied profile or resume."
        )

    if req_exp > 0:
        if cand_exp >= req_exp:
            parts.append(f"Reported experience ({cand_exp} yr(s)) meets the minimum requirement ({req_exp} yr(s)).")
        else:
            parts.append(f"Reported experience ({cand_exp} yr(s)) is below the preferred {req_exp} yr(s).")

    parts.append(
        "Note: This score reflects profile-to-job requirement alignment and is not a guarantee or probability of being hired."
    )
    return " ".join(parts)
