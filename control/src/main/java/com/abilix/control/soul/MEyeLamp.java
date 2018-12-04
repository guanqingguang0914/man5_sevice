package com.abilix.control.soul;

import com.abilix.control.protocol.ProtocolSender;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

public class MEyeLamp {
    private byte[] colorSelect = {0x55, 'E', 'Y', 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private byte[] lamp = {0x55, 'E', 'Y', 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private Thread mLedColorThread;
    private boolean isStop = false;
    private SoulHelper mSoulHelper;

    private byte[] eye_r = new byte[48];
    private byte[] eye_g = new byte[48];
    private byte[] eye_b = new byte[48];

    private int count;
    public MEyeLamp() {
        mSoulHelper = new SoulHelper();
    }

    public void startBlinkEye() {
        count = 0;
        isStop = false;
        eyeByte();
        mLedColorThread = new LedColor();
        mLedColorThread.start();
    }

    public void stopBinkEye() {
        count = 0;
        isStop = true;
        ProtocolSender.sendProtocol((byte) 0x02,(byte) 0xA3,(byte) 0x35,new byte[]{0x00});
        turnoutLight();
    }

    private void turnoutLight() {
        LogMgr.d("turn off eye light");
        mSoulHelper.turnOutEyeLights();
    }

    private void eyeByte() {//创建眼灯数据位
        for (int i = 0; i < 48; i++) {
            if(i%3 == 0){
                eye_r[i] = (byte) 0xFF;
            }else if(i%3 == 1){
                eye_g[i] = (byte) 0xFF;
            }else if(i%3 == 2){
                eye_b[i] = (byte) 0xFF;
            }
        }
    }

    class LedColor extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            try {
                while (!isStop) {
                    if (count == 0) {
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x31,eye_r);
//                    ProtocolSender.sendProtocol((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x01,0x03 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x32,new byte[]{(byte)0xFF,0x00,0x00 });
//                    ProtocolSender.sendProtocol((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x02,0x03 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x33,new byte[]{(byte)0xFF,0x00,0x00 });
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x03,0x03 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x34,new byte[]{(byte)0xFF,0x00,0x00 });
                    } else if (count == 1) {
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x31,eye_g);
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x01,0x03 });
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x02,0x03 });
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x03,0x03 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x32,new byte[]{0x00 ,(byte)0xFF,0x00});
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x33,new byte[]{0x00 ,(byte)0xFF,0x00 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x34,new byte[]{0x00 ,(byte)0xFF,0x00});
                    } else if (count == 2) {
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x31,eye_b);
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x01,0x03 });
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x02,0x03 });
//                    sendData((byte) 0x02,(byte) 0xA3,(byte) 0x36,new byte[]{0x03,0x03 });
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x32,new byte[]{0x00 ,0x00,(byte)0xFF});
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x33,new byte[]{0x00 ,0x00 ,(byte)0xFF});
                        sendData((byte) 0x02,(byte) 0xA3,(byte) 0x34,new byte[]{0x00 ,0x00 ,(byte)0xFF});
                        count = -1;
                    }
                    Thread.sleep(1500);
                    if (count == -1){
                        stopBinkEye();
                    }
                    ++count;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopBinkEye();
            }
        }
    }

    private void sendEyeLampMoto(int led1, int led2, int led3, int led4, int led5, int led6, int led7, int led8, int led9, int led10, int led11, int led12, int led13, int led14, int led15, int led16) {

        lamp[4] = (byte) led1;
        lamp[5] = (byte) led2;
        lamp[6] = (byte) led3;
        lamp[7] = (byte) led4;
        lamp[8] = (byte) led5;
        lamp[9] = (byte) led6;
        lamp[10] = (byte) led7;
        lamp[11] = (byte) led8;
        lamp[12] = (byte) led9;
        lamp[13] = (byte) led10;
        lamp[14] = (byte) led11;
        lamp[15] = (byte) led12;
        lamp[16] = (byte) led13;
        lamp[17] = (byte) led14;
        lamp[18] = (byte) led15;
        lamp[19] = (byte) led16;


        try {
            SP.request(lamp); // 先选定眼睛颜色
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendEyeColor() {
        colorSelect[4] = 0;
        colorSelect[5] = 0;
        colorSelect[6] = (byte) 255;

        try {
            SP.request(colorSelect); // 先选定眼睛颜色
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 发送数据
     * @param type
     * @param cmd1
     * @param cmd2
     * @param data
     */
    private void sendData(byte type, byte cmd1, byte cmd2, byte[] data){
        if (!isStop){
            ProtocolSender.sendProtocol(type,cmd1,cmd2,data);
        }
    }
}
