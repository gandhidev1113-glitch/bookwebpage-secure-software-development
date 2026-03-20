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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndBorrowIntegrationTest {

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
    void unauthenticatedRequest_toProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void userCannotAddBook_butLibrarianCan() throws Exception {
        registerUser("user1", "user1@lib.com");
        String userToken = login("user1", "StrongPass123");

        mockMvc.perform(post("/api/books/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Clean Code",
                                "author", "Robert C. Martin",
                                "category", "Programming"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        registerLibrarian("librarian1", "librarian1@lib.com");
        String librarianToken = login("librarian1", "StrongPass123");

        mockMvc.perform(post("/api/books/add")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Clean Code",
                                "author", "Robert C. Martin",
                                "category", "Programming"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void librarianCanApproveBorrow_andBookBecomesUnavailable() throws Exception {
        registerLibrarian("librarian1", "librarian1@lib.com");
        String librarianToken = login("librarian1", "StrongPass123");

        MvcResult addBookResult = mockMvc.perform(post("/api/books/add")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Domain-Driven Design",
                                "author", "Eric Evans",
                                "category", "Software"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        long bookId = Long.parseLong(readJson(addBookResult).get("id").toString());

        registerUser("user1", "user1@lib.com");
        String userToken = login("user1", "StrongPass123");

        MvcResult requestResult = mockMvc.perform(post("/api/borrow/request/{bookId}", bookId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long requestId = Long.parseLong(readJson(requestResult).get("requestId").toString());

        mockMvc.perform(get("/api/borrow/pending")
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        mockMvc.perform(put("/api/borrow/approve/{requestId}", requestId)
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/borrow/request/{bookId}", bookId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Book is not available"));
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

    private void registerLibrarian(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register-librarian")
                        .header("X-Setup-Token", "test-bootstrap-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", email,
                                "password", "StrongPass123"
                        ))))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
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
