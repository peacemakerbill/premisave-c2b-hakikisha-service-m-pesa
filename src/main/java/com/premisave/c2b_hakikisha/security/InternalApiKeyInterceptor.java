package com.premisave.c2b_hakikisha.security;

import com.premisave.c2b_hakikisha.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protects this service's own /internal/** endpoints with the shared X-API-Key
 * (the same INTERNAL_API_KEY used between the other Premisave services).
 * Deliberately separate from the Bearer token issued to Safaricom, which must never
 * grant access to bulk account data.
 */
@Component
public class InternalApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER = "X-API-Key";

    private final byte[] expected;

    public InternalApiKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY must not be blank");
        }
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String provided = request.getHeader(HEADER);
        if (provided == null || !MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or missing API key");
        }
        return true;
    }
}