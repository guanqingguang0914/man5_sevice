package com.abilix.control;

import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.IBrain;
import com.abilix.control.utils.LogMgr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

/**
 * @author jingh
 * @Descripton:该类主要是绑定BrainService,并向Brain发送反馈信息，使用前需要先调用initBranResponder（）方法绑定Brain。
 * @date2017-3-29下午2:22:37
 */
public class BrainResponder {

    private static BrainResponder instance = new BrainResponder();
    private HandlerThread responseHandlerThread;
    private ResponseHandler responseHandler;
    private static IBrain mIBrain;
    private static IInitCallback mIInitCallback;

    private BrainResponder() {
        responseHandlerThread = new HandlerThread("ResponseHandlerThread");
        responseHandlerThread.start();
        responseHandler = new ResponseHandler(responseHandlerThread.getLooper());
    }

    /**
     * 绑定BrainService
     */
    public static synchronized void initBrainResponder(IInitCallback iInitCallback) {
        mIInitCallback = iInitCallback;
        bindBrainService();
    }

    public static BrainResponder getInstance() {
        return instance;
    }

    private synchronized static void bindBrainService() {
        LogMgr.d("start bind brain service");
        Intent it = new Intent("com.abilix.brain.aidl.IBrain");
        it.setPackage("com.abilix.brain");
        ControlApplication.instance.bindService(it, mBrainServiceCon, Service.BIND_AUTO_CREATE);

    }

    static ServiceConnection mBrainServiceCon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            LogMgr.d("bund service sucess and get iBrain service");
            mIBrain = IBrain.Stub.asInterface(service);
            if (mIInitCallback != null) {
                mIInitCallback.onSucess();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (mIBrain != null) {
                mIBrain = null;
            }
            LogMgr.d("brain service disconnected");
        }
    };

    /**
     * @param brain
     * @Descripton:对外接口
     */
    public void responsetToBrain(Brain brain) {
        if (brain != null) {
            Message responseMsg = responseHandler.obtainMessage();
            responseMsg.obj = brain;
            responseHandler.sendMessage(responseMsg);
        }
    }

    private class ResponseHandler extends Handler {

        public ResponseHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Brain brain = (Brain) msg.obj;
            if (mIBrain == null) {
                LogMgr.e("mIBrain is null,绑定Brain还未成功");
            }
            if (mIBrain != null && brain != null) {
                try {
                    mIBrain.BrainInterface(brain);
                    LogMgr.d("send response to brain");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void destory() {
        if (mBrainServiceCon != null) {
            LogMgr.d("unbind brain service");
            ControlApplication.instance.unbindService(mBrainServiceCon);
        }
    }
}
