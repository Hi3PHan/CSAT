package group2.CSAT.BMTT.algorithm;

/**
 * Lớp tự triển khai sinh Salt (Muối) dựa trên chiến lược Nonce 3 lớp.
 * Lớp 1: Thời gian hệ thống (Timestamp)
 * Lớp 2: Giá trị ngẫu nhiên (sử dụng thuật toán XORShift tự viết)
 * Lớp 3: Bộ đếm toàn cục (Counter)
 * 
 * Mã hóa kết quả dưới dạng Base36 để rút ngắn chuỗi.
 */
public class CustomSaltGenerator {

    private static long nonceCounter = 0;
    private static long seed = System.nanoTime();
    private static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";

    /**
     * Sinh chuỗi Salt ngẫu nhiên không lặp.
     * Định dạng: {timestamp}-{random1}-{random2}-{counter} (tất cả encoded Base36)
     */
    public static synchronized String generateSalt() {
        nonceCounter++;
        
        long timestamp = System.currentTimeMillis();
        long random1 = nextRandom();
        long random2 = nextRandom();
        
        return longToBase36(timestamp) + "-" +
               longToBase36(Math.abs(random1)) + "-" +
               longToBase36(Math.abs(random2)) + "-" +
               longToBase36(nonceCounter);
    }

    /**
     * Thuật toán XORShift64 đơn giản để sinh số ngẫu nhiên không dùng java.util.Random.
     */
    private static long nextRandom() {
        seed ^= (seed << 13);
        seed ^= (seed >> 7);
        seed ^= (seed << 17);
        return seed;
    }

    /**
     * Chuyển đổi số long sang chuỗi Base36 (tự viết).
     */
    private static String longToBase36(long value) {
        if (value == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(DIGITS.charAt((int) (value % 36)));
            value /= 36;
        }
        return sb.reverse().toString();
    }
}
