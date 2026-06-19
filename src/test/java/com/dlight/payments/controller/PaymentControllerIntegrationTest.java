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

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("dlightadmin", "bJopnie@uu");
        userToken = login("dlightuser", "njffd@4@bhfd");
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }
}
