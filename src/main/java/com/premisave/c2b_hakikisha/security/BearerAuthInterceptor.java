package com.premisave.c2b_hakikisha.security;

import com.premisave.c2b_hakikisha.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Requires "Authorization: Bearer <access_token>" on every protected endpoint. */
@Component
public class BearerAuthInterceptor implements HandlerInterceptor {

    private static final String PREFIX = "Bearer ";

    private final TokenService tokenService;

    public BearerAuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Missing or invalid access token");
        }
        String token = header.substring(PREFIX.length()).trim();
        if (!tokenService.isValid(token)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
        return true;
    }
}