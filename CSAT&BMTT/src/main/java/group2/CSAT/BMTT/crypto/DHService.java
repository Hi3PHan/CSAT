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
     * One-step DH Handshake: Receive client public key, generate server key, compute AES key immediately.
     */
    public DHResponse handshake(String clientPublicKeyHex) {
        DiffieHellman dh = new DiffieHellman();
        String sessionId = UUID.randomUUID().toString();
        
        BigInteger clientPubKey = parseClientPublicKey(clientPublicKeyHex);
        byte[] sharedSecretBytes = dh.getSharedSecretBytes(clientPubKey);

        // Derive AES-128 key using custom SHA-256
        String hashedHex = SHA256.hashHex(sharedSecretBytes);
        
        // Take the first 32 hex chars (16 bytes) for AES-128 session key
        byte[] aesKey = hexStringToByteArray(hashedHex.substring(0, 32));
        
        // Save to cache
        sessionKeys.put(sessionId, aesKey);
        
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

        BigInteger clientPubKey = parseClientPublicKey(clientPublicKeyHex);
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

    private BigInteger parseClientPublicKey(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Client public key is null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Client public key is empty");
        }
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Client public key is empty after removing prefix");
            }
            return new BigInteger(normalized, 16);
        }
        boolean hasHexLetter = normalized.matches(".*[a-fA-F].*");
        int radix = hasHexLetter ? 16 : 10;
        return new BigInteger(normalized, radix);
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
