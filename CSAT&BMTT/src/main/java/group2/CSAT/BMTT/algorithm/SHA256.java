package group2.CSAT.BMTT.algorithm;

import group2.CSAT.BMTT.algorithm.sha256.Sha256Engine;
import java.nio.charset.StandardCharsets;

/** Public API facade for the custom SHA-256 implementation. */
public class SHA256 {

    public static String hashHex(String message) {
        return hashHex(message.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashHex(byte[] message) {
        return Sha256Engine.hashHex(message);
    }
}

