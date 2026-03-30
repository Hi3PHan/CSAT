package group2.CSAT.BMTT.algorithm;

/**
 * Galois Field GF(2^8) arithmetic for AES.
 *
 * AES uses the irreducible polynomial: x^8 + x^4 + x^3 + x + 1 (0x11b)
 * All operations are performed modulo this polynomial.
 *
 * Only basic arithmetic is used: XOR (addition), bit shifting, and comparison.
 * No external cryptography libraries are used.
 */
public final class GaloisField {

    private GaloisField() {
    }

    /**
     * Multiplies two values in GF(2^8).
     *
     * Uses the "Russian Peasant Multiplication" algorithm:
     * - Double (xtime): left shift by 1, then XOR with 0x1b if the high bit was
     * set.
     * - Halve multiplier, accumulate when its LSB is 1.
     *
     * @param a multiplier (scalar integer)
     * @param b value to multiply (as byte)
     * @return result of a * b in GF(2^8)
     */
    public static int multiply(int a, byte b) {
        int result = 0;
        // dòng này để chuyển byte sang int 
        int val = b & 0xFF;

        while (a > 0) {
            // Nếu bit thấp nhất của 'a' là 1, tích lũy giá trị hiện tại của 'val'
            if ((a & 1) != 0) {
                result ^= val;
            }

            // xtime: kiểm tra bit thứ 8 (MSB) TRƯỚC khi dịch
            // nếu bit cao nhất = 1, sau khi dịch sẽ tràn ra ngoài 8-bit → cần XOR khử
            boolean highBitSet = (val & 0x80) != 0;
            val <<= 1;       // dịch trái 1 bit (nhân đôi trong GF)
            val &= 0xFF;     // cắt về 8-bit, loại bỏ bit tràn ra ngoài
            if (highBitSet) {
                val ^= 0x1b; // XOR phần thấp của đa thức m(x) = x^4+x^3+x+1
            }

            // Halve a
            a >>= 1;
        }
        return result;
    }

    /**
     * Multiplies two bytes in GF(2^8).
     *
     * @param a first byte
     * @param b second byte
     * @return result of a * b in GF(2^8)
     */
    public static int multiply(byte a, byte b) {
        return multiply(a & 0xFF, b);
    }

    /**
     * Convenience: XOR two integer values (addition in GF(2^8)).
     */
    public static int add(int a, int b) {
        return a ^ b;
    }
}
