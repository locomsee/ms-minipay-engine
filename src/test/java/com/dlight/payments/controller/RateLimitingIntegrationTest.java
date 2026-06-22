package com.dlight.payments.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.dlight.payments.AbstractIntegrationTest;
import com.dlight.payments.dto.LoginRequestDto;


@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingIntegrationTest extends AbstractIntegrationTest {

    private static final String FAKE_IP = "203.0.113.200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_exceedingConfiguredLimit_returns429() throws Exception {
        // minipay.resilience4j.ratelimiter.configs.login.limit-for-period = 5
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", FAKE_IP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequestDto("dlightadmin", "bJopnie@uu"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", FAKE_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("dlightadmin", "bJopnie@uu"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(429)))
                .andExpect(jsonPath("$.path", org.hamcrest.Matchers.is("/api/auth/login")));
    }
}
