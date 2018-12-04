package com.abilix.control.patch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.pad.PadTracker;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.robot.walktunner.GaitAlgorithm;
import com.abilix.robot.walktunner.SensorImuService;

public class SelfProtectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SelfProtect";

    public static final String SelfProtectBroadcastActionName = "com.abilix.control.utils.SelfProtectBroadcast";

    private DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    public void onReceive(Context context, Intent intent) {
        LogMgr.d("intent.getAction()：" + intent.getAction());
        switch (intent.getAction()) {
            case SelfProtectBroadcastActionName:
                LogMgr.d(TAG, "now0 = " + df.format(new Date()));
                if (PlayMoveOrSoundUtils.getInstance().isRobotMoving()) {
                    PlayMoveOrSoundUtils.getInstance().forceStop(true);
                }
                new Thread(new SelfProtectRunnable()).start();
                break;
            case SensorImuService.SENSOR_RESULT:
                LogMgr.e("收到机器身体状态广播");
                if (ControlInfo.getMain_robot_type() != ControlInitiator.ROBOT_TYPE_H3){
                    return;
                }
                int sensor = intent.getIntExtra(SensorImuService.SENSOR_BACK, 0);
                if(sensor == SensorImuService.FRONT_UP){
                    LogMgr.e("收到广播 前起 关闭摔倒检测");
                    if (!ControlApplication.RobotFallCheck){ // 站起来只执行一次
                        LogMgr.e("摔倒检测已关闭 不执行前起动作");
                        return;
                    }
//                    PlayMoveOrSoundUtils.getInstance().isFall = true;
//                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
//                    GaitAlgorithm.getInstance().stopWalk();// 停止步态运动
                    // 前倒
                    ControlApplication.RobotFallCheck = false;
                    String movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_RISE_FRONT;
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, false, 0, false,
                            PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, null);
                } else if (sensor == SensorImuService.BACK_UP){
                    LogMgr.e("收到广播 后起 关闭摔倒检测");
                    if (!ControlApplication.RobotFallCheck){ // 站起来只执行一次
                        LogMgr.e("摔倒检测已关闭 不执行后起动作");
                        return;
                    }
//                    PlayMoveOrSoundUtils.getInstance().isFall = true;
//                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
//                    GaitAlgorithm.getInstance().stopWalk();// 停止步态运动
                    // 后倒
                    ControlApplication.RobotFallCheck = false;
                    String movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_RISE_BACK;
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, false, 0, false,
                            PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, null);
                } else if (sensor == SensorImuService.SENSOR_DOWN){
                    LogMgr.e("收到广播 摔倒状态");
//                    ControlApplication.IsRobotFall = true;
                    if(!ControlApplication.RobotFallCheck){ // 没有开启摔倒检测，不停止动作执行
                        LogMgr.e("没有开启摔倒检测，不停止动作执行");
                        return;
                    }
                    //返回Pad端，告知机器人摔倒状态
                    PadTracker.getInstance().mHandler.obtainMessage(0, ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
                            GlobalConfig.KNOW_ROBOT_OUT_CMD_1,GlobalConfig.KNOW_ROBOT_OUT_CMD_2_H34_ROBOT_STATE,new byte[]{(byte)0x01,(byte)0x01})).sendToTarget();
                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                    GaitAlgorithm.getInstance().stopWalk();// 停止步态运动
                    // 倒了
                } else if (sensor == SensorImuService.SENSOR_UP){
                    LogMgr.e("收到广播 站立状态");
                    // 站起来了
//                    ControlApplication.IsRobotFall = false;
                    ControlApplication.RobotFallCheck = false;
                }
                break;
            default:
                break;
        }

    }

}
