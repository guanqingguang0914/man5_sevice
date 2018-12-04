package com.abilix.control.uav;

public class DataTransform {
    public static String bytesToString(byte[] bytes, int begin, int end) {// byte转化为string
        int len = end - begin + 1;
        byte[] str = new byte[len];
        System.arraycopy(bytes, begin, str, 0, len);
        return new String(str);
    }

    public static int bytesToInt2h(byte[] bytes, int begin) {// 两个字节转化为int  高位在前低位在后
        return (int) (0x00ff & bytes[begin + 1]) | ((0x00ff & bytes[begin]) << 8);
    }

    public static short bytesToShort2h(byte[] bytes, int begin) {// 两个字节转化为short  高位在前低位在后
        return (short) (((short) 0xff & bytes[begin + 1]) | (((short) 0xff & bytes[begin]) << 8));
    }

    public static int bytesToInt3h(byte[] bytes, int begin) {// 三字节转换为int  高位在前低位在后
        return (int) (0x0000ff & bytes[begin + 2]) | ((0x0000ff & bytes[begin + 1]) << 8) | ((0x0000ff & bytes[begin]) << 16);
    }

    public static int bytesToInt4h(byte[] bytes, int begin) {// 四字节转换为int  高位在前低位在后
        return (int) ((int) 0xff & bytes[begin + 3]) | (((int) 0xff & bytes[begin + 2]) << 8) | (((int) 0xff & bytes[begin + 1]) << 16) | (((int) 0xff & bytes[begin]) << 24);
    }

    public static float bytesToFloat4h(byte[] bytes, int begin) {// 四字节转换为float 高位在前低位在后
        int floatInt = bytesToInt4h(bytes, begin);//先转换为int型，再转换为float
        return Float.intBitsToFloat(floatInt);
    }

    public static int bytesToInt1(byte[] bytes, int begin) {// 一个字节转int
        return (int) ((bytes[begin] & 0xff));
    }

    public static int bytesToInt2(byte[] bytes, int begin) {// 两个字节转化为int  低位在前高位在后
        return (int) (0x00ff & bytes[begin]) | ((0x00ff & bytes[begin + 1]) << 8);
    }

    public static int bytesToInt3(byte[] bytes, int begin) {// 三字节转换为int  低位在前高位在后
        return (int) (0x0000ff & bytes[begin]) | ((0x0000ff & bytes[begin + 1]) << 8) | ((0x0000ff & bytes[begin + 2]) << 16);
    }

    /*short 2字节、int 4字节、float 4字节、long 8字节。*/
    public static int bytesToInt4(byte[] bytes, int begin) {// 四字节转换为int  低位在前高位在后
        return (int) ((int) 0xff & bytes[begin]) | (((int) 0xff & bytes[begin + 1]) << 8) | (((int) 0xff & bytes[begin + 2]) << 16) | (((int) 0xff & bytes[begin + 3]) << 24);
    }

    public static short bytesToShort2(byte[] bytes, int begin) {// 两个字节转化为short  低位在前高位在后
        return (short) (((short) 0xff & bytes[begin]) | (((short) 0xff & bytes[begin + 1]) << 8));
    }

    public static float bytesToFloat4(byte[] bytes, int begin) {// 四字节转换为float 低位在前高位在后
        int floatInt = bytesToInt4(bytes, begin);//先转换为int型，再转换为float
        return Float.intBitsToFloat(floatInt);
    }

    public static long bytesToLong8(byte[] bytes, int begin) {// 8字节转换为float 低位在前高位在后
        long longdata = ((long) 0xff & bytes[begin]) | (((long) 0xff & bytes[begin + 1]) << 8)
                | (((long) 0xff & bytes[begin + 2]) << 16) | (((long) 0xff & bytes[begin + 3]) << 24)
                | (((long) 0xff & bytes[begin + 4]) << 32) | (((long) 0xff & bytes[begin + 5]) << 40)
                | (((long) 0xff & bytes[begin + 6]) << 48) | (((long) 0xff & bytes[begin + 7]) << 56);
        return longdata;
    }

    public static byte IntTobyte(int data, int allNum, int byteNum) {// int转换为字节      低位在前高位在后 , data数据，allNum转化成的字节个数，byteNum第几个字节
        if (allNum < 1 || byteNum < 1) return (byte) 0x00;
        int bytedata = 0;
        switch (allNum) {
            case 4:
                return (byte) (data >> (8 * (byteNum - 1)));
            case 3:
                return (byte) (data >> (8 * (byteNum - 1)));
            case 2:
                return (byte) (data >> (8 * (byteNum - 1)));
            case 1:
                return (byte) (data >> (8 * (byteNum - 1)));
            default:
                return (byte) 0x00;
        }
    }

    public static int FloatToInt(float fdata) {
        return Float.floatToIntBits(fdata);
    }

    public static byte[] FloatTobyte(float fdata) {// float转换为字节      高位在前低位在后 , data数据，allNum转化成的字节个数，byteNum第几个字节
        // 把float转换为byte[]
        int fbit = FloatToInt(fdata);
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }
        return b;
    }

    public static byte[] LongTobyte(long dataD, int model) {//高字节在前
        byte[] data = new byte[9];
        for (int i = 0; i < 8; i++) {
            data[i] = (byte) (dataD >> (8 * i));
        }
        data[8] = (byte) model;
        return data;
    }

    public static int XORcheckSend(byte[] buf) {//传参是完整报文,生成CRC
        int len = buf.length;
        if (len < 12) return -1;

        int crc = buf[0] & 0xff;
        for (int i = 1; i <= len - 2; i++) {
            crc = (crc + buf[i] & 0xff) & 0xff;
        }
        return crc;
    }

    public static boolean XORcheckAdd(byte[] buf) {//传参是完整报文，校验通过返回true
        int len = buf.length;
        if (len < 12) return false;
        int crc = buf[0] & 0xff;
        for (int i = 1; i <= len - 2; i++) {
            crc = (crc + buf[i] & 0xff) & 0xff;
        }
        if ((buf[len - 1] & 0xff) == crc) return true;
        else return false;
    }
}
