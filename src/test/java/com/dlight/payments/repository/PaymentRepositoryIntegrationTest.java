package com.dlight.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.dlight.payments.AbstractIntegrationTest;
import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentMethod;
import com.dlight.payments.entity.PaymentStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryIntegrationTest extends AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanDatabase() {
        // The container/database is shared with other (non-transactional) test classes in the
        // suite, so committed rows from them would otherwise leak into this test's exact counts.
        paymentRepository.deleteAll();
    }

    @Test
    void save_persistsPaymentWithGeneratedIdAndTimestamps() {
        Payment payment = Payment.builder()
                .transactionReference("TXN-20260101000000-ABCDEFGH")
                .amount(BigDecimal.valueOf(500))
                .phoneNumber("254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByStatus_returnsOnlyMatchingPayments() {
        paymentRepository.save(buildPayment("TXN-A", PaymentStatus.SUCCESS));
        paymentRepository.save(buildPayment("TXN-B", PaymentStatus.FAILED));
        paymentRepository.save(buildPayment("TXN-C", PaymentStatus.SUCCESS));

        Page<Payment> page = paymentRepository.findByStatus(PaymentStatus.SUCCESS, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);
    }

    private Payment buildPayment(String reference, PaymentStatus status) {
        return Payment.builder()
                .transactionReference(reference)
                .amount(BigDecimal.valueOf(100))
                .phoneNumber("254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(status)
                .build();
    }
}
