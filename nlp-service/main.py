"""
FastAPI Service for AI/NLP Job Matching.
Run with: uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

from typing import List, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from matching_engine import calculate_match

app = FastAPI(
    title="AI Job Matching NLP Service",
    description="Provides explainable skill extraction, normalization, and job-matching scores.",
    version="1.0.0",
)


class MatchRequestPayload(BaseModel):
    candidateSkills: Optional[str] = ""
    candidateExperienceYears: Optional[int] = Field(default=0, ge=0)
    candidateEducation: Optional[str] = ""
    resumeText: Optional[str] = ""
    jobTitle: str = Field(..., min_length=1)
    jobDescription: str = Field(..., min_length=1)
    jobRequiredSkills: str = Field(..., min_length=1)
    jobPreferredQualifications: Optional[str] = ""
    jobMinExperienceYears: Optional[int] = Field(default=0, ge=0)


class MatchResponsePayload(BaseModel):
    matchScore: int
    matchedSkills: List[str]
    missingSkills: List[str]
    explanation: str


@app.get("/health")
def health_check():
    return {"status": "UP", "service": "nlp-job-matching-service"}


@app.post("/api/nlp/match", response_model=MatchResponsePayload)
def match_candidate_to_job(payload: MatchRequestPayload):
    try:
        result = calculate_match(
            candidate_skills=payload.candidateSkills,
            candidate_experience_years=payload.candidateExperienceYears,
            candidate_education=payload.candidateEducation,
            resume_text=payload.resumeText,
            job_title=payload.jobTitle,
            job_description=payload.jobDescription,
            job_required_skills=payload.jobRequiredSkills,
            job_preferred_qualifications=payload.jobPreferredQualifications,
            job_min_experience_years=payload.jobMinExperienceYears,
        )
        return result
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
