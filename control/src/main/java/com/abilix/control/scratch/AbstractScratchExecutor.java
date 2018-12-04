package com.abilix.control.scratch;


import android.os.Handler;
import android.os.Message;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.Player;

public abstract class AbstractScratchExecutor implements IScratchExecutor {
    protected Player mPlayer;
    protected Handler mHandler;

    public AbstractScratchExecutor(Handler mHandler) {
        mPlayer = new Player();
        this.mHandler = mHandler;
    }

    // String解析
    protected String getParam(String strRecv, int pos) {
        int n1 = 0, n2 = 0, n3 = 0, n4 = 0;
        String s1 = "";
        if (strRecv == "")
            return "";

        // AbilixRot: runMotor: 19,M2,100
        if (pos == 0) { // 头
            n1 = strRecv.indexOf(':');
            if (n1 != -1)
                s1 = strRecv.substring(0, n1);
            return s1;
        } else if (pos == 1) { // 函数名
            n1 = strRecv.indexOf(':');
            if (n1 != -1)
                n2 = strRecv.indexOf(':', n1 + 1);
            if (n2 != -1)
                s1 = strRecv.substring(n1 + 2, n2);
            return s1;
        } else if (pos == 2) { // 参数1
            n1 = strRecv.indexOf(',');
            if (n1 != -1)
                n2 = strRecv.indexOf(',', n1 + 1);
            if (n2 == -1)
                s1 = strRecv.substring(n1 + 1);
            else
                s1 = strRecv.substring(n1 + 1, n2);
            return s1;
        } else if (pos == 3) { // 参数2
            n1 = strRecv.indexOf(',');
            if (n1 != -1)
                n2 = strRecv.indexOf(',', n1 + 1);
            if (n2 != -1) {
                n3 = strRecv.indexOf(',', n2 + 1);
                if (n3 == -1)
                    s1 = strRecv.substring(n2 + 1);
                else
                    s1 = strRecv.substring(n2 + 1, n3);
            }
            return s1;
        } else if (pos == 4) { // 参数3
            n1 = strRecv.indexOf(',');
            if (n1 != -1)
                n2 = strRecv.indexOf(',', n1 + 1);
            if (n2 != -1)
                n3 = strRecv.indexOf(',', n2 + 1);
            if (n3 != -1) {
                n4 = strRecv.indexOf(',', n3 + 1);
                if (n4 == -1)
                    s1 = strRecv.substring(n3 + 1);
                else
                    s1 = strRecv.substring(n3 + 1, n4);
            }
            return s1;
        } else if (pos == 10) { // 返回数据
            n1 = strRecv.indexOf(',');
            if (n1 != -1)
                s1 = strRecv.substring(0, n1 + 1);
            else
                s1 = strRecv + ",";
            return s1;
        }
        return "";
    }


    protected void writeBuff(byte[] buff) {
        try {
            for (int n = 0; n < 2; n++) {
                SP.request(buff,20);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
    }

    public void dealwith(int value, byte[] data, int n) {

        LogMgr.e("value is " + value);
        byte[] sendtopad = new byte[20];
        System.arraycopy(data, 0, sendtopad, 0, data.length);
        sendtopad[3] = (byte) 0x10;
        sendtopad[5] = (byte) 0xA1;
        sendtopad[18] = (byte) (value & 0xFF);
        sendtopad[17] = (byte) ((value >> 8) & 0xFF);
        sendtopad[16] = (byte) ((value >> 16) & 0xFF);
        sendtopad[15] = (byte) ((value >> 24) & 0xFF);
        SendScratch(n, sendtopad);
    }

    public void SendScratch(int n, byte[] buff) {
        if (buff != null) {
            //LogMgr.e("send Scratch  "+str);
            //byte[] buff = str.getBytes("UTF-8");
            //LogMgr.e("send Scratch  "+Utils.bytesToString(buff, buff.length));
            Message msg = mHandler.obtainMessage();
            msg.what = n;
            msg.obj = buff;
            mHandler.sendMessage(msg);
        }
    }

    public void SendScratchToBrain(int n, int mode, byte[] buff) {

        Message msg = mHandler.obtainMessage();
        msg.what = n;
        msg.arg1 = mode;
        msg.obj = buff;
        LogMgr.e("send to pad:   " + Utils.bytesToString(buff));
        mHandler.sendMessage(msg);
    }
}
