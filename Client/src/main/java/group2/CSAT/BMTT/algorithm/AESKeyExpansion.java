package group2.CSAT.BMTT.algorithm;

public final class AESKeyExpansion {
    private AESKeyExpansion() {}

    public static byte[][] expand(byte[] key) {
        if (key == null || (key.length != 16 && key.length != 32))
            throw new IllegalArgumentException("Key must be 16 or 32 bytes. Got " + (key == null ? 0 : key.length));
        int nk = key.length / 4;
        int nr = nk + 6;
        int totalWords = 4 * (nr + 1);
        byte[] w = new byte[totalWords * 4];
        System.arraycopy(key, 0, w, 0, key.length);
        for (int i = nk; i < totalWords; i++) {
            byte[] temp = new byte[4];
            System.arraycopy(w, (i - 1) * 4, temp, 0, 4);
            if (i % nk == 0) {
                temp = rotWord(temp);
                temp = subWord(temp);
                temp[0] ^= (byte) AESTables.RCON[i / nk];
            } else if (nk > 6 && i % nk == 4) {
                temp = subWord(temp);
            }
            for (int j = 0; j < 4; j++)
                w[i * 4 + j] = (byte) (w[(i - nk) * 4 + j] ^ temp[j]);
        }
        byte[][] roundKeys = new byte[nr + 1][16];
        for (int i = 0; i <= nr; i++)
            System.arraycopy(w, i * 16, roundKeys[i], 0, 16);
        return roundKeys;
    }

    public static int getRounds(int keyLengthBytes) { return (keyLengthBytes / 4) + 6; }

    private static byte[] rotWord(byte[] word) {
        byte first = word[0]; word[0] = word[1]; word[1] = word[2]; word[2] = word[3]; word[3] = first;
        return word;
    }
    private static byte[] subWord(byte[] word) {
        for (int i = 0; i < 4; i++) word[i] = (byte) AESTables.SBOX[word[i] & 0xFF];
        return word;
    }
}
