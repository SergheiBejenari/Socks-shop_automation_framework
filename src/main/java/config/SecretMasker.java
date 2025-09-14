package config;

/**
 * Utility class for masking secret values in logs and debug output.
 * Implements masking strategy based on value length.
 */
public class SecretMasker {
    
    /**
     * Masks a secret value according to the masking strategy:
     * - empty → ""
     * - length 1-5 → "***"
     * - length ≥6 → show first 2 and last 2 chars: "ab****yz"
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        int length = value.length();
        
        if (length <= 5) {
            return "***";
        }
        
        // Show first 2 and last 2 characters with at least 4 stars in between
        String prefix = value.substring(0, 2);
        String suffix = value.substring(length - 2);
        int starsCount = Math.max(4, length - 4);
        String stars = "*".repeat(starsCount);
        
        return prefix + stars + suffix;
    }
    
    /**
     * Masks a value only if it's marked as a secret.
     */
    public static String maskIfSecret(String value, boolean isSecret) {
        return isSecret ? mask(value) : value;
    }
}
