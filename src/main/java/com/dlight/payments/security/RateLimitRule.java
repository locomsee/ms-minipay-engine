package com.dlight.payments.security;

import java.util.List;

import org.springframework.http.HttpMethod;

record RateLimitRule(HttpMethod method, String path, String configName, KeyStrategy keyStrategy) {

    enum KeyStrategy {
        IP,
        AUTHENTICATED_USER
    }

    static final List<RateLimitRule> DEFAULT_RULES = List.of(
            new RateLimitRule(HttpMethod.POST, "/api/auth/login", "login", KeyStrategy.IP),
            new RateLimitRule(HttpMethod.POST, "/api/payments/webhook", "webhook", KeyStrategy.IP),
            new RateLimitRule(HttpMethod.POST, "/api/payments", "paymentInitiation", KeyStrategy.AUTHENTICATED_USER)
    );
}
