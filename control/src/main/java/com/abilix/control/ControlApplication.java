package com.abilix.control;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.lang.Thread.UncaughtExceptionHandler;

public class ControlApplication extends android.app.Application {
    public static ControlApplication instance;

    /**
     * 摔倒检测：false关闭检测，true打开检测
     */
    public static boolean RobotFallCheck = false;
    public static boolean IsRobotFall = false;

    /**
     * 当前Android是否开始关机
     */
    private boolean isTurningOff = false;

    public boolean isTurningOff() {
        return isTurningOff;
    }

    public void setTurningOff(boolean isTurningOff) {
        this.isTurningOff = isTurningOff;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if(!Utils.appIsDebugable(instance)){
            LogMgr.setLogLevel(LogMgr.NOLOG);
        }
        //判断重启后更新状态
        if (Utils.getStm32UpgradeStatus() == Utils.STM32_STATUS_UPGRADING) {
            Utils.setStm32UpgradeStatus(Utils.STM32_STATUS_BOOTLOADER);
        }
        // Control相关初始化
        ControlInitiator.init();
        uncatchExecptionForLog();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LogMgr.stopExportLog();
    }

    // 拦截不可捕获的异常，保存到Log日志里
    private void uncatchExecptionForLog() {
        // 拦截异常
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                throwable.printStackTrace();
                LogMgr.e("Control已经崩溃！！！！！崩溃原因：" + throwable.toString());
            }
        });
    }

}
