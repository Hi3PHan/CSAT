package group2.CSAT.BMTT.crypto;

import group2.CSAT.BMTT.algorithm.DiffieHellman;
import group2.CSAT.BMTT.algorithm.SHA256;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DHService {

    // Store SessionId -> AES Key
    private final Map<String, byte[]> sessionKeys = new ConcurrentHashMap<>();

    // Store in-progress DH server instances (SessionId -> DH instance)
    private final Map<String, DiffieHellman> dhInstances = new ConcurrentHashMap<>();

    /**
     * Thực hiện bắt tay Diffie-Hellman: Nhận Public Key của client, tạo mã phiên,
     * tính toán Shared Secret và sinh ra khóa AES dùng cho phiên làm việc đó.
     */
    public DHResponse handshake(String clientPublicKeyHex) {
        // 1. Khởi tạo đối tượng DH (Tự sinh cặp khóa Private/Public của Server)
        DiffieHellman dh = new DiffieHellman();

        // 2. Tạo ID phiên duy nhất (Session UUID) cho kết nối này
        String sessionId = UUID.randomUUID().toString();

        // 3. Chuyển đổi Public Key của Client từ chuỗi Hex sang số nguyên BigInteger
        BigInteger clientPubKey = new BigInteger(clientPublicKeyHex, 16);

        // 4. Tính toán Shared Secret (Bí mật dùng chung) từ khóa Server và khóa Client
        byte[] sharedSecretBytes = dh.getSharedSecretBytes(clientPubKey);

        // 5. Băm Shared Secret bằng thuật toán SHA-256 để tăng tính bảo mật
        String hashedHex = SHA256.hashHex(sharedSecretBytes);

        // 6. Lấy 32 ký tự Hex đầu tiên (tương đương 16 byte/128 bit) làm khóa AES-128
        byte[] aesKey = hexStringToByteArray(hashedHex.substring(0, 32));

        // 7. Lưu trữ cặp (SessionId -> Khóa AES) vào bộ nhớ tạm (Cache) để dùng cho các request sau
        sessionKeys.put(sessionId, aesKey);

        // 8. Trả về SessionId và Public Key của Server để Client cũng có thể tự tính ra khóa AES tương ứng
        return new DHResponse(sessionId, dh.getPublicKey().toString(16));
    }

    /**
     * Step 1: Initialize DH exchange, generate server public key and session id
     */
    public DHResponse initExchange() {
        DiffieHellman dh = new DiffieHellman();
        String sessionId = UUID.randomUUID().toString();
        dhInstances.put(sessionId, dh);

        return new DHResponse(sessionId, dh.getPublicKey().toString(16));
    }

    /**
     * Step 2: Compute shared secret with client's public key, derive AES key
     */
    public void computeSharedSecret(String sessionId, String clientPublicKeyHex) {
        DiffieHellman dh = dhInstances.remove(sessionId);
        if (dh == null) {
            throw new RuntimeException("Invalid or expired session id");
        }

        BigInteger clientPubKey = new BigInteger(clientPublicKeyHex, 16);
        byte[] sharedSecretBytes = dh.getSharedSecretBytes(clientPubKey);

        // Derive AES-128 key using custom SHA-256
        String hashedHex = SHA256.hashHex(sharedSecretBytes);

        // Take the first 32 hex chars (16 bytes) for AES-128 session key
        byte[] aesKey = hexStringToByteArray(hashedHex.substring(0, 32));

        // Save to cache
        sessionKeys.put(sessionId, aesKey);
    }

    public byte[] getSessionKey(String sessionId) {
        return sessionKeys.get(sessionId);
    }

    // Helper to convert hex to byte[] natively without external libs
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static class DHResponse {
        public String sessionId;
        public String serverPublicKey;

        public DHResponse(String sessionId, String serverPublicKey) {
            this.sessionId = sessionId;
            this.serverPublicKey = serverPublicKey;
        }
    }
}
