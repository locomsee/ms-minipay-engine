package com.dlight.payments.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dlight.payments.dto.PaymentRequestDto;
import com.dlight.payments.dto.PaymentResponseDto;
import com.dlight.payments.dto.PaymentStatusResponseDto;
import com.dlight.payments.dto.WebhookRequestDto;
import com.dlight.payments.entity.PaymentStatus;
import com.dlight.payments.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payments", description = "Payment initiation, status, history and webhook simulation")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Initiate a payment", description = "Creates a payment and synchronously resolves it via the mock payment gateway")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> initiatePayment(@Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request));
    }

    @Operation(summary = "Get payment status by id")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentStatusResponseDto> getPaymentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(id));
    }

    @Operation(summary = "List transaction history", description = "Supports pagination, sorting and filtering by status")
    @GetMapping
    public ResponseEntity<Page<PaymentStatusResponseDto>> getPaymentHistory(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(status, pageable));
    }

    @Operation(summary = "Simulate a payment provider webhook callback",
            description = "Idempotent: calling this on a payment already in a terminal state (SUCCESS/FAILED) is a no-op")
    @PostMapping("/webhook")
    public ResponseEntity<PaymentStatusResponseDto> handleWebhook(@Valid @RequestBody WebhookRequestDto request) {
        return ResponseEntity.ok(paymentService.handleWebhook(request));
    }
}
