package com.mortgage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MortgageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mortgageCheck_feasibleRequest_returns200WithFeasibleTrue() throws Exception {
        String body = """
                {
                    "income": 100000,
                    "maturityPeriod": 30,
                    "loanValue": 300000,
                    "homeValue": 400000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feasible").value(true))
                .andExpect(jsonPath("$.monthlyCosts").isNumber());
    }

    @Test
    void mortgageCheck_loanExceedsIncome_returns200WithFeasibleFalse() throws Exception {
        String body = """
                {
                    "income": 50000,
                    "maturityPeriod": 30,
                    "loanValue": 300000,
                    "homeValue": 400000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feasible").value(false))
                .andExpect(jsonPath("$.monthlyCosts").value(0));
    }

    @Test
    void mortgageCheck_loanExceedsHomeValue_returns200WithFeasibleFalse() throws Exception {
        String body = """
                {
                    "income": 200000,
                    "maturityPeriod": 30,
                    "loanValue": 400000,
                    "homeValue": 350000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feasible").value(false));
    }

    @Test
    void mortgageCheck_unknownMaturityPeriod_returns422() throws Exception {
        String body = """
                {
                    "income": 100000,
                    "maturityPeriod": 99,
                    "loanValue": 300000,
                    "homeValue": 400000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void mortgageCheck_missingFields_returns400() throws Exception {
        String body = """
                {
                    "income": 100000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void mortgageCheck_negativeIncome_returns400() throws Exception {
        String body = """
                {
                    "income": -1000,
                    "maturityPeriod": 30,
                    "loanValue": 300000,
                    "homeValue": 400000
                }
                """;

        mockMvc.perform(post("/api/mortgage-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
