import unittest
from matching_engine import calculate_match, extract_and_normalize_skills


class TestMatchingEngine(unittest.TestCase):

    def test_skill_normalization_and_aliases(self):
        skills = extract_and_normalize_skills("springboot, reactjs, mysql", "Built REST APIs using core java")
        self.assertIn("Spring Boot", skills)
        self.assertIn("React", skills)
        self.assertIn("MySQL", skills)
        self.assertIn("REST APIs", skills)
        self.assertIn("Java", skills)

    def test_strong_match(self):
        result = calculate_match(
            candidate_skills="Java, Spring Boot, MySQL, REST APIs, Git",
            candidate_experience_years=3,
            candidate_education="B.Tech Computer Science",
            resume_text="Designed scalable Java and Spring Boot REST APIs backed by MySQL.",
            job_title="Java Backend Engineer",
            job_description="Build scalable REST APIs with Java, Spring Boot, and MySQL.",
            job_required_skills="Java, Spring Boot, MySQL, REST APIs",
            job_preferred_qualifications="Docker, AWS",
            job_min_experience_years=2,
        )
        self.assertGreaterEqual(result["matchScore"], 85)
        self.assertEqual(len(result["missingSkills"]), 0)
        self.assertIn("Java", result["matchedSkills"])
        self.assertIn("Spring Boot", result["matchedSkills"])

    def test_partial_match(self):
        result = calculate_match(
            candidate_skills="Java, Spring Boot, MySQL",
            candidate_experience_years=2,
            candidate_education="B.Tech CSE",
            resume_text="Java Spring Boot developer working with MySQL databases.",
            job_title="Senior Backend Developer",
            job_description="Develop microservices using Java, Spring Boot, MySQL, and Docker.",
            job_required_skills="Java, Spring Boot, MySQL, Docker",
            job_min_experience_years=2,
        )
        self.assertTrue(60 <= result["matchScore"] <= 90)
        self.assertIn("Docker", result["missingSkills"])
        self.assertIn("Java", result["matchedSkills"])

    def test_weak_match(self):
        result = calculate_match(
            candidate_skills="HTML, CSS",
            candidate_experience_years=0,
            candidate_education="B.A.",
            resume_text="Created static web pages with HTML and CSS.",
            job_title="AI / NLP Engineer",
            job_description="Build deep learning and NLP services with Python, FastAPI, and Machine Learning.",
            job_required_skills="Python, FastAPI, NLP, Machine Learning, SQL",
            job_min_experience_years=3,
        )
        self.assertLess(result["matchScore"], 25)
        self.assertEqual(len(result["matchedSkills"]), 0)
        self.assertIn("Python", result["missingSkills"])

    def test_empty_candidate_input_returns_zero_score(self):
        result = calculate_match(
            candidate_skills="",
            candidate_experience_years=0,
            candidate_education="",
            resume_text="",
            job_title="Java Developer",
            job_description="Java role",
            job_required_skills="Java, Spring Boot",
            job_min_experience_years=1,
        )
        self.assertEqual(result["matchScore"], 0)
        self.assertIn("No candidate skills", result["explanation"])

    def test_invalid_empty_job_input_raises_error(self):
        with self.assertRaises(ValueError):
            calculate_match(
                candidate_skills="Java",
                candidate_experience_years=1,
                candidate_education="",
                resume_text="",
                job_title="   ",
                job_description="Desc",
                job_required_skills="",
            )


if __name__ == "__main__":
    unittest.main()
