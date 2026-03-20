package com.library.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRequestRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        borrowRequestRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_login_and_me_flow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user1",
                                "email", "user1@lib.com",
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.role").value("USER"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user1",
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = readJson(loginResult).get("token").toString();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    void registerLibrarian_requiresBootstrapToken() throws Exception {
        mockMvc.perform(post("/api/auth/register-librarian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "lib1",
                                "email", "lib1@lib.com",
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid setup token"));

        mockMvc.perform(post("/api/auth/register-librarian")
                        .header("X-Setup-Token", "test-bootstrap-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "lib1",
                                "email", "lib1@lib.com",
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"));
    }

    private Map<String, Object> readJson(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }
}
