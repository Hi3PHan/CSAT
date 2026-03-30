package group2.CSAT.BMTT.crypto;

import group2.CSAT.BMTT.algorithm.AES;

import java.nio.charset.StandardCharsets;

/**
 * AESCipher — bọc ngoài AES.java để xử lý chuỗi có độ dài bất kỳ.
 *
 * AES.java chỉ nhận/trả đúng 16 bytes. Class này giải quyết bằng:
 *  1. PKCS#7 Padding : đệm thêm bytes để chia hết cho 16.
 *  2. ECB Block Mode : chia chuỗi thành các block 16 bytes, mã hóa từng block,
 *                     nối kết quả lại thành chuỗi Hex hoàn chỉnh.
 *
 * Key phải đúng 16 bytes (AES-128) hoặc 32 bytes (AES-256).
 */
public class AESCipher {

    private final AES aes;

    /**
     * @param key Mảng byte làm khóa — 16 bytes cho AES-128, 32 bytes cho AES-256.
     */
    public AESCipher(byte[] key) {
        this.aes = new AES(key);
    }

    // ───────────────────────────────────────────────────────────
    //  PUBLIC API
    // ───────────────────────────────────────────────────────────

    /**
     * Mã hóa một chuỗi văn bản bất kỳ thành chuỗi Hex.
     *
     * @param plaintext Chuỗi UTF-8 cần mã hóa (VD: JSON, tên, CCCD...)
     * @return Chuỗi Hex (VD: "A3F4CC...")
     */
    public String encrypt(String plaintext) {
        byte[] padded = pkcs7Pad(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] cipher = encryptBlocks(padded);
        return bytesToHex(cipher);
    }

    /**
     * Giải mã chuỗi Hex (do encrypt() tạo ra) trở về chuỗi văn bản gốc.
     *
     * @param hexCipher Chuỗi Hex nhận về từ mạng hoặc DB
     * @return Chuỗi UTF-8 gốc
     */
    public String decrypt(String hexCipher) {
        byte[] cipherBytes = hexToBytes(hexCipher);
        byte[] decrypted   = decryptBlocks(cipherBytes);
        byte[] unpadded    = pkcs7Unpad(decrypted);
        return new String(unpadded, StandardCharsets.UTF_8);
    }

    // ───────────────────────────────────────────────────────────
    //  PKCS#7 PADDING
    // ───────────────────────────────────────────────────────────

    /**
     * Thêm bytes đệm theo chuẩn PKCS#7.
     * Nếu dữ liệu đã chia hết cho 16, thêm 1 block 16 bytes đệm hoàn toàn.
     * Giá trị byte đệm = số byte được thêm vào.
     */
    private byte[] pkcs7Pad(byte[] data) {
        int blockSize  = 16;
        int padLen     = blockSize - (data.length % blockSize);
        byte[] padded  = new byte[data.length + padLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int i = data.length; i < padded.length; i++) {
            padded[i] = (byte) padLen;
        }
        return padded;
    }

    /**
     * Loại bỏ bytes đệm PKCS#7 sau khi giải mã.
     */
    private byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) return data;
        int padLen = data[data.length - 1] & 0xFF;
        if (padLen < 1 || padLen > 16) {
            throw new IllegalArgumentException("Invalid PKCS#7 padding value: " + padLen);
        }
        byte[] unpadded = new byte[data.length - padLen];
        System.arraycopy(data, 0, unpadded, 0, unpadded.length);
        return unpadded;
    }

    // ───────────────────────────────────────────────────────────
    //  ECB BLOCK MODE
    // ───────────────────────────────────────────────────────────

    /** Chia các block 16 bytes -> gọi AES.encrypt() từng block -> nối lại */
    private byte[] encryptBlocks(byte[] data) {
        int    numBlocks = data.length / 16;
        byte[] result    = new byte[data.length];
        byte[] block     = new byte[16];
        for (int i = 0; i < numBlocks; i++) {
            System.arraycopy(data, i * 16, block, 0, 16);
            byte[] encrypted = aes.encrypt(block);
            System.arraycopy(encrypted, 0, result, i * 16, 16);
        }
        return result;
    }

    /** Chia các block 16 bytes -> gọi AES.decrypt() từng block -> nối lại */
    private byte[] decryptBlocks(byte[] data) {
        if (data.length % 16 != 0) {
            throw new IllegalArgumentException("Ciphertext length must be a multiple of 16 bytes.");
        }
        int    numBlocks = data.length / 16;
        byte[] result    = new byte[data.length];
        byte[] block     = new byte[16];
        for (int i = 0; i < numBlocks; i++) {
            System.arraycopy(data, i * 16, block, 0, 16);
            byte[] decrypted = aes.decrypt(block);
            System.arraycopy(decrypted, 0, result, i * 16, 16);
        }
        return result;
    }

    // ───────────────────────────────────────────────────────────
    //  HEX UTILITIES
    // ───────────────────────────────────────────────────────────

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length.");
        }
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }

    // ───────────────────────────────────────────────────────────
    //  QUICK SELF-TEST
    // ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // AES-128 key: 16 bytes
        byte[] key = "MySecretKey12345".getBytes(StandardCharsets.UTF_8);
        AESCipher cipher = new AESCipher(key);

        String[] testCases = {
            "Hello World!",
            "Nguyen Van A",
            "0912345678",
            "079201234567",
            "{\"username\":\"admin\",\"password\":\"secret123\"}"
        };

        System.out.println("=".repeat(60));
        System.out.println("  AESCipher Round-Trip Test");
        System.out.println("=".repeat(60));
        for (String plain : testCases) {
            String enc = cipher.encrypt(plain);
            String dec = cipher.decrypt(enc);
            boolean ok = plain.equals(dec);
            System.out.printf("[%s] \"%s\"%n     -> %s%n", ok ? "OK" : "FAIL", plain, enc);
        }
    }
}
