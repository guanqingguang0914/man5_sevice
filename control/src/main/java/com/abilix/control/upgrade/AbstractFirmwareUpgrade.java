package com.abilix.control.upgrade;

import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public abstract class AbstractFirmwareUpgrade implements IFirmwareUpgrade {
    public static final String UPGRADE_FILE = FileUtils.DATA_UPDATE + File.separator + "M5ControlUpdata.bin";
    public static final String UPGRADE_FILE2 = FileUtils.DATA_UPDATE + File.separator + "M5ControlUpdata2.bin";
    public static final byte TYPE_NORMAL = (byte) 0x00; // 通用类型
    public static final byte TYPE_C_STM32 = (byte) 0x01; // C系列 (VJC)
    public static final byte TYPE_M_STM32 = (byte) 0x02; // M系列
    public static final byte TYPE_H_STM32 = (byte) 0x03; // H系列
    public static final byte TYPE_F_STM32 = (byte) 0x04; // F系列
    public static final byte TYPE_CX_STM32 = (byte) 0x09; // C1系列
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

    public static final byte S_ADJUST_OUT_CMD_1 = (byte)0x11;
    public static final byte S_ADJUST_OUT_CMD_5_TEST = (byte)0x15;

    public static final byte SEND_JUMP = (byte) 0x05;
    public static final byte QUERY_VER = (byte) 0x06;
    public static final byte STM8_QUERT_VERSION = (byte)0x16;

    public static final byte RESPONSE_RESET = (byte) 0x01;
    public static final byte RESPONSE_REQUEST = (byte) 0x02;
    public static final byte RESPONSE_REQUEST_ELF = (byte) 0x11;
    public static final byte RESPONSE_REQUEST_EXIST= (byte) 0x01;
    public static final byte RESPONSE_SEND_NG = (byte) 0x03;

    public static final byte SEND_FILE_REQUEST = (byte) 0x11;

    public static final byte RESPONSE_SEND_OK = (byte) 0x04;
    public static final byte RESPONSE_JUMP_OK = (byte) 0x05;
    public static final byte RESPONSE_VER_OK = (byte) 0x06;
    public static final byte RESPONSE_STM8_VERSION_CMD_2 = (byte)0x0C;

    public static final byte RESPONSE_CMD_ONE = (byte) 0x0F;


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

    // 协议封装： AA 55 len1 len2 type cmd1 cmd2 00 00 00 00 (data) check
    public static byte[] addProtocol(byte[] buff) {
        short len = (short) (buff.length + 5);
        byte[] sendbuff = new byte[len];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        sendbuff[2] = (byte) (((len - 4) >> 8) & 0x00FF);
        sendbuff[3] = (byte) ((len - 4) & 0x00FF); // 长度: 从type到check
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data

        byte check = 0x00; // 校验位
        for (int n = 0; n < len - 1; n++) {
            check += sendbuff[n];
        }
        sendbuff[len - 1] = (byte) (check & 0x00FF);
        return sendbuff;
    }


    public synchronized boolean transFileToStm32(byte type, String filePath) {
        // 文件不存在
        File file = new File(filePath);
        if (!file.exists()) {
            LogMgr.e("elf file not exist");
            return false;
        }
        // 发送指令进入boot模式
        // sendProtocol(TYPE_C, CMD_ONE, CMD_BOOT, null,false);
        byte[] data = new byte[9];
        data[0]=0x01;
        int len = (int) file.length();
        // 把文件的长度变为4byte
        byte[] byte_fileLen = intToBytes(len);

        System.arraycopy(byte_fileLen, 0, data, 1, byte_fileLen.length);
        long value = Utils.getCRC32(filePath);
        // 得到校验码4byte
        LogMgr.e("CRC value::" + value);
/*        if (value==sendedFileCRC){//如果之前传过则 不再传
            LogMgr.d("已发送文件CRC："+sendedFileCRC+"  新文件CRC："+value);
            return true;
        }*/
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        byte[] byte_value = longToBytes(value);
        System.arraycopy(byte_value, 0, data, 5, byte_value.length);
        // 请求发送bin文件
        byte[] responseBuff = sendBuff(ProtocolBuilder.buildProtocol(type, CMD_ONE, SEND_FILE_REQUEST, data));
        if(responseBuff==null){
            LogMgr.e("请求发送bin文件，STM32返回值为空");
            return false;
        }
        LogMgr.e("responseBuff::" + Utils.bytesToString(responseBuff));
        // 收到后stm32回复02通知android继续发送
        if (!(responseBuff[6] == RESPONSE_REQUEST_ELF)) {
            return false;
        }
        if (responseBuff[6]==RESPONSE_REQUEST_ELF&&responseBuff.length>12&&responseBuff[11]==RESPONSE_REQUEST_EXIST){
            return true;
        }

        try {
            FileInputStream fos = new FileInputStream(filePath);
          byte[] b = new byte[1000];
           // byte[] b = new byte[200];
           byte[] resBuff = new byte[20];
            resBuff[6] = RESPONSE_REQUEST;
            int buff_length = 0;
            while (((buff_length = fos.read(b)) != -1) & (resBuff[6] == RESPONSE_REQUEST)) {
                //	if (buff_length < 2048) {
                if (buff_length < 1000) {
                    LogMgr.e("buff_length::" + buff_length);
                    b = Arrays.copyOfRange(b, 0, buff_length);
                }
              //  resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                    resBuff=sendBuff(ProtocolBuilder.buildProtocol(type, CMD_ONE, SEND_ING, b));
                if (resBuff==null){
                    return false;
                }
                LogMgr.e("resBuff::" + Utils.bytesToString(resBuff));
                int i = 0;
                while (!(resBuff[6] == RESPONSE_REQUEST) & i < 5) {
                    LogMgr.e("send buff and try times::" + (i + 1));
                    Thread.sleep(10);
                 //   resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                    resBuff=sendBuff(ProtocolBuilder.buildProtocol(type, CMD_ONE, SEND_ING, b));
                    LogMgr.e("send data buff response::" + Utils.bytesToString(resBuff));
                    i++;
                    if (i >= 5) {
                        LogMgr.e("send buff NG and give up");
                        return false;
                    }
                }
            }
            // 发送完成，android直接发送
           // byte[] rBuff = sendProtocol(type, CMD_ONE, SEND_END, null, false);
            byte[] rBuff = sendBuff(ProtocolBuilder.buildProtocol(type, CMD_ONE, SEND_END, null));
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

    private synchronized static byte[] sendBuff(byte[] b) {
        byte[] temp = new byte[256];
        try {
            LogMgr.e("send buff::" + Utils.bytesToString(b));
           return SP.request(b,5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogMgr.e("return stm32 reponse");
        return temp;
    }
}
