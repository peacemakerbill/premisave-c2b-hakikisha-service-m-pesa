package com.premisave.c2b_hakikisha.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.premisave.c2b_hakikisha.config.HakikishaProperties;
import com.premisave.c2b_hakikisha.dto.TokenResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    /** Minimal movable clock for expiry tests. */
    static class MovableClock extends Clock {
        Instant now = Instant.parse("2026-09-20T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final HakikishaProperties props = new HakikishaProperties(
            "600992",
            new HakikishaProperties.Auth("user", "pass", 3599),
            new HakikishaProperties.Sync(true, 5, true, 100, true, 10, 30));

    @Test
    void checksCredentials() {
        TokenService service = new TokenService(props);
        assertThat(service.credentialsValid("user", "pass")).isTrue();
        assertThat(service.credentialsValid("user", "wrong")).isFalse();
        assertThat(service.credentialsValid("wrong", "pass")).isFalse();
        assertThat(service.credentialsValid(null, null)).isFalse();
    }

    @Test
    void issuedTokenIsValidUntilItExpires() {
        MovableClock clock = new MovableClock();
        TokenService service = new TokenService(props, clock);

        TokenResponse token = service.issue();
        assertThat(token.expiresIn()).isEqualTo("3599");
        assertThat(service.isValid(token.accessToken())).isTrue();

        clock.now = clock.now.plusSeconds(3598);
        assertThat(service.isValid(token.accessToken())).isTrue();

        clock.now = clock.now.plusSeconds(2);
        assertThat(service.isValid(token.accessToken())).isFalse();
    }

    @Test
    void unknownTokenIsInvalid() {
        TokenService service = new TokenService(props);
        assertThat(service.isValid("nope")).isFalse();
        assertThat(service.isValid(null)).isFalse();
    }
}