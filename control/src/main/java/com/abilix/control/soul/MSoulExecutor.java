package com.abilix.control.soul;

import java.util.Random;
import java.util.TimerTask;

import com.abilix.control.utils.LogMgr;

public class MSoulExecutor extends ASoulExecutor {
    private MEyeLamp mEyeLamp;
    private MNeck mMNeck;
    private TimerTask timerTask_10S;
    private TimerTask timerTask_30S;

    public MSoulExecutor() {
        super();
        mEyeLamp = new MEyeLamp();
        mMNeck = new MNeck();
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
                LogMgr.d("M play 10S cmd:" + "changedansw" + i + ".mp3");
                mPlayer.play("changedansw" + i + ".mp3");
                mEyeLamp.startBlinkEye();
                mMNeck.swingHead();
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
                mEyeLamp.startBlinkEye();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mMNeck.nodHead();
            }
        };
        timer_30S.schedule(timerTask_30S, THIRTY_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    ;

    public void cancelCmd() {
        super.cancelCmd();
        mEyeLamp.stopBinkEye();
        mMNeck.resetNeck();
    }

    ;

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
