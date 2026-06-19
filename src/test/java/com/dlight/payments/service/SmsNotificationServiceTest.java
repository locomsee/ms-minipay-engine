package com.dlight.payments.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;
import com.dlight.payments.integration.sms.SmsProvider;

@ExtendWith(MockitoExtension.class)
class SmsNotificationServiceTest {

    @Mock
    private SmsProvider smsProvider;

    @InjectMocks
    private SmsNotificationService notificationService;

    @Test
    void notifyPaymentSuccess_sendsSmsWithTransactionReference() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .transactionReference("TXN-20260101000000-ABCDEFGH")
                .amount(BigDecimal.valueOf(500))
                .phoneNumber("254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(PaymentStatus.SUCCESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        notificationService.notifyPaymentSuccess(payment);

        verify(smsProvider).send(eq("254712345678"), contains("TXN-20260101000000-ABCDEFGH"));
    }
}
