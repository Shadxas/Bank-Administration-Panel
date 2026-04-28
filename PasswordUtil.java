import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

// hashes passwords using pbkdf2 so we dont store plaintext in the db
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    // takes a plaintext password and returns "salt:hash" for storing in db
    public static String hashPassword(String plaintext) {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(plaintext.toCharArray(), salt);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        return saltB64 + ":" + hashB64;
    }

    // checks if a plaintext password matches a stored salt:hash string
    public static boolean verifyPassword(String plaintext, String storedSaltAndHash) {
        if (plaintext == null || storedSaltAndHash == null)
            return false;

        String[] parts = storedSaltAndHash.split(":");
        if (parts.length != 2)
            return false;

        // pull out the salt, re-hash the attempt, and compare
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = pbkdf2(plaintext.toCharArray(), salt);

        return constantTimeEquals(expectedHash, actualHash);
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // the actual pbkdf2 call
    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 hashing failed", e);
        }
    }

    // constant time compare so hackers cant use timing to guess the hash
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
