package com.abilix.control;

import android.content.Intent;

import com.abilix.control.utils.LogMgr;

/**
 * @author jingh
 * @Descripton:所有从Control通过广播发到外面的指令都走这里
 * @date2017-3-30上午10:09:39
 */
public class BroadcastResponder {
    public static final byte UPGRADE_SUCESS = 0X01;
    public static final byte UPGRADE_FAILED = 0X00;

    private static final String BROADCAST_ACTION_STM32_UPDATE_PROGRESS = "com.abilix.control.STM32_UPDATE_PROGRESS";
    private static final String BROADCAST_ACTION_EXTRA_NAME_STM32_UPDATE_PROGRESS = "stm32_update_progress";
    public static final String BROADCAST_ACTION_VIDEO_CONTROL = "com.abilix.control.VEDIO_CONTROL";
    public static final String BROADCAST_EXTRA_VIDEO_CONTROL_STATE = "video_control_state";
    public static final String BROADCAST_EXTRA_VIDEO_CONTROL_PATH = "video_control_path";

    public void sendStm32UpdateStateBroadCastToService(byte b, String upgradeFilePath) {
        Intent sendIntent = new Intent("com.abilix.control.STM32_SOFTWARE_UPDATE_STATE");
        sendIntent.putExtra("state", b);
        sendIntent.putExtra("apkname", upgradeFilePath);
        ControlApplication.instance.sendBroadcast(sendIntent);
        LogMgr.d("send upgrade result to upgrade apk");
    }

    public void sendRobotInfoBroadCastToService(Integer version, String type) {
        Intent sendIntent = new Intent("com.abilix.control.STM32_SOFTWARE_VERSION_RSP");
        sendIntent.putExtra("version", version);
        sendIntent.putExtra("type", type);
        ControlApplication.instance.sendBroadcast(sendIntent);
        LogMgr.d("send stm version to upgrade apk");
    }
    public static void sendServoVersionToBrainset(Integer version) {
        Intent sendIntent = new Intent("com.abilix.servo");
        sendIntent.putExtra("servo", version);
        ControlApplication.instance.sendBroadcast(sendIntent);
        LogMgr.d("send servo version to brainset");
    }
    /**
     * stm32升级进度广播
     * @param progress 进度
     */
    public static void sendStm32UpdateProgress(int progress){
        LogMgr.d("sendStm32UpdateProgress progress = "+progress);
        Intent sendIntent = new Intent(BROADCAST_ACTION_STM32_UPDATE_PROGRESS);
        sendIntent.putExtra(BROADCAST_ACTION_EXTRA_NAME_STM32_UPDATE_PROGRESS,progress);
        ControlApplication.instance.sendBroadcast(sendIntent);
    }

    public static void sendVedioControl(int videoControlState, String videoPath){
        LogMgr.d("发送广播控制视屏");
        Intent sendIntent = new Intent(BROADCAST_ACTION_VIDEO_CONTROL);
        sendIntent.putExtra(BROADCAST_EXTRA_VIDEO_CONTROL_STATE,videoControlState);
        sendIntent.putExtra(BROADCAST_EXTRA_VIDEO_CONTROL_PATH,videoPath);
        ControlApplication.instance.sendBroadcast(sendIntent);
    }
}
