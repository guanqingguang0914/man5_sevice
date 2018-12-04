package com.abilix.control.soul;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

import java.util.Random;
import java.util.TimerTask;


public class C1SoulExecutor extends ASoulExecutor {
    private TimerTask timerTask_10S;
    private TimerTask timerTask_30S;

    public C1SoulExecutor() {
        super();

    }

    @Override
    public void execute5SCmd() {
        super.execute5SCmd();
    }

    @Override
    public void execute10SCmd() {
        super.execute10SCmd();
        if (timerTask_10S != null) {
            timerTask_10S.cancel();
            timerTask_10S = null;
        }
        timerTask_10S = new TimerTask() {

            @Override
            public void run() {
                int i = getRandom();
                LogMgr.d("C1 play 10S cmd:" + "cchangedansw" + i + ".mp3");
                mPlayer.play("cchangedansw" + i + ".mp3");
            }
        };

        timer_10S.schedule(timerTask_10S, TEN_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    public void execute30SCmd() {
        super.execute30SCmd();
        if (timerTask_30S != null) {
            timerTask_30S.cancel();
            timerTask_30S = null;
        }
        timerTask_30S = new TimerTask() {

            @Override
            public void run() {
                LogMgr.d("execute 30s cmd");
                setLedLight();
            }
        };
        timer_30S.schedule(timerTask_30S, THIRTY_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    ;

    public void cancelCmd() {
        super.cancelCmd();
        mPlayer.stop();
        stopLedLight();
    }

    ;

    private void stopLedLight() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setLightColor(0);
    }

    private void setLedLight() {
        for (int i = 1; i < 4; i++) {
            try {
                if (!isStop) {
                    setLightColor(i);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopLedLight();
    }

    /**
     * @return
     * @Description 获取1-8的随机数
     * @author lz
     * @time 2017-4-6 下午3:52:03
     */
    private int getRandom() {
        Random random = new Random();
        int i = random.nextInt(8) + 1;
        return i;
    }

    /**
     * @param rgb
     * @Description 设置灯的颜色
     * @author lz
     * @time 2017-4-7 下午7:15:43
     */
    private void setLightColor(int rgb) {
        byte[] bs = new byte[1];
        bs[0] = (byte) rgb;
        writeData((byte) 0x04, bs);
    }

    /**
     * 向stm32写数据
     *
     * @param
     * @param cmd2
     * @param data
     */
    private void writeData(byte cmd2, byte[] data) {
        byte[] bs = ProtocolUtils.buildProtocol((byte) 0x09, (byte) 0xA5, cmd2, data);
        SP.request(bs);
    }
}
