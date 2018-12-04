package com.abilix.control.skillplayer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.ControlKernel;
import com.abilix.control.IControlKenel;
import com.abilix.control.factory.ControlFactory;


public class SkillPlayerTracker {

    private static SkillPlayerTracker instance = null;
    private final static Object mLock = new Object();
    private IControlKenel mIControl;
    private ISkillPlayerCmdDisposer mSkillPlayerCmdDisposer;
    private HandlerThread doSkillPlayerCmdThread;
    private Handler doSkillPlayercmdThreadHandler;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            byte[] response_data = (byte[]) msg.obj;
            try {
                mIControl.doSkillPlayerCmdCallBack(response_data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private SkillPlayerTracker() {
        doSkillPlayerCmdThread = new HandlerThread("doSkillPlayerCmdThread");
        doSkillPlayerCmdThread.start();
        doSkillPlayercmdThreadHandler = new DoSkillPlayerCmdThreadHandler(doSkillPlayerCmdThread.getLooper());
        mSkillPlayerCmdDisposer = ControlFactory.createSkillPlayerCmdDisposer(mHandler);
    }

    public static SkillPlayerTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new SkillPlayerTracker();
                }
            }
        }
        return instance;
    }

    public void doSkillPlayerCmd(byte[] cmd, int modeState, IControlKenel iControl) {
        this.mIControl = iControl;
        Message msg = doSkillPlayercmdThreadHandler.obtainMessage();
        msg.obj = cmd;
        msg.arg1 = modeState;
        doSkillPlayercmdThreadHandler.removeCallbacksAndMessages(null);
        doSkillPlayercmdThreadHandler.sendMessage(msg);
    }

    private class DoSkillPlayerCmdThreadHandler extends Handler {
        public DoSkillPlayerCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int modeState = msg.arg1;
            mSkillPlayerCmdDisposer.disposeSkillPlayerCmd((byte[]) msg.obj, modeState);
        }

    }

}
