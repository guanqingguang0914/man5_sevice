package com.abilix.control.soul;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;

public class M1EyeLamp {
    private Thread mLedColorThread;
    private boolean isStop = false;

    public M1EyeLamp() {

    }

    public void startBlinkEye() {
        isStop = false;
        mLedColorThread = new LedColor();
        mLedColorThread.start();
    }

    public void stopBinkEye() {
        isStop = true;
        turnoutLight();
    }

    private void turnoutLight() {
        byte[] bs = {(byte) 0x01};
        writeData((byte) 0x35, bs);
    }

    class LedColor extends Thread {
        @Override
        public void run() {
            super.run();
            if (!isStop) {
                setEyeColor(255, 0, 0);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            if (!isStop) {
                setEyeColor(0, 255, 0);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            if (!isStop) {
                setEyeColor(0, 0, 255);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            stopBinkEye();
        }
    }

    /**
     * 开始动作
     */
    private void setEyeColor(int r, int g, int b) {
        byte[] bs = new byte[30];
        for (int i = 0; i < 10; i++) {
            bs[i * 3] = (byte) r;
            bs[i * 3 + 1] = (byte) g;
            bs[i * 3 + 2] = (byte) b;
        }
        writeData((byte) 0x31, bs);

    }

    /**
     * 向stm32写数据
     *
     * @param
     * @param cmd2
     * @param data
     */
    private void writeData(byte cmd2, byte[] data) {
        byte[] bs = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, cmd2, data);
        SP.request(bs);
    }
}
