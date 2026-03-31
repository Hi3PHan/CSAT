package csat.client.crypto;

import group2.CSAT.BMTT.algorithm.AES;
import java.nio.charset.StandardCharsets;

/**
 * AESCipher — Bọc ngoài AES.java để xử lý chuỗi có độ dài bất kỳ.
 * Dùng PKCS#7 Padding + ECB Block Mode (khớp với backend CryptoFilter).
 */
public class AESCipher {
    private final AES aes;

    public AESCipher(byte[] key) { this.aes = new AES(key); }

    public String encrypt(String plaintext) {
        byte[] padded = pkcs7Pad(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] cipher = encryptBlocksECB(padded);
        return bytesToHex(cipher);
    }

    public String decrypt(String hexCipher) {
        byte[] cipherBytes = hexToBytes(hexCipher);
        byte[] decrypted   = decryptBlocksECB(cipherBytes);
        byte[] unpadded    = pkcs7Unpad(decrypted);
        return new String(unpadded, StandardCharsets.UTF_8);
    }

    private byte[] pkcs7Pad(byte[] data) {
        int padLen = 16 - (data.length % 16);
        byte[] padded = new byte[data.length + padLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int i = data.length; i < padded.length; i++) padded[i] = (byte) padLen;
        return padded;
    }

    private byte[] pkcs7Unpad(byte[] data) {
        int padLen = data[data.length - 1] & 0xFF;
        byte[] r = new byte[data.length - padLen];
        System.arraycopy(data, 0, r, 0, r.length);
        return r;
    }

    private byte[] encryptBlocksECB(byte[] data) {
        byte[] result = new byte[data.length];
        byte[] block = new byte[16];
        for (int i = 0; i < data.length / 16; i++) {
            System.arraycopy(data, i * 16, block, 0, 16);
            byte[] enc = aes.encrypt(block);
            System.arraycopy(enc, 0, result, i * 16, 16);
        }
        return result;
    }

    private byte[] decryptBlocksECB(byte[] data) {
        if (data.length % 16 != 0) {
            throw new IllegalArgumentException("Ciphertext length must be a multiple of 16 bytes.");
        }
        byte[] result = new byte[data.length];
        byte[] block = new byte[16];
        for (int i = 0; i < data.length / 16; i++) {
            System.arraycopy(data, i * 16, block, 0, 16);
            byte[] dec = aes.decrypt(block);
            System.arraycopy(dec, 0, result, i * 16, 16);
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return data;
    }
}
