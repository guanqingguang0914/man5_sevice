package com.abilix.control.soul;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.ControlKernel;
import com.abilix.control.IControlKenel;
import com.abilix.control.R;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.pad.CProtocolDisposer;
import com.abilix.control.pad.HProtocolDisposer;
import com.abilix.control.pad.MProtocolDisposer;
import com.abilix.control.utils.LogMgr;

public class SoulTracker {
    private static SoulTracker instance = null;
    private final static Object mLock = new Object();
    private DoSoulCmdHandler doSoulCmdHandler;
    private HandlerThread soulHandlerThread;
    private static MediaPlayer mp = null;// 声明一个MediaPlayer对象
    private ISoulExecutor mSoulExecutor;

    private SoulTracker() {
        soulHandlerThread = new HandlerThread("soulHandlerThread");
        soulHandlerThread.start();
        doSoulCmdHandler = new DoSoulCmdHandler(soulHandlerThread.getLooper());
        mSoulExecutor = ControlFactory.createSoulExecutor();
    }

    public static SoulTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new SoulTracker();
                }
            }
        }
        return instance;
    }

    public void doSoulCmd(int state, IControlKenel iControl) {
        Message msg = doSoulCmdHandler.obtainMessage();
        msg.obj = state;
        doSoulCmdHandler.sendMessage(msg);
    }

    private class DoSoulCmdHandler extends Handler {
        public DoSoulCmdHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int state = (Integer) msg.obj;
            LogMgr.d("soul cmd::" + state);
//            if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
//                LogMgr.w("H系列暂时取消SOUL功能");
//                return;
//            }
            if (state == 1) {
                mSoulExecutor.execute30SCmd();
                mSoulExecutor.execute10SCmd();
                mSoulExecutor.execute5SCmd();
            } else if (state == 0) {
                mSoulExecutor.cancelCmd();
            }
        }
    }

    public void destory() {
        mSoulExecutor.cancelCmd();
    }
}
