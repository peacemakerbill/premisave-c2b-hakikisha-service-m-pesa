package com.premisave.c2b_hakikisha.controller;

import com.premisave.c2b_hakikisha.dto.TokenResponse;
import com.premisave.c2b_hakikisha.exception.AuthException;
import com.premisave.c2b_hakikisha.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Step 1 of the C2B Hakikisha flow: Safaricom exchanges Basic-Auth credentials for an access token. */
@RestController
public class TokenController {

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private static final String BASIC_PREFIX = "Basic ";
    private static final String INVALID_CREDENTIALS = "Client credentials are invalid";

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/oauth2/v1/generate")
    public ResponseEntity<TokenResponse> generate(
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        String[] credentials = decodeBasic(authorization);
        if (credentials == null) {
            log.warn("Token request rejected: missing or malformed Basic Authorization header "
                    + "(in Postman, set Authorization type to Basic Auth)");
            throw new AuthException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (!tokenService.credentialsValid(credentials[0], credentials[1])) {
            log.warn("Token request rejected: {}", tokenService.describeMismatch(credentials[0], credentials[1]));
            throw new AuthException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (!"client_credentials".equals(grantType)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "grant_type must be client_credentials");
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(tokenService.issue());
    }

    /** @return {username, password}, or null if the header is absent or malformed */
    private static String[] decodeBasic(String header) {
        if (header == null || !header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return null;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()).trim()),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return null;
            }
            return new String[] {decoded.substring(0, colon), decoded.substring(colon + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}