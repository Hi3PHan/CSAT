package group2.CSAT.BMTT.algorithm.sha256;

final class Sha256Padding {

    private Sha256Padding() {
    }

    static byte[] pad(byte[] message) {
        // số byte của message gốc
        int originalLengthBytes = message.length;
        // độ dài theo bit của message
        long originalLengthBits = originalLengthBytes * 8L;

        int k = 0;
        // tính số bit cần thêm
        while ((originalLengthBytes + 1 + k) % 64 != 56) {
            k++;
        }
        // nếu byte vào là 100 thì k đầu ra sẽ là 19

        byte[] padded = new byte[originalLengthBytes + 1 + k + 8];
        // copy message vào padded
        System.arraycopy(message, 0, padded, 0, originalLengthBytes);
        // thêm bit 1 vào sau message
        padded[originalLengthBytes] = (byte) 0x80;

        for (int i = 0; i < 8; i++) {
            padded[padded.length - 1 - i] = (byte) (originalLengthBits >>> (i * 8));
        }

        return padded;
    }
}

