package com.abilix.control.patch.MotorProtect;

import com.abilix.control.ControlInfo;
import com.abilix.control.GlobalConfig;
import com.abilix.control.protocol.ProtocolUtils;

import android.os.Handler;

public class M1MotorProtector extends AbstractMotorProtector {
    public M1MotorProtector() {
        heartBeatOutCmd1 = GlobalConfig.M1_STM32_HEART_BEAT_OUT_CMD_1;
        heartBeatOutCmd2 = GlobalConfig.M1_STM32_HEART_BEAT_OUT_CMD_2;
        heartBeatInCmd1 = GlobalConfig.M1_STM32_HEART_BEAT_IN_CMD_1;
        heartBeatInCmd2 = GlobalConfig.M1_STM32_HEART_BEAT_IN_CMD_2;
        relieveProtectOutCmd1 = GlobalConfig.M1_STM32_RELIEVE_PROTECT_OUT_CMD_1;
        relieveProtectOutCmd2 = GlobalConfig.M1_STM32_RELIEVE_PROTECT_OUT_CMD_2;
        queryMStm32WheelStateCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getChild_robot_type(), heartBeatOutCmd1, heartBeatOutCmd2, null);
        stopMotorProtectCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getChild_robot_type(), relieveProtectOutCmd1, relieveProtectOutCmd2, new byte[]{0x00});
    }

    @Override
    public void startHeartBeatTimer(Handler handler) {
        // TODO Auto-generated method stub
        super.startHeartBeatTimer(handler);
    }

    @Override
    public void stopMotorProtect() {
        // TODO Auto-generated method stub
        super.stopMotorProtect();
    }
}
