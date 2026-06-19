package com.dlight.payments.dto;

import java.util.UUID;

import com.dlight.payments.entity.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record WebhookRequestDto(

        @NotNull(message = "paymentId is required")
        UUID paymentId,

        @NotNull(message = "status is required")
        PaymentStatus status
) {
}
