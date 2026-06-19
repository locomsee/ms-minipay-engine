package com.dlight.payments.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256", 3600000);

    @Test
    void generateToken_thenExtractUsernameAndRoles_roundTrips() {
        String token = jwtService.generateToken("admin",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_ADMIN");
    }

    @Test
    void isTokenValid_withGarbageToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isTokenValid_withTokenSignedByDifferentKey_returnsFalse() {
        JwtService otherService = new JwtService(
                "a-completely-different-secret-key-also-256-bits-long-here", 3600000);
        String token = otherService.generateToken("admin", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
