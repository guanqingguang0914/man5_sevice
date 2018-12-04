package com.abilix.control.soul;


import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;



public class M1Neck {
    private NeckSwingThread mNeckSwingThread;
    private NeckNodThread mNeckNodThread;
    private boolean isResetNeck = false;

    public M1Neck() {

    }

    public void swingHead() {
        mNeckSwingThread = new NeckSwingThread();
        mNeckSwingThread.start();
    }

    public void nodHead() {
        mNeckNodThread = new NeckNodThread();
        mNeckNodThread.start();
    }

    public void resetNeck() {
        LogMgr.d("reset neck");
        isResetNeck = true;
        new Thread() {
            @Override
            public void run() {
                super.run();
                LogMgr.d("reset neck up to 0");
                for (int i = 0; i < 3; i++) { // 发送三次，防止收不到消息
                    setNeckData(0, 130);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setNeckData(1, 15);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private class NeckSwingThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                isResetNeck = false;
                if (!isResetNeck) {
                    setNeckData(0, 0);
                }
                Thread.sleep(3000);
                if (!isResetNeck) {
                    setNeckData(0, 260);
                }
                Thread.sleep(3000);
                setNeckData(0, 130);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class NeckNodThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                isResetNeck = false;
                if (!isResetNeck) {
                    setNeckData(1, 45);
                }
                Thread.sleep(3000);
                if (!isResetNeck) {
                    setNeckData(1, 0);
                }
                Thread.sleep(3000);
                setNeckData(1, 15);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置脖子电机
     * <p>
     * index- 0:head Left or Right(0 ~ 260), 1: head Up or Down(0 ~ 45) byte[]
     * neckData = {
     * 0x55,'N','E','C','K','M','O','T',100,100,100,100,0,0,0,0,0,0,0,0};
     */
    public void setNeckData(int index, int angle) {
        if (0 == index) {
            angle = angle < 0 ? 0 : angle;
            angle = angle > 260 ? 260 : angle;
        } else if (1 == index) {
            angle = angle < 0 ? 0 : angle;
            angle = angle > 45 ? 45 : angle;
        } else {
            return;
        }
        byte[] bs = intToBytes(angle);
        byte[] bs2 = new byte[3];
        bs2[0] = (byte) index;
        System.arraycopy(bs, 0, bs2, 1, bs.length);
        writeData((byte) 0x39, bs2);
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

    /**
     * 将int数值转换为占两个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

}
