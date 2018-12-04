package com.abilix.control.learnletter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.IControlKenel;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.utils.LogMgr;

public class LearnLetterTracker {

    private static LearnLetterTracker instance = null;
    private final static Object mLock = new Object();
    private IControlKenel mIControl;
    private ILearnLetterCmdDisposer mLearnLetterCmdDisposer;
    private HandlerThread doLearnLetterCmdThread;
    private Handler doLearnLettercmdThreadHandler;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            byte[] response_data = (byte[]) msg.obj;
            try {
                mIControl.doLearnLetterCmdCallBack(response_data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ;
    };

    private LearnLetterTracker() {
        LogMgr.d("LearnLetterTracker()");
        doLearnLetterCmdThread = new HandlerThread("doLearnLetterCmdThread");
        doLearnLetterCmdThread.start();
        doLearnLettercmdThreadHandler = new DoLearnLetterCmdThreadHandler(doLearnLetterCmdThread.getLooper());
        mLearnLetterCmdDisposer = ControlFactory.createLearnLetterCmdDisposer(mHandler);
    }

    public static LearnLetterTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new LearnLetterTracker();
                }
            }
        }
        return instance;
    }

    public void doLearnLetterCmd(int modeState, String fileFullPath, IControlKenel iControl) {
        LogMgr.v("doLearnLetterCmd");
        this.mIControl = iControl;
        Message msg = doLearnLettercmdThreadHandler.obtainMessage();
        msg.arg1 = modeState;
        msg.obj = fileFullPath;
        LogMgr.d("modeState = " + modeState + ";fileFullPath = "+ fileFullPath + "doLearnLettercmdThreadHandler = " + (doLearnLettercmdThreadHandler == null));
        doLearnLettercmdThreadHandler.removeCallbacksAndMessages(null);
        doLearnLettercmdThreadHandler.sendMessage(msg);
    }

    private class DoLearnLetterCmdThreadHandler extends Handler {
        public DoLearnLetterCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mLearnLetterCmdDisposer.disposeLearnLetterCmd(msg.arg1, (String) msg.obj);
        }
    }
}
