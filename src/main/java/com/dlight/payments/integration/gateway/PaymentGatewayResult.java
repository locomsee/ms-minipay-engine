package com.dlight.payments.integration.gateway;

import com.dlight.payments.entity.PaymentStatus;

public record PaymentGatewayResult(
        PaymentStatus status,
        String providerMessage
) {
}
