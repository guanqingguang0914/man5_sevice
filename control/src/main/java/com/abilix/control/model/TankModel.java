package com.abilix.control.model;

import android.os.Handler;
import android.support.annotation.IntRange;

import com.abilix.control.ControlInfo;
import com.abilix.control.GlobalConfig;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;


import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yangz on 2017/7/15.
 */

public class TankModel extends Model {

    private boolean isAvoidObstacleAble = false;
    private boolean isDetectObstacle = false;

    public TankModel(Handler handler) {
        super(handler);
    }

    @Override
    public void onCreate() {
        type = MODEL_TYPE_TANK;
        startDetectTimer();
    }

    @Override
    public void onDestroy() {
        stopDetectTimer();
        moveStop();
        super.onDestroy();
    }

    @Override
    public void move(int moveMode, int speed) {
        super.move(moveMode, speed);
        switch (moveMode) {
            case MOVE_STOP:
                moveStop();
                break;
            case MOVE_FORWARD:
                moveForward(speed);
                break;
            case MOVE_BACKWARD:
                moveBackward(speed);
                break;
            case MOVE_LEFT:
                moveLeft(speed);
                break;
            case MOVE_RIGHT:
                moveRight(speed);
                break;
            case MOVE_FORWARD_LEFT:
                moveWithSpeed(35, 65);
                break;
            case MOVE_FORWARD_RIGHT:
                moveWithSpeed(65, 35);
                break;
            case MOVE_BACKWARD_LEFT:
                moveWithSpeed(-35, -65);
                break;
            case MOVE_BACKWARD_RIGHT:
                moveWithSpeed(-65, -35);
                break;
            default:
                break;
        }
    }

    /**
     * 设置左右轮速度
     *
     * @param leftSpeed
     * @param rightSpeed
     */
    private void moveWithSpeed(@IntRange(from = -100, to = 100) int leftSpeed, @IntRange(from = -100, to = 100) int rightSpeed) {
        LogMgr.i("执行前进命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制AD两个电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 + leftSpeed) & 0xFF); //速度值
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) ((100 - rightSpeed) & 0xFF); //速度值
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 前进
     */
    private void moveForward(int speed) {
        LogMgr.i("执行前进命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        if (isDetectObstacle) {
            LogMgr.d("超声检测到障碍,不执行前进");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制AD两个电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 + speed) & 0xFF); //速度值 150
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) ((100 - speed) & 0xFF); //速度值 50
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 后退
     */
    private void moveBackward(int speed) {
        LogMgr.i("执行后退命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制AD两个电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 - speed) & 0xFF); //速度值 50
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) ((100 + speed) & 0xFF); //速度值 150
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 向左
     */
    private void moveLeft(int speed) {
        LogMgr.i("执行向左命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制D电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 - speed) & 0xFF); //速度值 150
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) ((100 - speed) & 0xFF); //速度值 150
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 向右
     */
    private void moveRight(int speed) {
        LogMgr.i("执行向右命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制A电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 + speed) & 0xFF); //速度值 50
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) ((100 + speed) & 0xFF); //速度值 50
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 停止移动
     */
    private void moveStop() {
        LogMgr.i("执行停止命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 9; //控制AD电机 1001
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) (100 & 0xFF); //速度值 100
        cmdData[4] = 0;
        cmdData[5] = 0;
        //电机D
        cmdData[16] = 0; //小电机
        cmdData[17] = 0; //速度
        cmdData[18] = (byte) (100 & 0xFF); //速度值 100
        cmdData[19] = 0;
        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    @Override
    public void function(int functionMode, boolean onOrOff) {
        super.function(functionMode, onOrOff);
        switch (functionMode) {
            case Model.FUNCTION_AVOID_OBSTACLE:
                avoidObstacle(onOrOff);
                break;
            default:
                break;
        }
    }

    /**
     * 避障功能打开或关闭
     *
     * @param turnOnOrOff
     */
    private void avoidObstacle(boolean turnOnOrOff) {
        isAvoidObstacleAble = turnOnOrOff;
    }


    /**
     * 转180度
     */
    private void turnAround() {
        LogMgr.i("执行转180度命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        moveLeft(50);
        isCmdAvaliable = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isCmdAvaliable = true;
                moveStop();
            }
        }, 1200);
    }

    /**
     * 向前移动40CM
     */
    private void moveForward40cm() {
        LogMgr.i("执行向前移动40CM命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        moveForward(50);
        isCmdAvaliable = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isCmdAvaliable = true;
                moveStop();
            }
        }, 2 * 1000);
    }

    private Timer detectTimer;
    private TimerTask detectTimerTask;

    /**
     * 开始监测传感器值
     */
    private void startDetectTimer() {
        stopDetectTimer();
        LogMgr.i("开启传感器值获取");
        detectTimer = new Timer();
        detectTimerTask = new TimerTask() {
            @Override
            public void run() {
                int[] sensorValues = getSensorValues();
                if (isAvoidObstacleAble && sensorValues[5] >= 1 && sensorValues[5] <= 300) {
                    if (!isDetectObstacle) {
                        isDetectObstacle = true;
                        moveStop();
                    }
                } else {
                    isDetectObstacle = false;
                    if (isTouchSensorEnable && (sensorValues[3] > 1000 || sensorValues[4] > 1000 || sensorValues[7] > 1000)) {
                        moveForward40cm();
                    } else {
                        LogMgr.w("当前碰撞传感器无效");
                    }
                }
            }
        };
        detectTimer.schedule(detectTimerTask, 1 * 1000, 200);
    }

    public void stopDetectTimer() {
        LogMgr.i("停止传感器值获取");
        if (detectTimer != null) {
            detectTimer.cancel();
        }
        if (detectTimerTask != null) {
            detectTimerTask.cancel();
        }
    }
}
