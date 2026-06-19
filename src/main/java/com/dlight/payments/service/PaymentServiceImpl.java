package com.dlight.payments.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dlight.payments.dto.PaymentRequestDto;
import com.dlight.payments.dto.PaymentResponseDto;
import com.dlight.payments.dto.PaymentStatusResponseDto;
import com.dlight.payments.dto.WebhookRequestDto;
import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentStatus;
import com.dlight.payments.exception.PaymentNotFoundException;
import com.dlight.payments.integration.gateway.PaymentGatewayClient;
import com.dlight.payments.integration.gateway.PaymentGatewayRequest;
import com.dlight.payments.integration.gateway.PaymentGatewayResult;
import com.dlight.payments.mapper.PaymentMapper;
import com.dlight.payments.repository.PaymentRepository;
import com.dlight.payments.util.TransactionReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final NotificationService notificationService;
    private final PaymentMapper paymentMapper;
    private final TransactionReferenceGenerator referenceGenerator;

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) {
        Payment payment = Payment.builder()
                .transactionReference(referenceGenerator.generate())
                .amount(request.amount())
                .phoneNumber(request.phoneNumber())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);
        log.info("payment_initiated id={} amount={} reference={}",
                payment.getId(), payment.getAmount(), payment.getTransactionReference());

        PaymentGatewayResult result = paymentGatewayClient.processPayment(
                new PaymentGatewayRequest(payment.getAmount(), payment.getPhoneNumber(), payment.getPaymentMethod()));

        payment.setStatus(result.status());
        payment = paymentRepository.save(payment);
        log.info("payment_completed id={} status={}", payment.getId(), payment.getStatus());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            notificationService.notifyPaymentSuccess(payment);
        }

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public PaymentStatusResponseDto getPaymentStatus(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        return paymentMapper.toStatusResponseDto(payment);
    }

    @Override
    public Page<PaymentStatusResponseDto> getPaymentHistory(PaymentStatus status, Pageable pageable) {
        Page<Payment> page = status != null
                ? paymentRepository.findByStatus(status, pageable)
                : paymentRepository.findAll(pageable);
        return page.map(paymentMapper::toStatusResponseDto);
    }

    @Override
    @Transactional
    public PaymentStatusResponseDto handleWebhook(WebhookRequestDto request) {
        Payment payment = findPaymentOrThrow(request.paymentId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("webhook_ignored_already_terminal id={} status={}", payment.getId(), payment.getStatus());
            return paymentMapper.toStatusResponseDto(payment);
        }

        payment.setStatus(request.status());
        payment = paymentRepository.save(payment);
        log.info("payment_completed id={} status={} source=webhook", payment.getId(), payment.getStatus());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            notificationService.notifyPaymentSuccess(payment);
        }

        return paymentMapper.toStatusResponseDto(payment);
    }

    private Payment findPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
