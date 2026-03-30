package group2.CSAT.BMTT.algorithm.sha256;

public final class Sha256Engine {

    private Sha256Engine() {
    }

    public static String hashHex(byte[] message) {
        byte[] padded = Sha256Padding.pad(message);
        int[] hashState = Sha256Constants.initialHash();

        int numBlocks = padded.length / 64;
        for (int i = 0; i < numBlocks; i++) {
            int[] words = Sha256MessageSchedule.fromBlock(padded, i);
            Sha256Compression.compressBlock(hashState, words);
        }

        return Sha256Hex.toHex(hashState);
    }
}

