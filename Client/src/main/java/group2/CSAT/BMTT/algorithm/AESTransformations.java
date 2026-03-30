package group2.CSAT.BMTT.algorithm;

public final class AESTransformations {
    private AESTransformations() {}
    public static void subBytes(byte[][] state) {
        for (int r=0;r<4;r++) for (int c=0;c<4;c++) state[r][c]=(byte)AESTables.SBOX[state[r][c]&0xFF];
    }
    public static void invSubBytes(byte[][] state) {
        for (int r=0;r<4;r++) for (int c=0;c<4;c++) state[r][c]=(byte)AESTables.INV_SBOX[state[r][c]&0xFF];
    }
    public static void shiftRows(byte[][] state) {
        byte t=state[1][0];state[1][0]=state[1][1];state[1][1]=state[1][2];state[1][2]=state[1][3];state[1][3]=t;
        t=state[2][0];byte t2=state[2][1];state[2][0]=state[2][2];state[2][1]=state[2][3];state[2][2]=t;state[2][3]=t2;
        t=state[3][3];state[3][3]=state[3][2];state[3][2]=state[3][1];state[3][1]=state[3][0];state[3][0]=t;
    }
    public static void invShiftRows(byte[][] state) {
        byte t=state[1][3];state[1][3]=state[1][2];state[1][2]=state[1][1];state[1][1]=state[1][0];state[1][0]=t;
        t=state[2][0];byte t2=state[2][1];state[2][0]=state[2][2];state[2][1]=state[2][3];state[2][2]=t;state[2][3]=t2;
        t=state[3][0];state[3][0]=state[3][1];state[3][1]=state[3][2];state[3][2]=state[3][3];state[3][3]=t;
    }
    public static void mixColumns(byte[][] state) {
        for (int c=0;c<4;c++) {
            byte a=state[0][c],b=state[1][c],d=state[2][c],e=state[3][c];
            state[0][c]=(byte)(GaloisField.multiply(2,a)^GaloisField.multiply(3,b)^GaloisField.multiply(1,d)^GaloisField.multiply(1,e));
            state[1][c]=(byte)(GaloisField.multiply(1,a)^GaloisField.multiply(2,b)^GaloisField.multiply(3,d)^GaloisField.multiply(1,e));
            state[2][c]=(byte)(GaloisField.multiply(1,a)^GaloisField.multiply(1,b)^GaloisField.multiply(2,d)^GaloisField.multiply(3,e));
            state[3][c]=(byte)(GaloisField.multiply(3,a)^GaloisField.multiply(1,b)^GaloisField.multiply(1,d)^GaloisField.multiply(2,e));
        }
    }
    public static void invMixColumns(byte[][] state) {
        for (int c=0;c<4;c++) {
            byte a=state[0][c],b=state[1][c],d=state[2][c],e=state[3][c];
            state[0][c]=(byte)(GaloisField.multiply(14,a)^GaloisField.multiply(11,b)^GaloisField.multiply(13,d)^GaloisField.multiply(9,e));
            state[1][c]=(byte)(GaloisField.multiply(9,a)^GaloisField.multiply(14,b)^GaloisField.multiply(11,d)^GaloisField.multiply(13,e));
            state[2][c]=(byte)(GaloisField.multiply(13,a)^GaloisField.multiply(9,b)^GaloisField.multiply(14,d)^GaloisField.multiply(11,e));
            state[3][c]=(byte)(GaloisField.multiply(11,a)^GaloisField.multiply(13,b)^GaloisField.multiply(9,d)^GaloisField.multiply(14,e));
        }
    }
    public static void addRoundKey(byte[][] state, byte[] roundKey) {
        for (int c=0;c<4;c++) for (int r=0;r<4;r++) state[r][c]^=roundKey[c*4+r];
    }
}
