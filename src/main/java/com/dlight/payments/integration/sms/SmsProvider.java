package com.dlight.payments.integration.sms;

public interface SmsProvider {

    void send(String phoneNumber, String message);
}
