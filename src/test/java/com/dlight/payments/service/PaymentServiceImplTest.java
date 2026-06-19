package com.dlight.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.dlight.payments.dto.PaymentRequestDto;
import com.dlight.payments.dto.PaymentResponseDto;
import com.dlight.payments.dto.PaymentStatusResponseDto;
import com.dlight.payments.dto.WebhookRequestDto;
import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;
import com.dlight.payments.exception.PaymentNotFoundException;
import com.dlight.payments.integration.gateway.PaymentGatewayClient;
import com.dlight.payments.integration.gateway.PaymentGatewayResult;
import com.dlight.payments.mapper.PaymentMapper;
import com.dlight.payments.repository.PaymentRepository;
import com.dlight.payments.util.TransactionReferenceGenerator;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGatewayClient paymentGatewayClient;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID paymentId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
    }

    @Test
    void initiatePayment_whenGatewayApproves_savesSuccessAndNotifies() {
        when(referenceGenerator.generate()).thenReturn("TXN-20260101000000-ABCDEFGH");
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(500), "254712345678", PaymentMethod.MPESA);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayClient.processPayment(any())).thenReturn(new PaymentGatewayResult(PaymentStatus.SUCCESS, "ok"));
        when(paymentMapper.toResponseDto(any(Payment.class)))
                .thenReturn(new PaymentResponseDto(paymentId, "TXN-20260101000000-ABCDEFGH", PaymentStatus.SUCCESS));

        PaymentResponseDto response = paymentService.initiatePayment(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(notificationService, times(1)).notifyPaymentSuccess(any(Payment.class));
    }

    @Test
    void initiatePayment_whenGatewayDeclines_savesFailedAndDoesNotNotify() {
        when(referenceGenerator.generate()).thenReturn("TXN-20260101000000-ABCDEFGH");
        PaymentRequestDto request = new PaymentRequestDto(BigDecimal.valueOf(20000), "254712345678", PaymentMethod.MPESA);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayClient.processPayment(any())).thenReturn(new PaymentGatewayResult(PaymentStatus.FAILED, "declined"));
        when(paymentMapper.toResponseDto(any(Payment.class)))
                .thenReturn(new PaymentResponseDto(paymentId, "TXN-20260101000000-ABCDEFGH", PaymentStatus.FAILED));

        PaymentResponseDto response = paymentService.initiatePayment(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        verify(notificationService, never()).notifyPaymentSuccess(any(Payment.class));
    }

    @Test
    void getPaymentStatus_whenFound_returnsMappedDto() {
        Payment payment = buildPayment(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toStatusResponseDto(payment)).thenReturn(buildStatusDto(PaymentStatus.SUCCESS));

        PaymentStatusResponseDto result = paymentService.getPaymentStatus(paymentId);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void getPaymentStatus_whenNotFound_throws() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getPaymentHistory_withStatusFilter_usesFindByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findByStatus(eq(PaymentStatus.SUCCESS), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        paymentService.getPaymentHistory(PaymentStatus.SUCCESS, pageable);

        verify(paymentRepository).findByStatus(PaymentStatus.SUCCESS, pageable);
        verify(paymentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPaymentHistory_withoutStatusFilter_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findAll(pageable)).thenReturn(org.springframework.data.domain.Page.empty());

        paymentService.getPaymentHistory(null, pageable);

        verify(paymentRepository).findAll(pageable);
        verify(paymentRepository, never()).findByStatus(any(), any());
    }

    @Test
    void handleWebhook_whenPaymentPending_updatesStatusAndNotifies() {
        Payment payment = buildPayment(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toStatusResponseDto(any(Payment.class))).thenReturn(buildStatusDto(PaymentStatus.SUCCESS));

        WebhookRequestDto webhook = new WebhookRequestDto(paymentId, PaymentStatus.SUCCESS);
        PaymentStatusResponseDto result = paymentService.handleWebhook(webhook);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(notificationService, times(1)).notifyPaymentSuccess(any(Payment.class));
    }

    @Test
    void handleWebhook_whenPaymentAlreadyTerminal_isNoOp() {
        Payment payment = buildPayment(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toStatusResponseDto(payment)).thenReturn(buildStatusDto(PaymentStatus.SUCCESS));

        WebhookRequestDto webhook = new WebhookRequestDto(paymentId, PaymentStatus.FAILED);
        PaymentStatusResponseDto result = paymentService.handleWebhook(webhook);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).notifyPaymentSuccess(any(Payment.class));
    }

    private Payment buildPayment(PaymentStatus status) {
        return Payment.builder()
                .id(paymentId)
                .transactionReference("TXN-20260101000000-ABCDEFGH")
                .amount(BigDecimal.valueOf(500))
                .phoneNumber("254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private PaymentStatusResponseDto buildStatusDto(PaymentStatus status) {
        return new PaymentStatusResponseDto(paymentId, "TXN-20260101000000-ABCDEFGH", BigDecimal.valueOf(500),
                "254712345678", PaymentMethod.MPESA, status, Instant.now(), Instant.now());
    }
}
