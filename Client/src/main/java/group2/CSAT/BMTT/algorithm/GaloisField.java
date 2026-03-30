package group2.CSAT.BMTT.algorithm;

public final class GaloisField {
    private GaloisField() {}
    public static int multiply(int a, byte b) {
        int result = 0;
        int val = b & 0xFF;
        while (a > 0) {
            if ((a & 1) != 0) result ^= val;
            boolean highBitSet = (val & 0x80) != 0;
            val <<= 1;
            val &= 0xFF;
            if (highBitSet) val ^= 0x1b;
            a >>= 1;
        }
        return result;
    }
    public static int multiply(byte a, byte b) { return multiply(a & 0xFF, b); }
    public static int add(int a, int b) { return a ^ b; }
}
