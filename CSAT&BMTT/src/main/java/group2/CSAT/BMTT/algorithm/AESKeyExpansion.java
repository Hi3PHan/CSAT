package group2.CSAT.BMTT.algorithm;

import group2.CSAT.BMTT.algorithm.AESTables;

/**
 * AES Key Expansion (FIPS 197).
 *
 * Supports:
 *   AES-128 : 16-byte key → 11 round keys (10 rounds)
 *   AES-256 : 32-byte key → 15 round keys (14 rounds)
 *
 * No external cryptography libraries are used.
 */
public final class AESKeyExpansion {

    private AESKeyExpansion() {
    }

    /**
     * Expands the given AES key (16 or 32 bytes) into round keys.
     *
     * Algorithm: FIPS 197 Section 5.2 – Key Expansion
     *   Nk = key length in 32-bit words (4 for AES-128, 8 for AES-256)
     *   Nr = Nk + 6 (number of rounds: 10 or 14)
     *   Produces 4*(Nr+1) words = (Nr+1) round keys of 16 bytes each
     *
     * @param key AES key: 16 bytes (AES-128) or 32 bytes (AES-256)
     * @return 2D array of round keys: [Nr+1][16]
     */
    public static byte[][] expand(byte[] key) {
        if (key == null || (key.length != 16 && key.length != 32)) {
            throw new IllegalArgumentException(
                "Key must be 16 bytes (AES-128) or 32 bytes (AES-256). Got " + (key == null ? 0 : key.length) + " bytes.");
        }

        int nk = key.length / 4;       // số từ trong key:  4 (AES-128) or 8 (AES-256)
        int nr = nk + 6;               // số vòng:      10 (AES-128) or 14 (AES-256)
        int totalWords = 4 * (nr + 1); // tổng số từ cần:    44 (AES-128) or 60 (AES-256)

        // mảng từ phẳng: w[i] là các byte i*4 .. i*4+3
        byte[] w = new byte[totalWords * 4];
        System.arraycopy(key, 0, w, 0, key.length);

        for (int i = nk; i < totalWords; i++) {
            byte[] temp = new byte[4];
            System.arraycopy(w, (i - 1) * 4, temp, 0, 4);

            if (i % nk == 0) {
                // RotWord → SubWord → XOR Rcon
                temp = rotWord(temp);
                temp = subWord(temp);
                temp[0] ^= (byte) AESTables.RCON[i / nk];
            } else if (nk > 6 && i % nk == 4) {
                // Extra SubWord for AES-256 only
                temp = subWord(temp);
            }

            for (int j = 0; j < 4; j++) {
                w[i * 4 + j] = (byte) (w[(i - nk) * 4 + j] ^ temp[j]);
            }
        }

        // Pack flat word array into [Nr+1] round keys of 16 bytes
        byte[][] roundKeys = new byte[nr + 1][16];
        for (int i = 0; i <= nr; i++) {
            System.arraycopy(w, i * 16, roundKeys[i], 0, 16);
        }
        return roundKeys;
    }

    /**
     * Returns the number of AES rounds for the given key length.
     *   16 bytes → 10 rounds (AES-128)
     *   32 bytes → 14 rounds (AES-256)
     */
    public static int getRounds(int keyLengthBytes) {
        return (keyLengthBytes / 4) + 6;
    }

    /**
     * RotWord: cyclically shifts a 4-byte word left by 1 byte.
     * [a0, a1, a2, a3] -> [a1, a2, a3, a0]
     */
    private static byte[] rotWord(byte[] word) {
        byte first = word[0];
        word[0] = word[1];
        word[1] = word[2];
        word[2] = word[3];
        word[3] = first;
        return word;
    }

    /**
     * SubWord: applies the AES S-Box to each byte in a 4-byte word.
     */
    private static byte[] subWord(byte[] word) {
        for (int i = 0; i < 4; i++) {
            word[i] = (byte) AESTables.SBOX[word[i] & 0xFF];
        }
        return word;
    }
}
