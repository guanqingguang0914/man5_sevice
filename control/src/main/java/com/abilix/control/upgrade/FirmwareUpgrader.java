package com.abilix.control.upgrade;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class FirmwareUpgrader {

    public static final String UPGRADE_FILE = FileUtils.DATA_UPDATE + File.separator + "M5ControlUpdata.bin";
    public static final String UPGRADE_FILE2 = FileUtils.DATA_UPDATE + File.separator + "M5ControlUpdata2.bin";
    public static final byte TYPE_NORMAL = (byte) 0x00; // 通用类型
    public static final byte TYPE_C_STM32 = (byte) 0x01; // C系列 (VJC)
    public static final byte TYPE_M_STM32 = (byte) 0x02; // M系列
    public static final byte TYPE_H_STM32 = (byte) 0x03; // H系列
    public static final byte TYPE_F_STM32 = (byte) 0x04; // F系列
    public static final byte TYPE_C_MOTOR = (byte) 0x11; // C系列 (VJC)
    public static final byte TYPE_M_MOTOR = (byte) 0x12; // M系列
    public static final byte TYPE_H_MOTOR = (byte) 0x13; // H系列
    public static final byte TYPE_F_MOTOR = (byte) 0x14; // F系列

    public static final byte CMD_ONE = (byte) 0x11;
    public static final byte CMD_RESET = (byte) 0x01;
    public static final byte CMD_BOOT = (byte) 0x00;
    public static final byte SEND_REQUEST = (byte) 0x02;
    public static final byte SEND_ING = (byte) 0x03;
    public static final byte SEND_END = (byte) 0x04;

    public static final byte SEND_JUMP = (byte) 0x05;
    public static final byte QUERY_VER = (byte) 0x06;

    public static final byte RESPONSE_RESET = (byte) 0x01;
    public static final byte RESPONSE_REQUEST = (byte) 0x02;
    public static final byte RESPONSE_SEND_NG = (byte) 0x03;

    public static final byte RESPONSE_SEND_OK = (byte) 0x04;
    public static final byte RESPONSE_JUMP_OK = (byte) 0x05;
    public static final byte RESPONSE_VER_OK = (byte) 0x06;

    public static final byte RESPONSE_CMD_ONE = (byte) 0x0F;

    private static OutputStream mOutputStream;
    private static InputStream mInputStream;
    private byte[] arrs;

    public FirmwareUpgrader(OutputStream os, InputStream is) {
        this.mOutputStream = os;
        this.mInputStream = is;
    }

    public static synchronized boolean upgradeFirmware(byte type, String filePath) {
        byte[] reponseBuff = sendProtocol(type, CMD_ONE, SEND_JUMP, null, false);
        LogMgr.e("reponseBuff::" + Utils.bytesToString(reponseBuff));
        if (reponseBuff[6] == RESPONSE_JUMP_OK) {
            // 文件不存在
            LogMgr.d("upgrade file path::" + filePath);
            File file = new File(filePath);
            if (!file.exists()) {
                LogMgr.e("update file not exist");
                return false;
            }
            // 发送指令进入boot模式
            // sendProtocol(TYPE_C, CMD_ONE, CMD_BOOT, null,false);
            byte[] data = new byte[8];
            int len = (int) file.length();
            byte[] byte_fileLen = intToBytes(len);
            System.arraycopy(byte_fileLen, 0, data, 0, byte_fileLen.length);
            long value = Utils.getCRC32(filePath);
            LogMgr.e("CRC value::" + value);
            byte[] byte_value = longToBytes(value);
            System.arraycopy(byte_value, 0, data, 4, byte_value.length);
            byte[] responseBuff = sendProtocol(type, CMD_ONE, SEND_REQUEST, data, true);
            LogMgr.e("responseBuff::" + Utils.bytesToString(responseBuff));
            if (!(responseBuff[6] == RESPONSE_REQUEST)) {
                return false;
            }
            try {
                FileInputStream fos = new FileInputStream(filePath);
                byte[] b = new byte[2048];
                byte[] resBuff = new byte[20];
                resBuff[6] = RESPONSE_REQUEST;
                int buff_length = 0;
                while (((buff_length = fos.read(b)) != -1) & (resBuff[6] == RESPONSE_REQUEST)) {
                    if (buff_length < 2048) {
                        LogMgr.e("buff_length::" + buff_length);
                        b = Arrays.copyOfRange(b, 0, buff_length);
                    }
                    resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                    LogMgr.e("resBuff::" + Utils.bytesToString(resBuff));
                    int i = 0;
                    while (!(resBuff[6] == RESPONSE_REQUEST) & i < 5) {
                        LogMgr.e("send buff and try times::" + (i + 1));
                        Thread.sleep(10);
                        resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                        LogMgr.e("send data buff response::" + Utils.bytesToString(resBuff));
                        i++;
                        if (i >= 5) {
                            LogMgr.e("send buff NG and give up");
                            return false;
                        }
                    }
                }
                byte[] rBuff = sendProtocol(type, CMD_ONE, SEND_END, null, false);
                LogMgr.e("rBuff::" + Utils.bytesToString(rBuff));
                if (!(rBuff[6] == RESPONSE_SEND_OK)) {
                    LogMgr.e("send file NG");
                    return false;
                }
                LogMgr.e("send file OK");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        LogMgr.e("no correct response to reset cmd");
        return false;
    }

    // 得到版本号
    public synchronized static int queryStmVersion(byte type) {
        byte[] verBuff = sendProtocol(type, CMD_ONE, QUERY_VER, null, false);
        LogMgr.d("stm 32 query version response::" + Utils.bytesToString(verBuff));

        if (verBuff[6] == RESPONSE_VER_OK) {
            byte[] ver = new byte[4];
            ver[0] = verBuff[11];
            ver[1] = verBuff[12];
            ver[2] = verBuff[13];
            ver[3] = verBuff[14];
            int verValue = Utils.byteAray2IntLH(ver);
            LogMgr.d("STM version::" + verValue);
            return verValue;
        }
        return -1;
    }

    // 发送指令,返回的是sendBuff(sendbuff, isNeedMoreTime),读的指令
    private static byte[] sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data, boolean isNeedMoreTime) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[7];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[7 + data.length];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }

        return sendBuff(sendbuff, isNeedMoreTime);
    }

    // 协议封装： AA 55 len1 len2 type cmd1 cmd2 00 00 00 00 (data) check
    public static byte[] addProtocol(byte[] buff) {
        short len = (short) (buff.length + 5);
        byte[] sendbuff = new byte[len];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        sendbuff[3] = (byte) ((len - 4) & 0x00FF); // 长度: 从type到check
        sendbuff[2] = (byte) (((len - 4) >> 8) & 0x00FF);
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data

        byte check = 0x00; // 校验位
        for (int n = 0; n < len - 1; n++) {
            check += sendbuff[n];
        }
        sendbuff[len - 1] = (byte) (check & 0x00FF);
        return sendbuff;
    }

    private synchronized static byte[] sendBuff(byte[] b, boolean isNeedMoreTime) {
        return SP.request(b);
    }

    // /return temp;

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 将long数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] longToBytes(long value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 获得指定文件的byte数组
     */
    public static byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
