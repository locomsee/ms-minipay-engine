package com.dlight.payments.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.dlight.payments.AbstractIntegrationTest;
import com.dlight.payments.dto.LoginRequestDto;
import com.dlight.payments.dto.PaymentRequestDto;
import com.dlight.payments.dto.WebhookRequestDto;
import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    // Login rate limit is 5/min per IP; @BeforeEach logs in twice per test method, so each test
    // method needs its own isolated fake IP rather than one shared per-class value (static so
    // it persists across JUnit's per-method test instances, guaranteeing uniqueness regardless
    // of execution order or total test count).
    private static final AtomicInteger LOGIN_IP_SUFFIX = new AtomicInteger(20);
    private static final String WEBHOOK_FAKE_IP = "203.0.113.99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String fakeIp = "203.0.113." + LOGIN_IP_SUFFIX.incrementAndGet();
        adminToken = login("dlightadmin", "bJopnie@uu", fakeIp);
        userToken = login("dlightuser", "njffd@4@bhfd", fakeIp);
    }

    private String login(String username, String password, String fakeIp) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", fakeIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void initiatePayment_withoutToken_returns401() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(500), "254712345678", PaymentMethod.MPESA);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void initiatePayment_withLowAmount_resolvesAsSuccess() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(500), "254712345678", PaymentMethod.MPESA);

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void initiatePayment_withInvalidPhoneNumber_returns400() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(500), "0712345678", PaymentMethod.MPESA);

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentStatus_whenPaymentDoesNotExist_returns404() throws Exception {
        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentHistory_asAdmin_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void getPaymentHistory_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhook_onAlreadyTerminalPayment_isIdempotentNoOp() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(500), "254712345678", PaymentMethod.MPESA);
        String response = mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(response).get("paymentId").asText());

        // Webhook is an unauthenticated provider-callback simulation - no token needed.
        WebhookRequestDto webhook = new WebhookRequestDto(paymentId, PaymentStatus.FAILED);
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Forwarded-For", WEBHOOK_FAKE_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }
}
