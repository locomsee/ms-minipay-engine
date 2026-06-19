package com.dlight.payments.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dlight.payments.entity.Payment;
import com.dlight.payments.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
