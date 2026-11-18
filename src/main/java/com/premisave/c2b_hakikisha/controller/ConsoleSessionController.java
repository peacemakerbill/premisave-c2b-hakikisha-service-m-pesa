package com.premisave.c2b_hakikisha.controller;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.ConsoleSession;
import com.premisave.c2b_hakikisha.dto.TokenResponse;
import com.premisave.c2b_hakikisha.exception.AuthException;
import com.premisave.c2b_hakikisha.security.PlatformTokenVerifier;
import com.premisave.c2b_hakikisha.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * A lookup session for staff of the Premisave app. The app sends the person's own Premisave token. When it belongs to a
 * staff role, the service answers with the configured shortcode and an access token made from the configured credentials,
 * so the Hakikisha username and password are read from application.yml here and never typed in or sent anywhere.
 *
 * It sits outside /api/** and /internal/**, because those paths need an access token and this is how a person gets one.
 */
@RestController
public class ConsoleSessionController {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSessionController.class);
    private static final String PREFIX = "Bearer ";

    private final PlatformTokenVerifier verifier;
    private final TokenService tokenService;
    private final HakikishaProperties props;

    public ConsoleSessionController(PlatformTokenVerifier verifier, TokenService tokenService, HakikishaProperties props) {
        this.verifier = verifier;
        this.tokenService = tokenService;
        this.props = props;
    }

    @GetMapping("/console/v1/session")
    public ResponseEntity<ConsoleSession> session(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (!verifier.configured()) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Set JWT_SECRET to the value the Premisave services share to turn on sign in for the lookup");
        }
        if (authorization == null || !authorization.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Sign in to Premisave first");
        }
        String role = verifier.staffRole(authorization.substring(PREFIX.length()))
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "The name lookup is for Premisave staff"));
        TokenResponse token = tokenService.issue();
        log.info("Lookup session issued to a {} account", role);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ConsoleSession(props.shortcode(), token.accessToken(), token.expiresIn()));
    }
}
