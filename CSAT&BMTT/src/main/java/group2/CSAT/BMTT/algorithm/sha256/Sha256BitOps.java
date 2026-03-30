package group2.CSAT.BMTT.algorithm.sha256;

final class Sha256BitOps {

    private Sha256BitOps() {
    }

    // dịch phải có lặp
    static int rightRotate(int value, int bits) {
        return (value >>> bits) | (value << (32 - bits));
    }

    // hàm ch nếu x = 1 thì lấy y còn nếu x = 0 thfi lấy z
    static int ch(int x, int y, int z) {
        return (x & y) ^ (~x & z);
    }

    // hàm maj lấy bit nào xuất hiện nhiều hơn
    static int maj(int x, int y, int z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    // hàm sigma0Upper
    static int sigma0Upper(int x) {
        return rightRotate(x, 2) ^ rightRotate(x, 13) ^ rightRotate(x, 22);
    }

    // hàm sigma1Upper
    static int sigma1Upper(int x) {
        return rightRotate(x, 6) ^ rightRotate(x, 11) ^ rightRotate(x, 25);
    }

    static int sigma0Lower(int x) {
        return rightRotate(x, 7) ^ rightRotate(x, 18) ^ (x >>> 3);
    }

    static int sigma1Lower(int x) {
        return rightRotate(x, 17) ^ rightRotate(x, 19) ^ (x >>> 10);
    }
}
