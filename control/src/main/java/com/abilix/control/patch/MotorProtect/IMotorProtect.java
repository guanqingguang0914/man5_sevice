package com.abilix.control.patch.MotorProtect;

import android.os.Handler;

public interface IMotorProtect {
    void startHeartBeatTimer(final Handler handler);

    void stopMotorProtect();
}
