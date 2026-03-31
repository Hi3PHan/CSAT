package group2.CSAT.BMTT.algorithm;

/**
 * AES Round Transformations.
 *
 * Implements the four core AES transformations and their inverses:
 *   - SubBytes / InvSubBytes
 *   - ShiftRows / InvShiftRows
 *   - MixColumns / InvMixColumns
 *   - AddRoundKey
 *
 * All operations use basic bitwise arithmetic (XOR, shifts) via GaloisField.
 * No external cryptography libraries are used.
 */
public final class AESTransformations {

    private AESTransformations() {}

    // ------------------------------------------------------------------ //
    //  SubBytes / InvSubBytes
    // ------------------------------------------------------------------ //

    /**
     * SubBytes: replaces each byte in the state with its S-Box value.
     */
    public static void subBytes(byte[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] = (byte) AESTables.SBOX[state[r][c] & 0xFF];
            }
        }
    }

    /**
     * InvSubBytes: replaces each byte using the Inverse S-Box (for decryption).
     */
    public static void invSubBytes(byte[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] = (byte) AESTables.INV_SBOX[state[r][c] & 0xFF];
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  ShiftRows / InvShiftRows
    // ------------------------------------------------------------------ //

    /**
     * ShiftRows: cyclically shifts each row to the left.
     *  Row 0: no shift
     *  Row 1: shift left 1
     *  Row 2: shift left 2
     *  Row 3: shift left 3
     */
    public static void shiftRows(byte[][] state) {
        // Row 1: shift left by 1
        byte t = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = t;

        // Row 2: shift left by 2
        t = state[2][0];
        byte t2 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = t;
        state[2][3] = t2;

        // Row 3: shift left by 3 (same as shift right by 1)
        t = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = state[3][0];
        state[3][0] = t;
    }

    /**
     * InvShiftRows: cyclically shifts each row to the right (inverse of ShiftRows).
     *  Row 0: no shift
     *  Row 1: shift right 1
     *  Row 2: shift right 2
     *  Row 3: shift right 3
     */
    public static void invShiftRows(byte[][] state) {
        // Row 1: shift right by 1
        byte t = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = t;

        // Row 2: shift right by 2
        t = state[2][0];
        byte t2 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = t;
        state[2][3] = t2;

        // Row 3: shift right by 3 (same as shift left by 1)
        t = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = state[3][3];
        state[3][3] = t;
    }

    // ------------------------------------------------------------------ //
    //  MixColumns / InvMixColumns
    // ------------------------------------------------------------------ //

    /**
     * MixColumns: mixes each column using GF(2^8) polynomial multiplication.
     *
     * Matrix used (encryption):
     * [ 2 3 1 1 ]
     * [ 1 2 3 1 ]
     * [ 1 1 2 3 ]
     * [ 3 1 1 2 ]
     */
    public static void mixColumns(byte[][] state) {
        for (int c = 0; c < 4; c++) {
            byte a = state[0][c];
            byte b = state[1][c];
            byte d = state[2][c];
            byte e = state[3][c];

            state[0][c] = (byte) (GaloisField.multiply(2, a) ^ GaloisField.multiply(3, b) ^ GaloisField.multiply(1, d) ^ GaloisField.multiply(1, e));
            state[1][c] = (byte) (GaloisField.multiply(1, a) ^ GaloisField.multiply(2, b) ^ GaloisField.multiply(3, d) ^ GaloisField.multiply(1, e));
            state[2][c] = (byte) (GaloisField.multiply(1, a) ^ GaloisField.multiply(1, b) ^ GaloisField.multiply(2, d) ^ GaloisField.multiply(3, e));
            state[3][c] = (byte) (GaloisField.multiply(3, a) ^ GaloisField.multiply(1, b) ^ GaloisField.multiply(1, d) ^ GaloisField.multiply(2, e));
        }
    }

    /**
     * InvMixColumns: inverse of MixColumns for decryption.
     *
     * Matrix used (decryption):
     * [ 14  11  13   9 ]
     * [  9  14  11  13 ]
     * [ 13   9  14  11 ]
     * [ 11  13   9  14 ]
     */
    public static void invMixColumns(byte[][] state) {
        for (int c = 0; c < 4; c++) {
            byte a = state[0][c];
            byte b = state[1][c];
            byte d = state[2][c];
            byte e = state[3][c];

            state[0][c] = (byte) (GaloisField.multiply(14, a) ^ GaloisField.multiply(11, b) ^ GaloisField.multiply(13, d) ^ GaloisField.multiply(9, e));
            state[1][c] = (byte) (GaloisField.multiply(9,  a) ^ GaloisField.multiply(14, b) ^ GaloisField.multiply(11, d) ^ GaloisField.multiply(13, e));
            state[2][c] = (byte) (GaloisField.multiply(13, a) ^ GaloisField.multiply(9,  b) ^ GaloisField.multiply(14, d) ^ GaloisField.multiply(11, e));
            state[3][c] = (byte) (GaloisField.multiply(11, a) ^ GaloisField.multiply(13, b) ^ GaloisField.multiply(9,  d) ^ GaloisField.multiply(14, e));
        }
    }

    // ------------------------------------------------------------------ //
    //  AddRoundKey
    // ------------------------------------------------------------------ //

    /**
     * AddRoundKey: XORs each byte of the state with the corresponding byte
     * of the round key. Same operation for both encryption and decryption.
     *
     * @param state    current 4x4 state matrix
     * @param roundKey 16-byte round key (stored column-major: [col*4 + row])
     */
    public static void addRoundKey(byte[][] state, byte[] roundKey) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] ^= roundKey[c * 4 + r];
            }
        }
    }
}
