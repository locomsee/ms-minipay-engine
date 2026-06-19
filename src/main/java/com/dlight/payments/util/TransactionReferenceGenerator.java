package com.dlight.payments.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class TransactionReferenceGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_SUFFIX_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return "TXN-" + timestamp + "-" + randomSuffix();
    }

    private String randomSuffix() {
        StringBuilder builder = new StringBuilder(RANDOM_SUFFIX_LENGTH);
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            builder.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return builder.toString();
    }
}
