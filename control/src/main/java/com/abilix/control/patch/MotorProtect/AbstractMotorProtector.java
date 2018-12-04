package com.abilix.control.patch.MotorProtect;

import android.os.Handler;
import android.os.Message;

import com.abilix.control.GlobalConfig;
import com.abilix.control.aidl.Brain;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class AbstractMotorProtector implements IMotorProtect {

    private Timer heartBeatTimer;
    private TimerTask heartBeatTimerTask;
    protected byte heartBeatOutCmd1;
    protected byte heartBeatOutCmd2;

    protected byte relieveProtectOutCmd1;
    protected byte relieveProtectOutCmd2;

    protected byte heartBeatInCmd1;
    protected byte heartBeatInCmd2;

    protected byte[] queryMStm32WheelStateCmd;
    protected byte[] stopMotorProtectCmd;

    //	protected byte[] queryMStm32WheelStateCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getChild_robot_type(), heartBeatOutCmd1, heartBeatOutCmd2, null);
//	protected byte[] stopMotorProtectCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getChild_robot_type(), heartBeatOutCmd1, heartBeatOutCmd2, null);
    private int mReceiveWrongHeartbeatCount = 0;

    protected AbstractMotorProtector() {

    }
    @Override
    public void startHeartBeatTimer(final Handler handler) {
        LogMgr.i("startHeartBeatTimer()");
        stopHeartBeatTimer();
        if (GlobalConfig.isMStm32HeartBeatActive) {
            heartBeatTimer = new Timer();
            heartBeatTimerTask = new TimerTask() {
                @Override
                public void run() {
                    askForMWheelState(handler);
                }
            };
            heartBeatTimer.schedule(heartBeatTimerTask, 1000, GlobalConfig.M_STM32_HEART_BEART_TIME * 1000);
        }
    }

    private void stopHeartBeatTimer() {
        if (heartBeatTimer != null) {
            heartBeatTimer.cancel();
        }
        if (heartBeatTimerTask != null) {
            heartBeatTimerTask.cancel();
        }
    }

    public void stopMotorProtect() {
        for (int i = 0; i < 5; i++) {
            try {
                LogMgr.v("stopMotorProtectCmd = " + Utils.bytesToString(stopMotorProtectCmd));
                byte[] buffer = SP.request(stopMotorProtectCmd);
                LogMgr.d("DisposeProtocol() buffer = " + Arrays.toString(buffer));
                boolean isMFeedbackCorrect = ProtocolUtils.isFeedbackCorrect(buffer);
                if (isMFeedbackCorrect) {
                    LogMgr.d("M轮子去除保护处理  成功");
                    break;
                } else {
                    LogMgr.e("M轮子去除保护处理理 返回错误");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 查询M轮子的状态
     */
    private void askForMWheelState(Handler handler) {
        try {
            byte[] receiveData =SP.request(queryMStm32WheelStateCmd);
            if(receiveData==null){
                return;
            }
            LogMgr.v("查询M轮子的状态 = " + Utils.bytesToString(receiveData) + " 查询命令 = " + Utils.bytesToString(queryMStm32WheelStateCmd));
            if (receiveData[0] == GlobalConfig.CMD_0 && receiveData[1] == GlobalConfig.CMD_1 && receiveData[5] == heartBeatInCmd1 && receiveData[6] == heartBeatInCmd2) {
                LogMgr.d("askForMWheelState() 查询M轮子的状态 返回数据正常");
                mReceiveWrongHeartbeatCount = 0;
                if (receiveData[11] == (byte) 0x01) {
                    // 有保护
                    LogMgr.i("当前机器人启动保护");
                    stopHeartBeatTimer();
                    PlayMoveOrSoundUtils.getInstance().forceStop(true);
                    Brain brain = new Brain(2, new byte[]{(byte) 0x00});// 为了防止sendbyte为null时产生Brain接受错误
                    brain.setModeState(11);
                    // PatchTracker.getInstance().sendDataToBrain(brain);
                    Message protectMessage = handler.obtainMessage();
                    protectMessage.obj = brain;
                    handler.sendMessage(protectMessage);
                } else if (receiveData[11] == (byte) 0x00) {
                    // 无保护
                    LogMgr.v("当前机器人无保护");
                } else {
                    LogMgr.e("askForMWheelState() 查询M轮子的状态 返回数据状态异常");
                }
            } else {
                LogMgr.d("askForMWheelState() 查询M轮子的状态 返回数据命令字异常");
//				mReceiveWrongHeartbeatCount++;
//				if (mReceiveWrongHeartbeatCount >= 5) {
//					LogMgr.w("连续5次返回命令错误，尝试进行调整");
//					for (int i = 1; i <= 11; i++) {
//						if (receiveData[i] == GlobalConfig.CMD_0 && receiveData[i + 1] == GlobalConfig.CMD_1 && receiveData[i + 5] == heartBeatInCmd1 && receiveData[i + 6] == heartBeatInCmd2) {
//							byte[] tempData = new byte[i];
//							SP.read(tempData);
//							LogMgr.d("连续5次返回命令错误，进行调整 i = " + i);
//							mReceiveWrongHeartbeatCount = 0;
//							break;
//						}
//					}
//					mReceiveWrongHeartbeatCount = 0;
//				}
            }
        } catch (Exception e) {
            LogMgr.e("askForMWheelState() 查询M轮子的状态异常");
            e.printStackTrace();
        }

    }

}
