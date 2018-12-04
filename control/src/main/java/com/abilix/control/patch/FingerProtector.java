package com.abilix.control.patch;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;

import com.abilix.control.ControlApplication;
import com.abilix.control.GlobalConfig;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.utils.LogMgr;

public class FingerProtector {
    private Timer timer;
    private TimerTask timerTask;
    private MySensor mSensor;

    /**
     * 控制手指自保程序是否启用
     */
    private boolean isFingerProtectActiveCrl = true;

    private FingerProtector() {
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    /**
     * 开启H机器人的手指保护程序
     */
    public void startSelfProtectTimer() {
        if (GlobalConfig.isFingerProtectActive) {
            if (timer != null) {
                timer.cancel();
            }
            if (timerTask != null) {
                timerTask.cancel();
            }
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    refresh();
                }
            };
            timer.schedule(timerTask, 0, 200);
        }
    }

    private void refresh() {
        if (mSensor == null) {
            return;
        }
        float[] mO = mSensor.getmO();
        if (mO != null && isFingerProtectActiveCrl) {
            LogMgr.d("SelfProtect", "mO[1] = " + mO[1]);
            if (mO[1] > 20 || mO[1] < -30) {
                LogMgr.e("SelfProtect", "倾斜角度过大，有摔倒危险");
                sendBroadcast();
            }
        } else {
            LogMgr.e("mO is null");
        }
    }

    private void sendBroadcast() {
        Intent intent = new Intent(SelfProtectBroadcastReceiver.SelfProtectBroadcastActionName);
        ControlApplication.instance.sendBroadcast(intent);
        LogMgr.d("SelfProtect", "sendBroadcast()");
    }
}
