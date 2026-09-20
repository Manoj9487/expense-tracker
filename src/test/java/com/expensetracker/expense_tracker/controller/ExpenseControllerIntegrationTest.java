package com.expensetracker.expense_tracker.controller;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    private String jwtToken;

    // ===== Register + log in a fresh test user before every test, and
    // capture the JWT so each request below can attach it. Using a
    // per-test-run unique username avoids "user already exists" failures
    // if tests run more than once against a non-fresh H2 instance. =====
    @BeforeEach
    void setUpAuthenticatedUser() throws Exception {
        String username = "testuser_" + System.nanoTime();
        String registerBody = """
            {
              "username": "%s",
              "password": "TestPass123"
            }
            """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated()); // register returns 201, not 200

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(loginResponse).get("token").asText();
    }

    // Helper so every request below attaches the Authorization header
    // without repeating .header(...) on every single call.
    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + jwtToken);
    }

    @Test
    void createExpense_withValidData_returns201AndPersistedExpense() throws Exception {
        String requestBody = """
            {
              "title": "Groceries",
              "amount": 45.50,
              "category": "FOOD",
              "expenseDate": "2026-09-10",
              "paymentMethod": "UPI",
              "description": "Weekly shop"
            }
            """;

        mockMvc.perform(withAuth(post("/api/expenses"))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.amount").value(45.50))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createExpense_withBlankTitle_returns400WithFieldError() throws Exception {
        String requestBody = """
            {
              "title": "",
              "amount": 10,
              "category": "FOOD",
              "expenseDate": "2026-09-10",
              "paymentMethod": "CASH"
            }
            """;

        mockMvc.perform(withAuth(post("/api/expenses"))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void getExpenseById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(withAuth(get("/api/expenses/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test
    void allExpenseEndpoints_withoutToken_return401() throws Exception {
        // No Authorization header attached here on purpose — this is the
        // test that proves the API actually enforces auth, not just that
        // it works when you remember to send a token.
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullCrudLifecycle_createReadUpdateDelete() throws Exception {
        // CREATE
        String createBody = """
            {
              "title": "Bus pass",
              "amount": 20.00,
              "category": "TRANSPORT",
              "expenseDate": "2026-09-12",
              "paymentMethod": "CASH"
            }
            """;

        String response = mockMvc.perform(withAuth(post("/api/expenses"))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(response).get("id").asLong();

        // READ
        mockMvc.perform(withAuth(get("/api/expenses/" + createdId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bus pass"));

        // UPDATE
        String updateBody = """
            {
              "title": "Bus pass (updated)",
              "amount": 25.00,
              "category": "TRANSPORT",
              "expenseDate": "2026-09-12",
              "paymentMethod": "CARD"
            }
            """;

        mockMvc.perform(withAuth(put("/api/expenses/" + createdId))
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bus pass (updated)"))
                .andExpect(jsonPath("$.amount").value(25.00));

        // DELETE
        mockMvc.perform(withAuth(delete("/api/expenses/" + createdId)))
                .andExpect(status().isNoContent());

        // CONFIRM GONE
        mockMvc.perform(withAuth(get("/api/expenses/" + createdId)))
                .andExpect(status().isNotFound());
    }
}