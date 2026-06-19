package com.dlight.payments.dto;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
