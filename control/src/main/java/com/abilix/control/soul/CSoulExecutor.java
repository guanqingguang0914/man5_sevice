package com.abilix.control.soul;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

import java.util.Random;
import java.util.TimerTask;

public class CSoulExecutor extends ASoulExecutor {
    byte[] light_send = new byte[]{'C', 'S', 'E', 'T', 'L', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 'O'};
    private TimerTask timerTask_10S;
    private TimerTask timerTask_30S;

    public CSoulExecutor() {
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
                LogMgr.d("C play 10S cmd:" + "cchangedansw" + i + ".mp3");
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
        byte[] rgb_send = new byte[]{'C', 'S', 'E', 'T', 'L', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 'O'};
        rgb_send[5] = 0;
        for (int i = 0; i < 3; i++) {
            try {
                SP.request(rgb_send);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setLedLight() {
        byte[] rgb_send = new byte[]{'C', 'S', 'E', 'T', 'L', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 'O'};
        rgb_send[5] = 1;
        for (int i = 0; i < 3; i++) {
            try {
                if (!isStop) {
                    SP.request(rgb_send);
                }
                Thread.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rgb_send[5] = 2;
        for (int i = 0; i < 3; i++) {
            try {
                if (!isStop) {
                    SP.request(rgb_send);
                }
                Thread.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rgb_send[5] = 3;
        for (int i = 0; i < 3; i++) {
            try {
                if (!isStop) {
                    SP.request(rgb_send);
                }
                Thread.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rgb_send[5] = 0;
        for (int i = 0; i < 3; i++) {
            try {
                if (!isStop) {
                    SP.request(rgb_send);
                }
                Thread.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
}
