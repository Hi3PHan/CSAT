package group2.CSAT.BMTT.algorithm;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES reference implementation using Java's built-in javax.crypto library.
 * Used ONLY for comparison/validation against the custom AES implementation.
 *
 * Mode: ECB (Electronic Codebook) – same as our scratch implementation (stateless per block).
 * Padding: NoPadding – operates on exact 16-byte blocks.
 */
public class AESReference {

    private final SecretKeySpec secretKey;

    /**
     * @param key 16-byte (AES-128) or 32-byte (AES-256) key
     */
    public AESReference(byte[] key) {
        this.secretKey = new SecretKeySpec(key, "AES");
    }

    /**
     * Encrypts a 16-byte block using Java's built-in AES/ECB.
     */
    public byte[] encrypt(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypts a 16-byte block using Java's built-in AES/ECB.
     */
    public byte[] decrypt(byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(ciphertext);
    }

    // ------------------------------------------------------------------ //
    //  Comparison runner
    // ------------------------------------------------------------------ //

    public static void main(String[] args) throws Exception {
        System.out.println("=== AES Comparison: Custom vs Java Built-in ===\n");

        // ---- AES-128 ----
        System.out.println("--- AES-128 (16-byte key) ---");
        compareAES(
            hexToBytes("000102030405060708090a0b0c0d0e0f"),
            hexToBytes("00112233445566778899aabbccddeeff")
        );

        // ---- AES-256 ----
        System.out.println("\n--- AES-256 (32-byte key) ---");
        compareAES(
            hexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"),
            hexToBytes("00112233445566778899aabbccddeeff")
        );
    }

    private static void compareAES(byte[] key, byte[] plaintext) throws Exception {
        // Custom implementation
        AES custom = new AES(key);
        byte[] customCipher    = custom.encrypt(plaintext);
        byte[] customDecrypted = custom.decrypt(customCipher);

        // Java built-in reference
        AESReference ref = new AESReference(key);
        byte[] refCipher    = ref.encrypt(plaintext);
        byte[] refDecrypted = ref.decrypt(refCipher);

        boolean encryptMatch = java.util.Arrays.equals(customCipher, refCipher);
        boolean decryptMatch = java.util.Arrays.equals(customDecrypted, refDecrypted);

        // Print key and plaintext outside the table (can be long)
        System.out.println("  Key (" + key.length * 8 + "-bit) : " + bytesToHex(key));
        System.out.println("  Plaintext        : " + bytesToHex(plaintext));
        System.out.println();

        String sep = "+-------------+----------------------------------+----------------------------------+--------+";
        System.out.println(sep);
        System.out.printf("| %-11s | %-32s | %-32s | %-6s |%n",
                "Operation", "Custom (from scratch)", "Java javax.crypto", "Match?");
        System.out.println(sep);
        System.out.printf("| %-11s | %-32s | %-32s | %-6s |%n",
                "Encrypted",
                bytesToHex(customCipher), bytesToHex(refCipher),
                encryptMatch ? "✓ YES" : "✗ NO");
        System.out.printf("| %-11s | %-32s | %-32s | %-6s |%n",
                "Decrypted",
                bytesToHex(customDecrypted), bytesToHex(refDecrypted),
                decryptMatch ? "✓ YES" : "✗ NO");
        System.out.println(sep);

        if (encryptMatch && decryptMatch) {
            System.out.println("  ✓ RESULT: Custom AES matches Java's built-in perfectly!\n");
        } else {
            System.out.println("  ✗ RESULT: Mismatch detected!\n");
        }
    }

    private static byte[] hexToBytes(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
