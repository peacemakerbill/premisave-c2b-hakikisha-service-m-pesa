package com.premisave.c2b_hakikisha.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks a Premisave sign in token (HS256, signed with the JWT_SECRET that the auth, wallet and listing services share) and
 * says which role it carries. It lets staff who are signed in to the Premisave app get a lookup session without anyone
 * typing the Hakikisha username and password. The key is derived exactly as the other Premisave services do it, so a token
 * from the auth service verifies here too.
 */
@Component
public class PlatformTokenVerifier {

    private static final Pattern ALG = Pattern.compile("\"alg\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final byte[] key;
    private final Set<String> staffRoles;
    private final Clock clock;

    @Autowired
    public PlatformTokenVerifier(@Value("${platform.jwt.secret:}") String secret,
                                 @Value("${platform.jwt.staff-roles:ADMIN,FINANCE,OPERATIONS,SUPPORT}") List<String> staffRoles) {
        this(secret, staffRoles, Clock.systemUTC());
    }

    PlatformTokenVerifier(String secret, List<String> staffRoles, Clock clock) {
        this.key = deriveKey(secret == null ? "" : secret);
        this.staffRoles = staffRoles.stream()
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
    }

    /** True once a JWT secret is configured. Without one no token can be trusted, so the lookup session stays off. */
    public boolean configured() {
        return key.length > 0;
    }

    /** The staff role in a valid token, or empty when the token is missing, forged, expired or not a staff token. */
    public Optional<String> staffRole(String bearerToken) {
        if (!configured() || bearerToken == null) {
            return Optional.empty();
        }
        String[] parts = bearerToken.trim().split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            Base64.Decoder url = Base64.getUrlDecoder();
            String header = new String(url.decode(parts[0]), StandardCharsets.UTF_8);
            Matcher alg = ALG.matcher(header);
            if (!alg.find() || !"HS256".equals(alg.group(1))) {
                return Optional.empty();
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!MessageDigest.isEqual(expected, url.decode(parts[2]))) {
                return Optional.empty();
            }
            // The signature is good, so the payload is exactly what the auth service wrote. Two claims are read from it.
            String claims = new String(url.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher exp = EXP.matcher(claims);
            if (!exp.find() || Long.parseLong(exp.group(1)) * 1000L <= clock.millis()) {
                return Optional.empty();
            }
            String role = roleClaim(claims, "roles");
            if (role == null) {
                role = roleClaim(claims, "role");
            }
            if (role == null) {
                return Optional.empty();
            }
            role = role.trim().toUpperCase(Locale.ROOT);
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
            return staffRoles.contains(role) ? Optional.of(role) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** A role claim written as a plain string or as a list of strings (the first one counts). */
    private static String roleClaim(String claims, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*(?:\"([^\"]*)\"|\\[\\s*\"([^\"]*)\")").matcher(claims);
        if (!m.find()) {
            return null;
        }
        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    /** The auth service's own derivation: Base64 text cut or padded to 32 bytes, or the raw text when it is not Base64. */
    static byte[] deriveKey(String secret) {
        if (secret.isEmpty()) {
            return new byte[0];
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            return Arrays.copyOf(decoded, 32);
        } catch (IllegalArgumentException notBase64) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
