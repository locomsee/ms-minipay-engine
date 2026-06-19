package com.dlight.payments.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dlight.payments.entity.Payment;
import com.dlight.payments.integration.sms.SmsProvider;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationService implements NotificationService {

    private final SmsProvider smsProvider;

    @Override
    @Async("notificationExecutor")
    @Retry(name = "smsProvider", fallbackMethod = "notifyFallback")
    @CircuitBreaker(name = "smsProvider")
    public void notifyPaymentSuccess(Payment payment) {
        String message = "Payment of %.2f successful. Ref: %s"
                .formatted(payment.getAmount(), payment.getTransactionReference());

        smsProvider.send(payment.getPhoneNumber(), message);
        log.info("notification_sent paymentId={} phoneNumber={}", payment.getId(), payment.getPhoneNumber());
    }

    private void notifyFallback(Payment payment, Throwable throwable) {
        log.error("notification_failed paymentId={} phoneNumber={}", payment.getId(), payment.getPhoneNumber(), throwable);
    }
}
