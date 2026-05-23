package utils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * TokenUtil - Session token generation
 * Generates cryptographically random session tokens
 */
public class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 48;

    /**
     * Generate a secure session token embedding userId and role
     */
    public static String generateToken(String userId, String role) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String random   = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Include userId prefix for easy debugging (NOT security-critical)
        String prefix   = Base64.getUrlEncoder().withoutPadding()
            .encodeToString((userId + ":" + role).getBytes());
        return prefix + "." + random;
    }

    /**
     * Extract userId from token prefix (non-trusted, verify in DB)
     */
    public static String extractUserId(String token) {
        try {
            String prefix = token.split("\\.")[0];
            String decoded = new String(Base64.getUrlDecoder().decode(prefix));
            return decoded.split(":")[0];
        } catch (Exception e) {
            return null;
        }
    }
}
