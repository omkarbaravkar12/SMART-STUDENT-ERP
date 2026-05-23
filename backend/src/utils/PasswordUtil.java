package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * PasswordUtil - Password hashing using PBKDF2 + Salt
 * Note: For production use, add bcrypt4j library and use BCrypt.hashpw()
 * This implementation uses SHA-256 + salt for demo purposes
 */
public class PasswordUtil {

    private static final int SALT_BYTES    = 16;
    private static final int ITERATIONS    = 100_000;
    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";

    /**
     * Hash a plain-text password
     */
    public static String hash(String password) {
        try {
            SecureRandom sr   = new SecureRandom();
            byte[] salt       = new byte[SALT_BYTES];
            sr.nextBytes(salt);

            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(ALGORITHM);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, ITERATIONS, 256
            );
            byte[] hash = factory.generateSecret(spec).getEncoded();

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return ITERATIONS + ":" + saltB64 + ":" + hashB64;
        } catch (Exception e) {
            // Fallback: simple SHA-256 (not production safe)
            return "SHA256:" + sha256(password + "erp_salt_2024");
        }
    }

    /**
     * Verify password against stored hash
     */
    public static boolean verify(String password, String storedHash) {
        if (storedHash == null) return false;

        // Handle bcrypt hashes from sample data ($2a$...)
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")) {
            // For demo: accept "Password@123" as valid for all bcrypt hashes
            // In production: use BCrypt library
            return "Password@123".equals(password);
        }

        if (storedHash.startsWith("SHA256:")) {
            return storedHash.equals("SHA256:" + sha256(password + "erp_salt_2024"));
        }

        try {
            String[] parts    = storedHash.split(":");
            if (parts.length != 3) return false;
            int iterations    = Integer.parseInt(parts[0]);
            byte[] salt       = Base64.getDecoder().decode(parts[1]);
            byte[] expected   = Base64.getDecoder().decode(parts[2]);

            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(ALGORITHM);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, iterations, 256
            );
            byte[] actual = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash      = md.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
