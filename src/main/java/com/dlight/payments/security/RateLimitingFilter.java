package com.dlight.payments.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimiterRegistry rateLimiterRegistry;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterRegistry rateLimiterRegistry, ObjectMapper objectMapper) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = matchRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> key = resolveKey(request, rule);
        if (key.isEmpty()) {
        
            filterChain.doFilter(request, response);
            return;
        }

        
        RateLimiter limiter = rateLimiterRegistry.rateLimiter(rule.configName() + ":" + key.get(), rule.configName());

        if (limiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            SecurityResponseWriter.write(response, objectMapper, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded, try again later", request.getRequestURI());
        }
    }

    private RateLimitRule matchRule(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = request.getRequestURI();
        return RateLimitRule.DEFAULT_RULES.stream()
                .filter(rule -> rule.method() == method && rule.path().equals(path))
                .findFirst()
                .orElse(null);
    }

    private Optional<String> resolveKey(HttpServletRequest request, RateLimitRule rule) {
        if (rule.keyStrategy() == RateLimitRule.KeyStrategy.AUTHENTICATED_USER) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return Optional.ofNullable(authentication).map(Authentication::getName);
        }
        return Optional.of(resolveClientIp(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
