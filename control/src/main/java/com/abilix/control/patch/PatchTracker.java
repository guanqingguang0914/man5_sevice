package com.abilix.control.patch;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.IControlKenel;
import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.Control;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class PatchTracker {
    public static final int INIT_CONTROL_TYPE = 6;
    public static final int STM32_SLEEPORWEAK = 7;
    public static final int STM32_TURNOFF = 11;
    public static final int STOP_PLAYMOVEORSOUND = 13;
    public static final int GET_BATTERY = 16;
    public static final int ANDROID_IS_SHUTDONW = 20;
    public static final int SKILL_CREATOR_CMD = 21;
    public static final int CHARGING_MOVE = 22;
    public static final int OPENROBOT_MOVE = 22;
    public static final int OPENROBOT_TOUCH_CSH = 0;
    public static final int CLOSE_GROUPControl = 23;
    public static final int CLOSEROBOT_XIADUN = 1;
    public static final int MODEL_CMD_INIT = 23;
    public static final int MODEL_CMD_MOVE = 24;
    public static final int MODEL_CMD_FUNCTION = 25;
    public static final int MODEL_CMD_ACTION = 26;

    public static final int PATCH_MODE_STATE_SET_STM32_UPDATE_STATE_TRUE = 40;
    public static final int PATCH_MODE_STATE_SET_STM32_UPDATE_STATE_FALSE = 41;
    /**
     * H5开机启动手指保护
     */
    public static final int STARTING_UP_FINGER_PROTECT = 27;
    /**
     * H5执行充电保护下蹲动作
     */
    public static final int CHARGE_PROTECTION_MOVE = 28;
    /**
     * H5执行充电保护起身动作
     */
    public static final int CHARGE_PROTECTION_UP = 29;
    private static PatchTracker instance = null;
    private final static Object mLock = new Object();
    private IControlKenel mIControl;
    private IPatchDisposer mPatchDisposer;
    private HandlerThread doPatchCmdThread;
    private Handler doPatchCmdThreadHandler;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Brain patchBrain = (Brain) msg.obj;
            try {
                mIControl.doPatchCmdCallBack(patchBrain);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static PatchTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                instance = new PatchTracker();
            }
        }
        return instance;
    }

    private PatchTracker() {
        doPatchCmdThread = new HandlerThread("doPatchCmdThread");
        doPatchCmdThread.start();
        doPatchCmdThreadHandler = new DoPatchCmdThreadHandler(doPatchCmdThread.getLooper());
        mPatchDisposer = ControlFactory.createPatchDisposer(mHandler);
    }

    public synchronized void doPatchCmd(Control control, IControlKenel iControl) {
        LogMgr.d("doPatchCmd===>");
        this.mIControl = iControl;
        Message msg = doPatchCmdThreadHandler.obtainMessage();
        msg.obj = control;
        doPatchCmdThreadHandler.sendMessage(msg);
    }
    private byte cmd[] = {(byte) 0xAA,0x55,0x00,0x10,0x00,0x11,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x20};
    private class DoPatchCmdThreadHandler extends Handler {
        public DoPatchCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Control control = (Control) msg.obj;
            if (control == null) {
                return;
            }
            if (control.getModeState() == ANDROID_IS_SHUTDONW) {
                LogMgr.d("Android 即将关机 串口不在写入任何数据");
                SP.write(cmd);
//                ControlApplication.instance.setTurningOff(true);
                return;
            } else if (control.getModeState() == SKILL_CREATOR_CMD) {
                LogMgr.d("执行skillcreator命令");
                String path = control.getFileFullPath();
                int mode = control.getModeState();
                LogMgr.d("执行skillcreator命令 path = " + path + " mode = " + mode);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(path, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_VARIABLE_LENGTH, false, true, null);
                return;
            }else if(control.getModeState() == PATCH_MODE_STATE_SET_STM32_UPDATE_STATE_TRUE){
                LogMgr.d("固件升级程序 即将开始升级");
                SP.setUpdateState(Utils.STM32_STATUS_UPGRADING);
            }else if(control.getModeState() == PATCH_MODE_STATE_SET_STM32_UPDATE_STATE_FALSE){
                LogMgr.d("固件升级程序 升级结束");
                SP.setUpdateState(Utils.STM32_STATUS_NORMAL);
            }

            LogMgr.e("执行patch模块任务：" + control.getControlFuncType());
            switch (control.getControlFuncType()) {
                case STM32_SLEEPORWEAK:
                    STM32Mgr.sleepOrWeak(control,mPatchDisposer);
                    break;
                case STM32_TURNOFF:
                    STM32Mgr.turnOff(control);
                    break;
                case STOP_PLAYMOVEORSOUND:
                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                    break;
                case GET_BATTERY:
                    if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_BRIANC
                            || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S
                            || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H
                            || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M
                            || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3
                            || ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_M3S
                            || ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_M4S
                            || ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9
                            /*|| ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_AF*/) {
                        LogMgr.e("开始从STM32读取电量");
                        STM32Mgr.getBatteryState();
                    }

                    break;

                default:
                    if (mPatchDisposer != null) {
                        mPatchDisposer.DisposeProtocol(control);
                    }
                    break;
            }

        }

    }

}
