package com.abilix.control.patch;

import android.os.Handler;

import com.abilix.control.aidl.Control;
import com.abilix.control.patch.MotorProtect.IMotorProtect;
import com.abilix.control.patch.MotorProtect.M1MotorProtector;
import com.abilix.control.utils.LogMgr;

public class M1PatchDisposer extends AbstractPatchDisposer {
    public static final int MOTRO_HEART_BEAT = 2;
    public static final int MOTRO_PROTECT_STOP = 1;
    private IMotorProtect mMotorProtector;

    public M1PatchDisposer(Handler mHandler) {
        super(mHandler);
        mMotorProtector = new M1MotorProtector();
    }

    @Override
    public void DisposeProtocol(Control control) {
        int modeState = control.getModeState();
        LogMgr.d("modeState = " + modeState);
        switch (modeState) {
            case MOTRO_PROTECT_STOP:
                // ModeState 1:通知control M 去除掉轮子保护
                LogMgr.d("ModeState 1:通知control M 去除掉轮子保护");
                if (mMotorProtector != null) {
                    mMotorProtector.stopMotorProtect();
                }
                mMotorProtector.startHeartBeatTimer(mHandler);
                break;

            case MOTRO_HEART_BEAT:
                if (mMotorProtector != null) {
                    mMotorProtector.startHeartBeatTimer(mHandler);
                }
                break;

            default:
                break;
        }

    }

}
