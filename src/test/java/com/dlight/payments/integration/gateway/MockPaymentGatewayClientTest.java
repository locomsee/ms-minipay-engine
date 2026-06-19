package com.dlight.payments.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;

class MockPaymentGatewayClientTest {

    @Test
    void deterministicMode_amountAtOrBelowThreshold_succeeds() {
        MockPaymentGatewayClient client = new MockPaymentGatewayClient("deterministic", BigDecimal.valueOf(10000), 0.8);

        PaymentGatewayResult result = client.processPayment(
                new PaymentGatewayRequest(BigDecimal.valueOf(10000), "254712345678", PaymentMethod.MPESA));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void deterministicMode_amountAboveThreshold_fails() {
        MockPaymentGatewayClient client = new MockPaymentGatewayClient("deterministic", BigDecimal.valueOf(10000), 0.8);

        PaymentGatewayResult result = client.processPayment(
                new PaymentGatewayRequest(BigDecimal.valueOf(10000.01), "254712345678", PaymentMethod.MPESA));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void randomMode_alwaysSucceeds_whenSuccessRateIsOne() {
        MockPaymentGatewayClient client = new MockPaymentGatewayClient("random", BigDecimal.valueOf(10000), 1.0);

        PaymentGatewayResult result = client.processPayment(
                new PaymentGatewayRequest(BigDecimal.valueOf(999999), "254712345678", PaymentMethod.MPESA));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void randomMode_alwaysFails_whenSuccessRateIsZero() {
        MockPaymentGatewayClient client = new MockPaymentGatewayClient("random", BigDecimal.valueOf(10000), 0.0);

        PaymentGatewayResult result = client.processPayment(
                new PaymentGatewayRequest(BigDecimal.valueOf(1), "254712345678", PaymentMethod.MPESA));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }
}
