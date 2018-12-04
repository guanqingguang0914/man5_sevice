package com.abilix.control.upgrade;

import com.abilix.control.BroadcastResponder;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.zmodem.ZmodemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class AFirmwareUpgrader extends AbstractFirmwareUpgrade {

    @Override
    public int getVersion(byte type) {
        if (SP.getUpdateState() == Utils.STM32_STATUS_BOOTLOADER){
            //bootloader状态返回最低版本号，提示升级
            return 0;
        }
        byte[] verBuff = sendProtocol(type, CMD_ONE, QUERY_VER, null, false);
        if(verBuff==null){
            return -1;
        }
        LogMgr.d("stm 32 query version response::" + Utils.bytesToString(verBuff));

        if (verBuff.length > 6 && verBuff[6] == RESPONSE_VER_OK) {
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
    @Override
    public int getSERVOVersion(byte type) {
        //获取舵机版本号
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

    private synchronized static byte[] sendBuff(byte[] b, boolean isNeedMoreTime) {
        byte[] temp = null;
        try {
           //LogMgr.e("send buff::" + Utils.bytesToString(b, b.length));
            temp = SP.requestOnlyUpdate(b,5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp == null) {
            LogMgr.e("reponse is null");
            temp = new byte[40];
        }
        return temp;
    }

    @Override
    public boolean upgrade(byte type, String filePath) {
        if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
            return upgradeByZmodem(type, filePath);
        }
        byte[] reponseBuff = sendProtocol(type, CMD_ONE, SEND_JUMP, null, false);
        if(reponseBuff==null){
            LogMgr.e("升级跳转失败");
            return false;
        }
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
             //sendProtocol(TYPE_C, CMD_ONE, CMD_BOOT, null,false);
            byte[] data = new byte[8];
            int len = (int) file.length();
            byte[] byte_fileLen = intToBytes(len);
            System.arraycopy(byte_fileLen, 0, data, 0, byte_fileLen.length);
            long value = Utils.getCRC32(filePath);
            LogMgr.e("CRC value::" + value);
            byte[] byte_value = longToBytes(value);
            System.arraycopy(byte_value, 0, data, 4, byte_value.length);
            byte[] responseBuff = sendProtocol(type, CMD_ONE, SEND_REQUEST, data, true);
            if (responseBuff==null){
                return false;
            }
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
                int sended_length = 0;
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
//                    if(GlobalConfig.isShowFFirmwareUpdateProgress){
//                        sended_length += buff_length;
//                        LogMgr.e("sended_length = "+sended_length +" len = "+len+" progress = "+((int) (1.0*sended_length/len)));
//                        BroadcastResponder.sendStm32UpdateProgress((int) (100.0*sended_length/len));
//                    }
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



    private boolean upgradeByZmodem(int type, String path) {
        // 检查文件是否存在
        File file = new File(path);
        if (!file.exists() && file.length() > 0) {
            LogMgr.e("update file not exist");
            return false;
        }
        switch (Utils.getStm32UpgradeStatus()) {
            case Utils.STM32_STATUS_NORMAL:
                byte[] reponseBuff = sendProtocol((byte) type, CMD_ONE, SEND_JUMP, null, false);
                if (reponseBuff != null && reponseBuff[6] == RESPONSE_JUMP_OK) {
                    LogMgr.e("升级跳转到bootloader");
                    SP.setUpdateState(Utils.STM32_STATUS_UPGRADING);
                } else {
                    LogMgr.e("升级跳转失败");
                    return false;
                }
                break;
            case Utils.STM32_STATUS_BOOTLOADER:
                SP.setUpdateState(Utils.STM32_STATUS_UPGRADING);
                break;
            case Utils.STM32_STATUS_UPGRADING:
            default:
                LogMgr.e("升级状态异常");
                return false;
        }

        int ret = -1;
        try {
            LogMgr.i("SP.destroySP()");
            SP.destroySP();
            Thread.sleep(3000);
            ret = ZmodemUtils.sendFile(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (ret == 0);
    }

}
