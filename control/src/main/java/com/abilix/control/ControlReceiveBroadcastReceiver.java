package com.abilix.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.Control;
import com.abilix.control.pad.PadTracker;
import com.abilix.control.patch.PatchTracker;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sp.SP;
import com.abilix.control.upgrade.UpgradeTracker;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.SharedPreferenceTools;

import java.util.Locale;

public class ControlReceiveBroadcastReceiver extends BroadcastReceiver implements IControlKenel {
    public static final String ACTION_STM32_VERSION_QUERY = "action_stm32_version_query";
    public static final String ACTION_SERVO_VERSION_QUERY = "action_servo_version_query";
    public static final String BROADCAST_ACTION_LOG = "com.abilix.change_log_status";
    public static final String BROADCAST_LOG_STATUS = "log_status";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogMgr.v("receive broadcast action::" + intent.getAction());
        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED:
                LogMgr.d("charge state::connected");
                SP.setChargeProtectState(true);
//                handleH3H4CharingState();
                break;

            case Intent.ACTION_POWER_DISCONNECTED:
                LogMgr.d("charge state::disconnected");
                SP.setChargeProtectState(false);
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                LogMgr.v(String.format(Locale.US, "battery state:: battery status[%d] plugged[%d]", status, plugged));
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);//EXTRA_LEVEL
                SharedPreferenceTools.saveInt(context, SharedPreferenceTools.SHAREDPREFERENCE_KEY_ELECTRICITY, level);
                if ((status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL) && plugged == BatteryManager
                        .BATTERY_PLUGGED_AC) {
                    SP.setChargeProtectState(true);
//                    handleH3H4CharingState();
                }
                break;
            case ControlReceiveBroadcastReceiver.ACTION_SERVO_VERSION_QUERY:
                LogMgr.d("receive broadcast to query servo version");
                UpgradeTracker.getInstance().saveSERVOVersion();
                break;
            case ControlReceiveBroadcastReceiver.ACTION_STM32_VERSION_QUERY:
                LogMgr.d("receive broadcast to query stm32 version");
                UpgradeTracker.getInstance().saveSTMVersion();
                break;
            case ControlReceiveBroadcastReceiver.BROADCAST_ACTION_LOG:
                Boolean log_status = intent.getBooleanExtra(BROADCAST_LOG_STATUS, false);
                LogMgr.d("InstallReceiver onReceive() action = " + intent.getAction() + " log_status = " + log_status);
                if (log_status) {
                    LogMgr.setLogLevel(LogMgr.VERBOSE);
                } else {
                    LogMgr.setLogLevel(LogMgr.NOLOG);
                }
                break;
        }

    }

    private synchronized void handleH3H4CharingState() {
        LogMgr.d("main robot type:" + ControlInfo.getMain_robot_type());
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            byte[] turnoff_cmd = {(byte) 0x00, (byte) 0x00, (byte) 0x00};
            byte[] turnoff_led = ProtocolBuilder.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                    ProtocolBuilder.CMD_H3H4_LED, turnoff_cmd);
            Control control = new Control(13, null);
            PatchTracker.getInstance().doPatchCmd(control, this);
            for (int i = 0; i < 10; i++) {
                PadTracker.getInstance().doPadCmd(turnoff_led, this);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void dispatchCmd(Control control) {
        // TODO Auto-generated method stub

    }

    @Override
    public void responseCmdToBrain(Brain brain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void responseCmdToUpgrade(ResponseBean responseBean) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPadCmdCallBack(byte[] buff_resposne) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doScratchCmdCallBack(byte[] buff_resposne) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doScratchCmdCallBack(Brain scratchBrain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doSkillPlayerCmdCallBack(byte[] buff_resposne) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doLearnLetterCmdCallBack(byte[] buff_resposne) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doVjcCmdCallBack(Brain vjcBrain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doSoulCmdCallBack(byte[] buff_resposne) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doUpgradeCmdCallBack(ResponseBean responseBean) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDestory() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPatchCmdCallBack(Brain patchBrain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispatchSkillPlayerCmd(int state, String filePath) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] serialWrite(byte[] data) {
        // TODO Auto-generated method stub
        return null;
    }
}
