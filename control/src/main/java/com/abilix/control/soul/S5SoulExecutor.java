package com.abilix.control.soul;

import java.util.Random;
import java.util.TimerTask;

import com.abilix.control.utils.LogMgr;

public class S5SoulExecutor extends ASoulExecutor {

    private TimerTask timerTask_10S;

    public S5SoulExecutor() {
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
                LogMgr.d("S play 10S cmd");
                mPlayer.play("cchangedansw"+i+".mp3");
            }
        };

        timer_10S.schedule(timerTask_10S, TEN_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    private int getRandom() {
        Random random = new Random();
        int i = random.nextInt(8) + 1;
        return i;
    }

    @Override
    public void execute30SCmd() {
        super.execute30SCmd();
    }

    @Override
    public void cancelCmd() {
        super.cancelCmd();
        mPlayer.stop();
    }

}
