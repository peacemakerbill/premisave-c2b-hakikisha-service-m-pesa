package com.premisave.c2b_hakikisha.security;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.TokenResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Validates the partner's client credentials and issues / checks opaque access tokens.
 * Tokens live in memory only: a restart invalidates them and Safaricom simply requests a new one.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final HakikishaProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    @Autowired
    public TokenService(HakikishaProperties props) {
        this(props, Clock.systemUTC());
    }

    TokenService(HakikishaProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    /** Constant-time comparison of both fields; never short-circuits on the username. */
    public boolean credentialsValid(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        boolean userOk = MessageDigest.isEqual(
                username.getBytes(StandardCharsets.UTF_8),
                props.auth().username().getBytes(StandardCharsets.UTF_8));
        boolean passOk = MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8),
                props.auth().password().getBytes(StandardCharsets.UTF_8));
        return userOk & passOk;
    }

    /**
     * Says WHICH credential failed, for server-side logs only (never sent to the caller).
     * Lengths are logged at DEBUG to reveal stray spaces or quotes without exposing the secrets.
     */
    public String describeMismatch(String username, String password) {
        String expectedUser = props.auth().username();
        String expectedPass = props.auth().password();
        boolean userBad = username == null || !expectedUser.equals(username);
        boolean passBad = password == null || !expectedPass.equals(password);
        log.debug("Credential lengths - username received={} expected={}, password received={} expected={}",
                username == null ? 0 : username.length(), expectedUser.length(),
                password == null ? 0 : password.length(), expectedPass.length());
        if (userBad && passBad) {
            return "username and password do not match the configured values";
        }
        if (userBad) {
            return "username does not match HAKIKISHA_USERNAME";
        }
        return "password does not match HAKIKISHA_PASSWORD";
    }

    public TokenResponse issue() {
        Instant now = clock.instant();
        tokens.values().removeIf(expiry -> !expiry.isAfter(now));

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        long ttl = props.auth().tokenTtlSeconds();
        tokens.put(token, now.plusSeconds(ttl));
        return new TokenResponse(token, String.valueOf(ttl));
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiry = tokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (!expiry.isAfter(clock.instant())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}