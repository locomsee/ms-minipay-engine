package com.dlight.payments.dto;

import java.util.UUID;

import com.dlight.payments.entity.PaymentStatus;

public record PaymentResponseDto(
        UUID paymentId,
        String transactionReference,
        PaymentStatus status
) {
}
