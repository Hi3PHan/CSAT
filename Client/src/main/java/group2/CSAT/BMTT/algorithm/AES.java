package group2.CSAT.BMTT.algorithm;

/**
 * AES (Advanced Encryption Standard) – main entry point.
 * Copy từ Backend để Client dùng chung cùng thuật toán.
 * Supports AES-128 (16-byte key, 10 rounds) và AES-256 (32-byte key, 14 rounds).
 */
public class AES {

    private final byte[][] roundKeys;
    private final int nr;

    public AES(byte[] key) {
        this.roundKeys = AESKeyExpansion.expand(key);
        this.nr = AESKeyExpansion.getRounds(key.length);
    }

    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null || plaintext.length != 16)
            throw new IllegalArgumentException("Plaintext must be 128 bits (16 bytes).");
        byte[][] state = bytesToState(plaintext);
        AESTransformations.addRoundKey(state, roundKeys[0]);
        for (int i = 1; i < nr; i++) {
            AESTransformations.subBytes(state);
            AESTransformations.shiftRows(state);
            AESTransformations.mixColumns(state);
            AESTransformations.addRoundKey(state, roundKeys[i]);
        }
        AESTransformations.subBytes(state);
        AESTransformations.shiftRows(state);
        AESTransformations.addRoundKey(state, roundKeys[nr]);
        return stateToBytes(state);
    }

    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length != 16)
            throw new IllegalArgumentException("Ciphertext must be 128 bits (16 bytes).");
        byte[][] state = bytesToState(ciphertext);
        AESTransformations.addRoundKey(state, roundKeys[nr]);
        AESTransformations.invShiftRows(state);
        AESTransformations.invSubBytes(state);
        for (int i = nr - 1; i >= 1; i--) {
            AESTransformations.addRoundKey(state, roundKeys[i]);
            AESTransformations.invMixColumns(state);
            AESTransformations.invShiftRows(state);
            AESTransformations.invSubBytes(state);
        }
        AESTransformations.addRoundKey(state, roundKeys[0]);
        return stateToBytes(state);
    }

    private byte[][] bytesToState(byte[] bytes) {
        byte[][] state = new byte[4][4];
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                state[r][c] = bytes[c * 4 + r];
        return state;
    }

    private byte[] stateToBytes(byte[][] state) {
        byte[] bytes = new byte[16];
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                bytes[c * 4 + r] = state[r][c];
        return bytes;
    }
}
