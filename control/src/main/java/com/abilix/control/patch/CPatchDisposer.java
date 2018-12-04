package com.abilix.control.patch;

import android.os.Handler;
import android.os.Message;

import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.Control;
import com.abilix.control.model.Model;
import com.abilix.control.utils.LogMgr;

public class CPatchDisposer extends AbstractPatchDisposer {

    public CPatchDisposer(Handler mHandler) {
        super(mHandler);
    }

    @Override
    public void DisposeProtocol(Control control) {
        byte[] data = control.getSendByte();
        if(control.getModeState() == PatchTracker.MODEL_CMD_INIT){
            LogMgr.d("收到模型类型通知命令");
            Model.initOrDestroyInstance(mHandler,data[1]);
        }else if(control.getModeState() == PatchTracker.MODEL_CMD_MOVE){
            LogMgr.d("收到模型移动命令");
            Model.getInstance().move(data[1],(int)data[2]);
        }else if(control.getModeState() == PatchTracker.MODEL_CMD_FUNCTION){
            LogMgr.d("收到模型功能开关命令");
            boolean onOrOff;
            if(data[2] == Model.FUNCTION_OFF){
                onOrOff = false;
            }else if(data[2] == Model.FUNCTION_ON){
                onOrOff = true;
            }else{
                LogMgr.e("参数错误");
                return;
            }
            Model.getInstance().function(data[1],onOrOff);
        }else if(control.getModeState() == PatchTracker.MODEL_CMD_ACTION){
            LogMgr.d("收到模型功能动作命令");
            Model.getInstance().action(data[1], new Model.ModelCallback() {
                @Override
                public void onActionStart() {

                }
                @Override
                public void onActionStop() {
                    sendModelCallBack(MODE_STATE_ACTION_STOP);
                }
                @Override
                public void onActionRefused() {
                    sendModelCallBack(MODE_STATE_ACTION_STOP);
                }
            });
        }
    }

    private static final int MODE_STATE_ACTION_STOP = 0x10;

    private void sendModelCallBack(int state) {
        Message msg = mHandler.obtainMessage();
        byte[] data = new byte[2];
        data[0] = 0x0E;
        data[1] = (byte) state;
        msg.obj = new Brain(0, data);
        mHandler.sendMessage(msg);
    }

}
