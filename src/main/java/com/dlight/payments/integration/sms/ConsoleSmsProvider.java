package com.dlight.payments.integration.sms;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ConsoleSmsProvider implements SmsProvider {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("Sending SMS to {}", phoneNumber);
        log.info("{}", message);
    }
}
