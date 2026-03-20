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
class ExceptionHandlingIntegrationTest {

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
    void registerWithInvalidPayload_returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "",
                                "email", "not-an-email",
                                "password", "123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void invalidPathParameter_returnsSanitizedValidationError() throws Exception {
        registerUser("user1", "user1@lib.com");
        String token = login("user1", "StrongPass123");

        MvcResult result = mockMvc.perform(get("/api/books/{id}", 0)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andReturn();

        String payload = result.getResponse().getContentAsString();
        assertThat(payload).doesNotContain("stackTrace");
        assertThat(payload).doesNotContain("java.lang");
    }

    private void registerUser(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", email,
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return readJson(result).get("token").toString();
    }

    private Map<String, Object> readJson(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }
}
