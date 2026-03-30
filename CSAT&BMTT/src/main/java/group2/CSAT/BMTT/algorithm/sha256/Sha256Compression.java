package group2.CSAT.BMTT.algorithm.sha256;

final class Sha256Compression {

    private Sha256Compression() {
    }

    static void compressBlock(int[] hashState, int[] messageSchedule) {
        int a = hashState[0];
        int b = hashState[1];
        int c = hashState[2];
        int d = hashState[3];
        int e = hashState[4];
        int f = hashState[5];
        int g = hashState[6];
        int h = hashState[7];

        for (int t = 0; t < 64; t++) {
            int t1 = h
                + Sha256BitOps.sigma1Upper(e)
                + Sha256BitOps.ch(e, f, g)
                + Sha256Constants.K[t]
                + messageSchedule[t];
            int t2 = Sha256BitOps.sigma0Upper(a) + Sha256BitOps.maj(a, b, c);

            h = g;
            g = f;
            f = e;
            e = d + t1;
            d = c;
            c = b;
            b = a;
            a = t1 + t2;
        }

        hashState[0] += a;
        hashState[1] += b;
        hashState[2] += c;
        hashState[3] += d;
        hashState[4] += e;
        hashState[5] += f;
        hashState[6] += g;
        hashState[7] += h;
    }
}

