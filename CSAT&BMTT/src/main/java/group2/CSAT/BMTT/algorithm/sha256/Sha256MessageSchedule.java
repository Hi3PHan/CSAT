package group2.CSAT.BMTT.algorithm.sha256;

final class Sha256MessageSchedule {

    private Sha256MessageSchedule() {
    }

    static int[] fromBlock(byte[] padded, int blockIndex) {
        int[] words = new int[64];

        for (int t = 0; t < 16; t++) {
            // ta sẽ dịch từng ký tự trong padded đi theo từng byte để sau cùng xor lại với nhau sẽ ra đc w
            // chô &0xFF là để ép về số dương
            int index = (blockIndex * 64) + (t * 4);
            words[t] = ((padded[index] & 0xFF) << 24) | ((padded[index + 1] & 0xFF) << 16)
                | ((padded[index + 2] & 0xFF) << 8)
                | (padded[index + 3] & 0xFF);
        }

        for (int t = 16; t < 64; t++) {
            words[t] = Sha256BitOps.sigma1Lower(words[t - 2])
                + words[t - 7]
                + Sha256BitOps.sigma0Lower(words[t - 15])
                + words[t - 16];
        }

        return words;
    }
}

