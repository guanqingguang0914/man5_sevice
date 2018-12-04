package com.abilix.control.pad;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.IControlKenel;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.sp.PushMsg;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

public class PadTracker {
    private static PadTracker instance = null;
    private final static Object mLock = new Object();
    private IControlKenel mIControl;
    private IProtocolDisposer mProtocolDisposer;
    private HandlerThread doPadCmdThread;
    private Handler doPadcmdThreadHandler;
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            byte[] response_data = (byte[]) msg.obj;
            try {
                mIControl.doPadCmdCallBack(response_data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static PadTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                instance = new PadTracker();
            }
        }
        return instance;
    }

    private PadTracker() {
        doPadCmdThread = new HandlerThread("doPadCmdThread");
        doPadCmdThread.start();
        doPadcmdThreadHandler = new DoPamCmdThreadHandler(doPadCmdThread.getLooper());
        mProtocolDisposer = ControlFactory.createProtocolDisposer(mHandler);
    }

    public void doPadCmd(byte[] cmd, IControlKenel iControl) {
        this.mIControl = iControl;
        Message msg = doPadcmdThreadHandler.obtainMessage();
        msg.obj = cmd;
        LogMgr.e("send message");
        doPadcmdThreadHandler.sendMessage(msg);
    }

    private class DoPamCmdThreadHandler extends Handler {
        public DoPamCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mProtocolDisposer.DisposeProtocol(msg);
        }

    }

    // 取消未执行完的命令
    public void  removeCmd() {
        doPadcmdThreadHandler.removeCallbacksAndMessages(null);
    }

    // 关闭正在执行的命令
    public void stopCmd() {
        try {
            mProtocolDisposer.stopDisposeProtocol();
        } catch (Exception e) {
            LogMgr.e("stopCmd() 异常");
            e.printStackTrace();
        }
    }

}
