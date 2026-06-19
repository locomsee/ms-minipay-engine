package com.dlight.payments.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dlight.payments.dto.PaymentRequestDto;
import com.dlight.payments.dto.PaymentResponseDto;
import com.dlight.payments.dto.PaymentStatusResponseDto;
import com.dlight.payments.dto.WebhookRequestDto;
import com.dlight.payments.entity.PaymentStatus;

public interface PaymentService {

    PaymentResponseDto initiatePayment(PaymentRequestDto request);

    PaymentStatusResponseDto getPaymentStatus(UUID paymentId);

    Page<PaymentStatusResponseDto> getPaymentHistory(PaymentStatus status, Pageable pageable);

    PaymentStatusResponseDto handleWebhook(WebhookRequestDto request);
}
