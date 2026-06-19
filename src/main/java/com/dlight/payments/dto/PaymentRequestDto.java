package com.dlight.payments.dto;

import java.math.BigDecimal;

import com.dlight.payments.entity.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequestDto(

        @Schema(example = "1000.00")
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        @Schema(example = "254712345678")
        @NotNull(message = "phoneNumber is required")
        @Pattern(regexp = "^254[17]\\d{8}$", message = "phoneNumber must be a valid Kenyan MSISDN, e.g. 254712345678")
        String phoneNumber,

        @Schema(example = "MPESA")
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod
) {
}
