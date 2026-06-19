package com.dlight.payments.service;

import com.dlight.payments.entity.Payment;

public interface NotificationService {

    void notifyPaymentSuccess(Payment payment);
}
