package com.dlight.payments.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        RateLimiterRegistry registry = RateLimiterRegistry.of(Map.of(
                "login", config,
                "webhook", config,
                "paymentInitiation", config));

        filter = new RateLimitingFilter(registry, new ObjectMapper().registerModule(new JavaTimeModule()));
        filterChain = mock(FilterChain.class);
    }

    private HttpServletRequest mockLoginRequest(String forwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/api/auth/login");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        return request;
    }

    @Test
    void withinLimit_requestsPassThrough() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(mockLoginRequest("198.51.100.1"), response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void overLimit_thirdRequestGets429AndChainNeverCalledAgain() throws Exception {
        for (int i = 0; i < 2; i++) {
            filter.doFilter(mockLoginRequest("198.51.100.2"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(mockLoginRequest("198.51.100.2"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void distinctIps_getIndependentBudgets() throws Exception {
        for (int i = 0; i < 2; i++) {
            filter.doFilter(mockLoginRequest("198.51.100.3"), new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(mockLoginRequest("198.51.100.4"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    void multiHopForwardedFor_resolvesToFirstEntry() throws Exception {
        filter.doFilter(mockLoginRequest("198.51.100.5, 10.0.0.1, 10.0.0.2"), new MockHttpServletResponse(), filterChain);
        filter.doFilter(mockLoginRequest("198.51.100.5"), new MockHttpServletResponse(), filterChain);

        // Both requests resolve to the same key (198.51.100.5), so the budget of 2 is now spent.
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(mockLoginRequest("198.51.100.5"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void nonMatchingPath_bypassesFilterEntirely() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/api/payments");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
