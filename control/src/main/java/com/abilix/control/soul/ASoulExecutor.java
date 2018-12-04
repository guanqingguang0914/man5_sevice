package com.abilix.control.soul;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.Player;

public abstract class ASoulExecutor implements ISoulExecutor {

    /**
     * 音频循环播放间隔时间
     */
    protected static final int CIRCULATION_TIME = 30000;

    /**
     * 5秒音频循环播放间隔时间
     */
    protected static final int FIVE_CIRCULATION_TIME = 0;

    /**
     * 10秒音频循环播放间隔时间
     */
    protected static final int TEN_CIRCULATION_TIME = 5000;

    /**
     * 30秒音频循环播放间隔时间
     */
    protected static final int THIRTY_CIRCULATION_TIME = 20000;

    protected boolean isStop = false;
    protected Player mPlayer;
    protected Timer timer_5S;
    protected Timer timer_10S;
    protected Timer timer_30S;
    protected Timer timer_playSound;
    protected TimerTask timerTask_5s;
    protected TimerTask timerTask_playSoud;
    protected String[] emotion_souds = {"biao_yang.mp3", "gao_xing.mp3", "huai_yi.mp3", "jin_ya.mp3", "sheng_qi.mp3",
            "tan_xi.mp3"};

    public ASoulExecutor() {
        mPlayer = new Player();
    }

    @Override
    public void execute5SCmd() {
        isStop = false;
        LogMgr.d("play 5s cmd");
        if (timer_5S != null) {
            timer_5S.cancel();
            timer_5S = null;
            mPlayer.stop();
        }
        if (timerTask_5s != null) {
            timerTask_5s.cancel();
            timerTask_5s = null;
        }
        timer_5S = new Timer();
        timerTask_5s = new TimerTask() {

            @Override
            public void run() {
                int soud_position = new Random().nextInt(6);
                mPlayer.play(emotion_souds[soud_position]);
            }
        };
        timer_5S.schedule(timerTask_5s, FIVE_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    @Override
    public void execute10SCmd() {
        if (timer_10S != null) {
            timer_10S.cancel();
            timer_10S = null;
            mPlayer.stop();
        }
        timer_10S = new Timer();
        LogMgr.d("play 10s cmd");

    }

    @Override
    public void execute30SCmd() {
        if (timer_30S != null) {
            timer_30S.cancel();
            timer_30S = null;
            mPlayer.stop();
        }
        if (timer_playSound != null) {
            timer_playSound.cancel();
            timer_playSound = null;
            mPlayer.stop();
        }
        if (timerTask_playSoud != null) {
            timerTask_playSoud.cancel();
            timerTask_playSoud = null;
        }
        timer_30S = new Timer();
        timer_playSound = new Timer();
        LogMgr.d("play 30s cmd");
        timerTask_playSoud = new TimerTask() {

            @Override
            public void run() {
                if (Utils.isZh()) {
                    mPlayer.play("en_changeddefb.mp3");
                } else {
                    mPlayer.play("changeddefb.mp3");
                }
            }
        };
        timer_playSound.schedule(timerTask_playSoud, THIRTY_CIRCULATION_TIME, CIRCULATION_TIME);
    }

    @Override
    public void cancelCmd() {
        LogMgr.d("stop soul");
        isStop = true;
        if (timer_5S != null) {
            timer_5S.cancel();
            timer_5S = null;
        }
        if (timer_10S != null) {
            timer_10S.cancel();
            timer_10S = null;
        }
        if (timer_30S != null) {
            timer_30S.cancel();
            timer_30S = null;
        }
        if (timerTask_playSoud != null) {
            timerTask_playSoud.cancel();
            timerTask_playSoud = null;
        }
        mPlayer.stop();
    }

    ;

}
