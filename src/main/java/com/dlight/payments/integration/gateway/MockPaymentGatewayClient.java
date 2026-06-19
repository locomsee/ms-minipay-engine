package com.dlight.payments.integration.gateway;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dlight.payments.entity.PaymentStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    private final String mode;
    private final BigDecimal threshold;
    private final double randomSuccessRate;

    public MockPaymentGatewayClient(
            @Value("${minipay.gateway.mode:deterministic}") String mode,
            @Value("${minipay.gateway.threshold:10000}") BigDecimal threshold,
            @Value("${minipay.gateway.random-success-rate:0.8}") double randomSuccessRate) {
        this.mode = mode;
        this.threshold = threshold;
        this.randomSuccessRate = randomSuccessRate;
    }

    @Override
    public PaymentGatewayResult processPayment(PaymentGatewayRequest request) {
        boolean success = "random".equalsIgnoreCase(mode)
                ? ThreadLocalRandom.current().nextDouble() < randomSuccessRate
                : request.amount().compareTo(threshold) <= 0;

        PaymentStatus status = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        String message = success
                ? "Payment approved by mock gateway"
                : "Payment declined by mock gateway";

        log.info("gateway_processed mode={} amount={} status={}", mode, request.amount(), status);
        return new PaymentGatewayResult(status, message);
    }
}
