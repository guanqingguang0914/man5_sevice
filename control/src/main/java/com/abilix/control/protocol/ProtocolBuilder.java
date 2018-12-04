package com.abilix.control.protocol;

import com.abilix.control.sp.SP;

public class ProtocolBuilder {
    // 命令字

    // 通用命令字
    // 获取电池电压
    public static final byte[] CMD_BATTERY = {(byte) 0x11, (byte) 0x0C};
    // 获取头部触摸
    public static final byte[] CMD_HEADTOUCH = {(byte) 0xA3, (byte) 0x6C};
    // 陀螺仪
    public static final byte[] CMD_GYRO = {(byte) 0xA4, (byte) 0x01};
    // 扬声器
    public static final byte[] CMD_SOUD = {(byte) 0xA4, (byte) 0x04};
    // 读取机器人子类型
    public static final byte[] CMD_GET_CHILD_ROBOT_TYPE = {(byte) 0x11, (byte) 0x09};
    // M1电机控制
    public static final byte[] CMD_M1_MOTOR_ = {(byte) 0xA6, (byte) 0x3B};
    //STM32重置清零
    public static final byte[] CMD_RESET={(byte)0x11,(byte)0x0B};
   //数据动作初始化
    public static final byte[] CMD_DATA_RESET={(byte)0xA3,(byte)0x45};
    //STM32休息
    public static final byte[] CMD_SLEEP={(byte)0x11,(byte)0x08};    //C5电机开环控制
    public static final byte[] CMD_C5MOTOR={(byte)0xA3,(byte)0x03};

    public static final byte[] CMD_FILE_EXCUTE={(byte)0x11,(byte)0x12};

    //H3、H4 Led灯控制
    public static final byte[] CMD_H3H4_LED={(byte)0xA3,(byte)0x74};

    public static final byte EXECUTE_ERROR=(byte)0x03;
    /**
     * C1系列命令字
     */
    // C1开环电机命令字
    public static final byte[] C1_CMD_MOTOR_ = {(byte) 0xA5, (byte) 0x02};

    // C1 Led灯控制
    public static final byte[] C1_CMD_LED_ = {(byte) 0xA5, (byte) 0x04};

    //读取版本号
    public static final byte[] CMD_GET_STM32_VERSION={(byte)0x11,(byte)0x06};

    // 向客户端发送协议
    public static byte[] buildProtocol(byte type, byte cmd1, byte cmd2, byte[] data) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        return sendbuff;
    }

    // 向客户端发送协议
    public static byte[] buildProtocol(byte type, byte[] cmd, byte[] data) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        return sendbuff;
    }

    public static byte[] getData(byte[] protocolBytes) {
        byte[] data = new byte[protocolBytes.length - 12];
        System.arraycopy(protocolBytes, 11, data, 0, protocolBytes.length - 12);
        return data;
    }

    // 向客户端发送协议
    public static void sendProtocol(byte type, byte[] cmd, byte[] data) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        try {
            SP.request(sendbuff,20);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    // 向客户端发送协议
    public static byte[] sendProtocol(byte type, byte[] cmd, byte[] data,boolean isToStm) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            buf[0] = type;
            buf[1] = cmd[0];
            buf[2] = cmd[1];
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        try {
            byte[] buffer = SP.request(sendbuff,100);
            if (buffer != null){
                return buffer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[30];
    }

    // 协议封装： AA 55 len1 len2 type cmd1 cmd2 00 00 00 00 (data) check
    public static byte[] addProtocol(byte[] buff) {
        short len = (short) (buff.length);
        byte[] sendbuff = new byte[len + 4];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        sendbuff[3] = (byte) (len & 0x00FF); // 长度: 从type到check
        sendbuff[2] = (byte) ((len >> 8) & 0x00FF);
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data

        byte check = 0x00; // 校验位
        for (int n = 0; n <= len + 2; n++) {
            check += sendbuff[n];
        }
        sendbuff[len + 3] = (byte) (check & 0x00FF);
        return sendbuff;
    }
}
