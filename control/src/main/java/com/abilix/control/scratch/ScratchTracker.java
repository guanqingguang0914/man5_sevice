package com.abilix.control.scratch;

import java.io.File;

import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.aidl.Brain;
import com.abilix.control.IControlKenel;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class ScratchTracker {
    private final String audioPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "animals"
            + File.separator;
    private static ScratchTracker instance = null;
    private final static Object mLock = new Object();
    private HandlerThread doScratchCmdThread;
    private DoCmdThreadHandler mDoCmdThreadHandler;
    private AudioManager am;
    private IControlKenel mIControl;
    private IScratchExecutor mScratchExecutor;
    private int request_id = -1;//初始值给个-1
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            Brain mscratch = new Brain(2, null);
            if (mIControl == null) {
                LogMgr.e("ScratchTracker() mIControl == null");
                return;
            }
            switch (msg.what) {

                case 1:
                    byte[] response_buff = (byte[]) msg.obj;
                    mIControl.doScratchCmdCallBack(response_buff);
                    break;
                case 2:
                    //显示照片。
                    LogMgr.e("come  M  takepicture");
                    mscratch.setModeState(4);//拍照。
                    mscratch.setSendByte((byte[]) msg.obj);
                    mIControl.doScratchCmdCallBack(mscratch);
                    break;
                case 3:
                    mscratch.setModeState(7);//7是录音。
                    mscratch.setSendByte((byte[]) msg.obj);
                    mIControl.doScratchCmdCallBack(mscratch);
                    break;
                case 4:
                    mscratch.setModeState(10);//校准指南针
                    byte[] compass = new byte[1];
                    compass[0] = 2;
                    mscratch.setSendByte(compass);
                    mIControl.doScratchCmdCallBack(mscratch);
                    break;
                case 5://以后的通知brain全部用五来表示。
                    mscratch.setModeState(msg.arg1);
                    mscratch.setSendByte((byte[]) msg.obj);
                    mIControl.doScratchCmdCallBack(mscratch);
                    break;
            }

        }
    };

    private ScratchTracker() {
        doScratchCmdThread = new HandlerThread("doScratchCmdThread");
        doScratchCmdThread.start();
        mDoCmdThreadHandler = new DoCmdThreadHandler(doScratchCmdThread.getLooper());
        mScratchExecutor = ControlFactory.createScratchExecutor(mHandler);
    }

    public static ScratchTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new ScratchTracker();
                }
            }
        }
        return instance;
    }

    public void doScratchCmd(byte[] cmd, IControlKenel iControl) {
//        LogMgr.e("cmd = " + Utils.bytesToString(cmd,cmd.length));
        if (cmd.length > 7) {
//            LogMgr.e("cmd = " + Utils.bytesToString(cmd,cmd.length));
            this.mIControl = iControl;
            Message msg = mDoCmdThreadHandler.obtainMessage();
            msg.obj = cmd;
//            LogMgr.e("mDoCmdThreadHandler = "+ (mDoCmdThreadHandler == null));
            mDoCmdThreadHandler.sendMessage(msg);
        } else {
            //这里是所有状态的清理。
            if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){//添加灯光熄灭H3
                for(int i = 0; i<2; i++){
                    ProtocolBuilder.sendProtocol((byte) ControlInfo.getMain_robot_type(), new byte[]{(byte) 0xA3, (byte) 0x74}, new byte[]{0,0,0});
                }
            }
            LogMgr.e("这里是所有状态的清理");
            mScratchExecutor.clearState();
        }

    }

    private class DoCmdThreadHandler extends Handler {
        public DoCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] data = (byte[]) msg.obj;
            LogMgr.e(Utils.bytesToString(data));
            try {
                if(data.length > 14){
                    int id = Utils.bytesToInt2(data, 11);//data[14];//这个是请求ID。
                    if (request_id != id) {
                        request_id = id;
                        mScratchExecutor.execute(data);
                    }else{
                        LogMgr.e("发送数据重复");
                    }
                    LogMgr.e("request_id = " + request_id);
                }else {
                    LogMgr.e("发送数据出错 ID 字段错误");
                    return;
                }
            }catch (Exception e){
                LogMgr.e("发送数据出错 e= "+e);
                e.printStackTrace();
            }
        }
    }

}
