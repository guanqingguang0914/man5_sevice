package com.abilix.control.upgrade;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;


public class BFirmwareUpgrader extends AbstractFirmwareUpgrade {
    @Override
    public int getVersion(byte type) {
        byte[] verBuff = sendProtocol(type, CMD_ONE, QUERY_VER, null, false);

        if(verBuff==null){
            return -1;
        }
        LogMgr.d("stm 32 query version response::" + Utils.bytesToString(verBuff));
        if (verBuff.length >= 16 && verBuff[6] == RESPONSE_VER_OK && verBuff[5] == (byte) 0xF0) {
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
    //获取1号舵机版本号
    @Override
    public int getSERVOVersion(byte type) {
        int version = -1;
        byte[] response;
        for (int i = 0; i < 5; i++) {
            response = SP.request(ProtocolUtils.buildProtocol(type,S_ADJUST_OUT_CMD_1, S_ADJUST_OUT_CMD_5_TEST,getIDbyte(i+1)),15);
//            response = sendProtocol(type,S_ADJUST_OUT_CMD_1, S_ADJUST_OUT_CMD_5_TEST,getIDbyte(i+1),false);
            if(response != null && response.length >= 18){
                version =  response[21] & 0xFF;
                break;
            }
        }
        return version;
    }
    private byte[] getIDbyte(int mCurrentID) {
        byte[] send = new byte[11];
        send[0] = 0x02;
        send[2] = 0x08;
        byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x00, 0x04, 0x02, 0x00, 0x36,0x00};//FF FF ID len 02 00 36 CRC
        data[2] = (byte)mCurrentID;
        byte check = 0;
        for (int i = 2; i < 7; i++) {
            check += data[i];
        }
        data[7] = (byte) ~(check & 0xFF);
        System.arraycopy(data,0,send,3,8);
        return send;
    }
//    /**
//     * 获取stm8版本号
//     * @param type
//     * @return
//     */
//    public int getStm8Version(byte type){
//        byte[] queryCmd = ProtocolUtils.buildProtocol(type,CMD_ONE,STM8_QUERT_VERSION,null);
//        byte[] resBuff= SP.request(queryCmd,3*1000);
//        if(resBuff==null){
//            return -1;
//        }
//        LogMgr.d("stm 8 获取版本返回 = " + Utils.bytesToString(resBuff, resBuff.length));
//        if (resBuff.length >= 16 && resBuff[6] == RESPONSE_VER_OK && resBuff[5] == (byte) 0xF0) {
//            byte[] ver = new byte[4];
//            ver[0] = verBuff[11];
//            ver[1] = verBuff[12];
//            ver[2] = verBuff[13];
//            ver[3] = verBuff[14];
//            int verValue = Utils.byteAray2IntLH(ver);
//            LogMgr.d("STM version::" + verValue);
//            return verValue;
//        }
//
//        return -1;
//    }

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

    private synchronized static byte[] sendBuff(byte[] b, boolean isNeedMoreTime) {
        byte[] temp = new byte[256];
        try {
            LogMgr.e("send buff::" + Utils.bytesToString(b));
            return  SP.requestOnlyUpdate(b,5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogMgr.e("reponse is null");
        return temp;
    }

    @Override
    public boolean upgrade(byte type, String filePath) {
        byte[] reponseBuff = sendProtocol(type, CMD_ONE, SEND_JUMP, null, false);
        if(reponseBuff==null){
            LogMgr.e("升级跳转失败");
            return false;
        }
        LogMgr.e("reponseBuff::" + (reponseBuff==null?"null":Utils.bytesToString(reponseBuff)));
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
            LogMgr.e("responseBuff::" + (responseBuff==null?"null":Utils.bytesToString(responseBuff)));
            if (!(responseBuff!=null && responseBuff[6] == RESPONSE_REQUEST)) {
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
                    Thread.sleep(5);
                    resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                    LogMgr.e("resBuff::" + (resBuff==null?"null":Utils.bytesToString(resBuff)));
                    int i = 0;
                    while (!(resBuff!=null && resBuff[6] == RESPONSE_REQUEST) & i < 5) {
                        LogMgr.e("send buff and try times::" + (i + 1));
                        Thread.sleep(40);
                        resBuff = sendProtocol(type, CMD_ONE, SEND_ING, b, false);
                        LogMgr.e("send data buff response::" + (resBuff==null?"null":Utils.bytesToString(resBuff)));
                        i++;
                        if (i >= 5) {
                            LogMgr.e("send buff NG and give up");
                            return false;
                        }
                    }
                }
                Thread.sleep(5);
                byte[] rBuff = sendProtocol(type, CMD_ONE, SEND_END, null, false);
                LogMgr.e("rBuff::" + (rBuff==null?"null":Utils.bytesToString(rBuff)));
                if (!(rBuff!=null && rBuff[6] == RESPONSE_SEND_OK)) {
                    LogMgr.e("send file NG");
                    return false;
                }
                LogMgr.e("send file OK");
                return true;
            } catch (Exception e) {
                LogMgr.e("stm32升级异常");
                e.printStackTrace();
                return false;
            }

        }
        LogMgr.e("no correct response to reset cmd");
        return false;
    }

}
