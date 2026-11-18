package com.premisave.c2b_hakikisha.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTokenVerifierTest {

    private static final String SECRET = "eW91ci12ZXJ5LXNlY3VyZS1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE=";
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final long LATER = NOW.plusSeconds(3600).getEpochSecond();
    private static final long EARLIER = NOW.minusSeconds(3600).getEpochSecond();

    private final PlatformTokenVerifier verifier =
            new PlatformTokenVerifier(SECRET, List.of("ADMIN", "FINANCE", "OPERATIONS", "SUPPORT"), Clock.fixed(NOW, ZoneOffset.UTC));

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String token(String secret, String alg, String payload) throws Exception {
        String head = b64(("{\"alg\":\"" + alg + "\",\"typ\":\"JWT\"}").getBytes(StandardCharsets.UTF_8));
        String body = b64(payload.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(PlatformTokenVerifier.deriveKey(secret), "HmacSHA256"));
        return head + "." + body + "." + b64(mac.doFinal((head + "." + body).getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void aStaffTokenFromTheAuthServiceIsAccepted() throws Exception {
        assertEquals(Optional.of("ADMIN"), verifier.staffRole(token(SECRET, "HS256", "{\"sub\":\"a@b.c\",\"roles\":\"ADMIN\",\"exp\":" + LATER + "}")));
        assertEquals(Optional.of("FINANCE"), verifier.staffRole(token(SECRET, "HS256", "{\"role\":\"ROLE_FINANCE\",\"exp\":" + LATER + "}")));
        assertEquals(Optional.of("SUPPORT"), verifier.staffRole(token(SECRET, "HS256", "{\"roles\":[\"support\"],\"exp\":" + LATER + "}")));
    }

    @Test
    void clientsExpiredForgedAndUnsignedTokensAreRefused() throws Exception {
        assertTrue(verifier.staffRole(token(SECRET, "HS256", "{\"roles\":\"CLIENT\",\"exp\":" + LATER + "}")).isEmpty());
        assertTrue(verifier.staffRole(token(SECRET, "HS256", "{\"roles\":\"ADMIN\",\"exp\":" + EARLIER + "}")).isEmpty());
        assertTrue(verifier.staffRole(token(SECRET, "HS256", "{\"roles\":\"ADMIN\"}")).isEmpty());
        assertTrue(verifier.staffRole(token("another-secret-another-secret-123", "HS256", "{\"roles\":\"ADMIN\",\"exp\":" + LATER + "}")).isEmpty());
        assertTrue(verifier.staffRole(token(SECRET, "none", "{\"roles\":\"ADMIN\",\"exp\":" + LATER + "}")).isEmpty());
        assertTrue(verifier.staffRole("a.b.c").isEmpty());
        assertTrue(verifier.staffRole("").isEmpty());
        assertTrue(verifier.staffRole(null).isEmpty());
    }

    @Test
    void withoutASecretNothingIsTrusted() throws Exception {
        PlatformTokenVerifier off = new PlatformTokenVerifier("", List.of("ADMIN"), Clock.fixed(NOW, ZoneOffset.UTC));
        assertFalse(off.configured());
        assertTrue(off.staffRole(token(SECRET, "HS256", "{\"roles\":\"ADMIN\",\"exp\":" + LATER + "}")).isEmpty());
    }
}
