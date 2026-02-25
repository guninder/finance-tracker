package com.financetracker.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.server.dto.RegisterRequest;
import com.financetracker.server.dto.TransactionRequest;
import com.financetracker.server.repository.TransactionRepository;
import com.financetracker.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        // Register a test user and get token
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(responseBody).get("token").asText();
    }

    @Test
    void getTransactions_empty() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTransactions_unauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_success() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "Food", "Lunch", LocalDate.of(2026, 2, 25));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.date").value("2026-02-25"));
    }

    @Test
    void createTransaction_missingAmount_returns400() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "EXPENSE", null, "Food", "Lunch", LocalDate.of(2026, 2, 25));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createTransaction_negativeAmount_returns400() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "EXPENSE", new BigDecimal("-10.00"), "Food", "Lunch", LocalDate.of(2026, 2, 25));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void fullCrudFlow() throws Exception {
        // Create an INCOME transaction
        TransactionRequest incomeRequest = new TransactionRequest(
                "INCOME", new BigDecimal("5000.00"), "Salary", "Monthly salary", LocalDate.of(2026, 2, 1));

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andReturn();

        Long transactionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Create an EXPENSE transaction
        TransactionRequest expenseRequest = new TransactionRequest(
                "EXPENSE", new BigDecimal("200.00"), "Food", "Groceries", LocalDate.of(2026, 2, 15));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        // Get all transactions
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Get summary
        mockMvc.perform(get("/api/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.balance").value(4800.00));

        // Update the income transaction
        TransactionRequest updateRequest = new TransactionRequest(
                "INCOME", new BigDecimal("6000.00"), "Salary", "Updated salary", LocalDate.of(2026, 2, 1));

        mockMvc.perform(put("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(6000.00))
                .andExpect(jsonPath("$.description").value("Updated salary"));

        // Verify updated summary
        mockMvc.perform(get("/api/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(6000.00))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.balance").value(5800.00));

        // Delete the income transaction
        mockMvc.perform(delete("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify only expense remains
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));

        // Verify updated summary after delete
        mockMvc.perform(get("/api/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.balance").value(-200.00));
    }

    @Test
    void getSummary_empty() throws Exception {
        mockMvc.perform(get("/api/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void deleteTransaction_notFound_returns400() throws Exception {
        mockMvc.perform(delete("/api/transactions/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }
}
