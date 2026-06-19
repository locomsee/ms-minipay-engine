package com.dlight.payments.integration.gateway;

import java.math.BigDecimal;

import com.dlight.payments.entity.PaymentMethod;

public record PaymentGatewayRequest(
        BigDecimal amount,
        String phoneNumber,
        PaymentMethod paymentMethod
) {
}
