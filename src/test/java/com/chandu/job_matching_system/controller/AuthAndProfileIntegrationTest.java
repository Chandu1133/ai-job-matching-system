package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.repository.UserRepository;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthAndProfileIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private Cookie fetchCsrfCookie(MockHttpSession session) throws Exception {
        var requestBuilder = get("/api/auth/csrf");
        if (session != null) {
            requestBuilder.session(session);
        }

        MvcResult csrfResult = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(xsrfCookie, "XSRF-TOKEN cookie should be generated");
        return xsrfCookie;
    }

    @Test
    void fullAuthAndProfileLifecycleShouldSucceed() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie(null);

        // 1. Register a new candidate
        String registerJson = """
                {
                  "name": "Chandu Candidate",
                  "email": "chandu@example.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Chandu Candidate"))
                .andExpect(jsonPath("$.email").value("chandu@example.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        // 2. Duplicate registration should fail with 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 3. Login with valid credentials
        String loginJson = """
                {
                  "email": "chandu@example.com",
                  "password": "Password123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Chandu Candidate"))
                .andExpect(jsonPath("$.email").value("chandu@example.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Authenticated HTTP session must be created");

        // 4. Retrieve authenticated user's profile
        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chandu Candidate"))
                .andExpect(jsonPath("$.email").value("chandu@example.com"));

        // 5. Update authenticated user's profile
        Cookie postLoginCsrf = fetchCsrfCookie(session);
        String updateProfileJson = """
                {
                  "name": "Chandu Kumar",
                  "phone": "+91-9876543210",
                  "location": "Hyderabad, India",
                  "education": "B.Tech in Computer Science",
                  "skills": "Java, Spring Boot, MySQL, Python, React",
                  "preferredJobRole": "Full Stack Engineer",
                  "experienceYears": 2
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .session(session)
                        .cookie(postLoginCsrf)
                        .header("X-XSRF-TOKEN", postLoginCsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateProfileJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chandu Kumar"))
                .andExpect(jsonPath("$.location").value("Hyderabad, India"))
                .andExpect(jsonPath("$.skills").value("Java, Spring Boot, MySQL, Python, React"))
                .andExpect(jsonPath("$.experienceYears").value(2))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        // 6. Logout and verify session is invalidated
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(postLoginCsrf)
                        .header("X-XSRF-TOKEN", postLoginCsrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAndValidationAndUnauthorizedAccessShouldBeRejected() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie(null);

        // 1. Unauthenticated request to /api/users/me -> 401
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        // 2. Invalid registration payload -> 400 with field errors
        String invalidRegisterJson = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRegisterJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        // 3. Invalid login credentials -> 401
        String badLoginJson = """
                {
                  "email": "nonexistent@example.com",
                  "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
