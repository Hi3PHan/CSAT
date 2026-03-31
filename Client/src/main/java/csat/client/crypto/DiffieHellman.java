package csat.client.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DiffieHellman {
    // A safe 1024-bit prime (for demonstration) or RFC 3526 2048-bit MODP Group.
    // Let's use RFC 3526 2048-bit prime.
    private static final BigInteger P = new BigInteger(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
        "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
        "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
        "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
        "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
        "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
        "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
        "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
        "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
        "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger G = BigInteger.valueOf(2);

    private final BigInteger privateKey;
    private final BigInteger publicKey;

    public DiffieHellman() {
        SecureRandom random = new SecureRandom();
        // Generate a random private key between 2 and P-2
        this.privateKey = new BigInteger(2048, random).mod(P.subtract(BigInteger.valueOf(2))).add(BigInteger.valueOf(2));
        this.publicKey = G.modPow(this.privateKey, P);
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    public BigInteger calculateSharedSecret(BigInteger otherPublicKey) {
        return otherPublicKey.modPow(privateKey, P);
    }

    public byte[] getSharedSecretBytes(BigInteger otherPublicKey) {
        byte[] secretBytes = calculateSharedSecret(otherPublicKey).toByteArray();

        // Remove the leading zero byte if present (due to two's complement representation)
        // Ensure to process the length properly
        if (secretBytes.length > 256 && secretBytes[0] == 0) {
            byte[] trimmed = new byte[secretBytes.length - 1];
            System.arraycopy(secretBytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return secretBytes;
    }
}


