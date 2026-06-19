package com.dlight.payments.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.dlight.payments.AbstractIntegrationTest;
import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;
import com.dlight.payments.integration.sms.SmsProvider;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Exercises the real Resilience4j AOP proxy (plain Mockito unit tests bypass it),
 * so this needs a full Spring context.
 */
@SpringBootTest
class SmsNotificationServiceResilienceTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private SmsProvider smsProvider;

    @BeforeEach
    void resetCircuitBreaker() {
        // The "smsProvider" CircuitBreaker is a singleton bean shared across test methods in this
        // class - reset its sliding window so one test's failures can't tip the breaker toward
        // OPEN and block a later test's retry attempts before they reach the mock.
        circuitBreakerRegistry.circuitBreaker("smsProvider").reset();
    }

    @Test
    void notifyPaymentSuccess_retriesOnTransientFailureThenSucceeds() {
        doThrow(new RuntimeException("transient sms gateway error"))
                .doThrow(new RuntimeException("transient sms gateway error"))
                .doNothing()
                .when(smsProvider).send(any(), any());

        notificationService.notifyPaymentSuccess(buildPayment());

        verify(smsProvider, timeout(2000).times(3)).send(any(), any());
    }

    @Test
    void notifyPaymentSuccess_whenAlwaysFails_exhaustsConfiguredRetryAttempts() {
        doThrow(new RuntimeException("permanent sms gateway error")).when(smsProvider).send(any(), any());

        notificationService.notifyPaymentSuccess(buildPayment());

        // minipay.resilience4j.retry.instances.smsProvider.max-attempts = 3
        verify(smsProvider, timeout(2000).times(3)).send(any(), any());
    }

    private Payment buildPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .transactionReference("TXN-20260101000000-ABCDEFGH")
                .amount(BigDecimal.valueOf(500))
                .phoneNumber("254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(PaymentStatus.SUCCESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
