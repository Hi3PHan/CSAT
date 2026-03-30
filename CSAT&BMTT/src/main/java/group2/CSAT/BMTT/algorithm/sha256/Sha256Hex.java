package group2.CSAT.BMTT.algorithm.sha256;

final class Sha256Hex {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256Hex() {
    }

    static String toHex(int[] hashState) {
        char[] chars = new char[64];
        int pos = 0;

        for (int value : hashState) {
            for (int shift = 28; shift >= 0; shift -= 4) {
                chars[pos++] = HEX[(value >>> shift) & 0x0F];
            }
        }

        return new String(chars);
    }
}

