package com.abilix.control.soul;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.Random;
import java.util.TimerTask;

public class HSoulExecutor extends ASoulExecutor {
    private TimerTask timerTask_10S;
    public HSoulExecutor(){
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
                mPlayer.play(Utils.isZh()? ("HHero_e"+i+".mp3") : ("HHero_c"+i+".mp3"));
            }
        };

        timer_10S.schedule(timerTask_10S, TEN_CIRCULATION_TIME, CIRCULATION_TIME);

    }

    @Override
    public void execute30SCmd() {
        super.execute30SCmd();

    }
    private int getRandom() {
        Random random = new Random();
        int i = random.nextInt(7);
        if(i != 3 && i!= 5){
            return i;
        }
        return 1;
    }
    @Override
    public void cancelCmd() {
        super.cancelCmd();
        mPlayer.stop();
    }

}
