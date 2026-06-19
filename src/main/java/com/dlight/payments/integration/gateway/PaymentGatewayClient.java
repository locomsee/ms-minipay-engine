package com.dlight.payments.integration.gateway;

public interface PaymentGatewayClient {

    PaymentGatewayResult processPayment(PaymentGatewayRequest request);
}
