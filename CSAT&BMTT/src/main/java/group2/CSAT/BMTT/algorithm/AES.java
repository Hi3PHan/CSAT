package group2.CSAT.BMTT.algorithm;

import group2.CSAT.BMTT.algorithm.AESTransformations;
/**
 * AES (Advanced Encryption Standard) – main entry point.
 *
 * Supports:
 *   AES-128 : 16-byte (128-bit) key, 10 rounds
 *   AES-256 : 32-byte (256-bit) key, 14 rounds
 *
 * This class orchestrates encryption and decryption using separate modules:
 *  - AESTables           : S-Box, Inverse S-Box, Rcon lookup tables
 *  - GaloisField         : GF(2^8) arithmetic (XOR, bit shifts)
 *  - AESKeyExpansion     : round key schedule
 *  - AESTransformations  : SubBytes, ShiftRows, MixColumns, AddRoundKey
 *
 * No external cryptography libraries are used.
 */
public class AES {

    /** Round keys (11 for AES-128, 15 for AES-256). */
    private final byte[][] roundKeys;
    /** Number of rounds (10 for AES-128, 14 for AES-256). */
    private final int nr;

    /**
     * Creates a new AES instance.
     *
     * @param key 16-byte key (AES-128) or 32-byte key (AES-256)
     */
    public AES(byte[] key) {
        this.roundKeys = group2.CSAT.BMTT.algorithm.AESKeyExpansion.expand(key);
        this.nr = group2.CSAT.BMTT.algorithm.AESKeyExpansion.getRounds(key.length);
    }

    // ------------------------------------------------------------------ //
    //  Encryption
    // ------------------------------------------------------------------ //

    /**
     * Encrypts a single 128-bit plaintext block.
     *
     * @param plaintext 16-byte input
     * @return 16-byte ciphertext
     */
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null || plaintext.length != 16) {
            throw new IllegalArgumentException("Plaintext must be 128 bits (16 bytes).");
        }

        // biến đổi mảng 16 byte thành ma trận 4x4
        byte[][] state = bytesToState(plaintext);

        // Initial round: AddRoundKey only
        AESTransformations.addRoundKey(state, roundKeys[0]);

        // Main rounds 1 .. nr-1
        for (int i = 1; i < nr; i++) {
            AESTransformations.subBytes(state);
            AESTransformations.shiftRows(state);
            AESTransformations.mixColumns(state);
            AESTransformations.addRoundKey(state, roundKeys[i]);
        }

        // Final round: no MixColumns
        AESTransformations.subBytes(state);
        AESTransformations.shiftRows(state);
        AESTransformations.addRoundKey(state, roundKeys[nr]);

        return stateToBytes(state);
    }

    // ------------------------------------------------------------------ //
    //  Decryption
    // ------------------------------------------------------------------ //

    /**
     * Decrypts a single 128-bit ciphertext block.
     *
     * @param ciphertext 16-byte input
     * @return 16-byte plaintext
     */
    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length != 16) {
            throw new IllegalArgumentException("Ciphertext must be 128 bits (16 bytes).");
        }

        byte[][] state = bytesToState(ciphertext);

        // Reverse final round
        AESTransformations.addRoundKey(state, roundKeys[nr]);
        AESTransformations.invShiftRows(state);
        AESTransformations.invSubBytes(state);

        // Reverse main rounds nr-1 .. 1
        for (int i = nr - 1; i >= 1; i--) {
            AESTransformations.addRoundKey(state, roundKeys[i]);
            AESTransformations.invMixColumns(state);
            AESTransformations.invShiftRows(state);
            AESTransformations.invSubBytes(state);
        }

        // Reverse initial round
        AESTransformations.addRoundKey(state, roundKeys[0]);

        return stateToBytes(state);
    }

    // ------------------------------------------------------------------ //
    //  State conversion helpers
    // ------------------------------------------------------------------ //

    /**
     * Converts a flat 16-byte array to a 4x4 column-major state matrix.
     * bytes[c*4 + r] -> state[r][c]
     */
    private byte[][] bytesToState(byte[] bytes) {
        byte[][] state = new byte[4][4];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = bytes[c * 4 + r];
            }
        }
        return state;
    }

    /**
     * Converts a 4x4 column-major state matrix back to a flat 16-byte array.
     * state[r][c] -> bytes[c*4 + r]
     */
    private byte[] stateToBytes(byte[][] state) {
        byte[] bytes = new byte[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                bytes[c * 4 + r] = state[r][c];
            }
        }
        return bytes;
    }

    // ------------------------------------------------------------------ //
    //  Quick self-test using the NIST FIPS 197 test vector
    // ------------------------------------------------------------------ //

    public static void main(String[] args) {
        // NIST test vector
        byte[] key       = hexToBytes("000102030405060708090a0b0c0d0e0f");
        byte[] plaintext = hexToBytes("00112233445566778899aabbccddeeff");

        AES aes = new AES(key);

        byte[] cipher    = aes.encrypt(plaintext);
        byte[] recovered = aes.decrypt(cipher);

        System.out.println("Key:       " + bytesToHex(key));
        System.out.println("Plaintext: " + bytesToHex(plaintext));
        System.out.println("Encrypted: " + bytesToHex(cipher));
        System.out.println("Expected:  69c4e0d86a7b0430d8cdb78070b4c55a");
        System.out.println("Decrypted: " + bytesToHex(recovered));
        System.out.println("Match:     " + java.util.Arrays.equals(plaintext, recovered));
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
