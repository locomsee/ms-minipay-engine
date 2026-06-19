package com.dlight.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;

public record PaymentStatusResponseDto(
        UUID paymentId,
        String transactionReference,
        BigDecimal amount,
        String phoneNumber,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
