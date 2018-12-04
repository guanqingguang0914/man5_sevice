package com.abilix.control.model;

import android.os.Handler;

import com.abilix.control.ControlInfo;
import com.abilix.control.GlobalConfig;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.vedio.Player;

import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.mode;

/**
 * Created by yangz on 2017/7/15.
 */

public class ScorpionModel extends Model {

    private static final String MUSIC_SOFT_ATTACK = "model_scorpion_soft_attack.mp3";
    private static final String MUSIC_ATTACK = "model_scorpion_attack.mp3";
    private static final String MUSIC_ALERT = "model_scorpion_alert.mp3";

    private boolean isAutoAttackAble = false;
    private boolean isDetectObstacle = false;
    private Timer detectTimer;
    private TimerTask detectTimerTask;
    protected Player mPlayer;

    public ScorpionModel(Handler handler) {
        super(handler);
    }

    @Override
    public void onCreate() {
        type = MODEL_TYPE_SCORPION;
        startDetectTimer();
    }

    @Override
    public void onDestroy() {
        stopDetectTimer();
        moveStop(true);
        super.onDestroy();
    }

    @Override
    public void move(int moveMode, int speed) {
        LogMgr.d("move mode:" + mode);
        super.move(moveMode, speed);
        switch (moveMode) {
            case MOVE_FORWARD:
                moveForward(speed, false);
                break;
            case MOVE_BACKWARD:
                moveBackward(speed, true);
                break;
            case MOVE_STOP:
                moveStop(false);
                break;
            default:
                break;
        }
    }

    /**
     * 前进  设置A端口电机以50的速度前进
     */
    private void moveForward(int speed, boolean isInnerCmd) {
        LogMgr.i("执行前进命令");
        if (!isCmdAvaliable && !isInnerCmd) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        if (isDetectObstacle) {
            LogMgr.d("超声检测到障碍,不执行前进");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 8; //控制A电机 1000
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 + speed) & 0xFF); //速度值 150
        cmdData[4] = 0;
        cmdData[5] = 0;
//        //电机D
//        cmdData[16] = 0; //小电机
//        cmdData[17] = 0; //速度
//        cmdData[18] = (byte) (50&0xFF); //速度值 50
//        cmdData[19] = 0;
//        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 后退
     */
    private void moveBackward(int speed, boolean isInnerCmd) {
        LogMgr.i("执行后退命令");
        if (!isCmdAvaliable && !isInnerCmd) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 8; //控制A电机 1000
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) ((100 - speed) & 0xFF); //速度值 50
        cmdData[4] = 0;
        cmdData[5] = 0;
//        //电机D
//        cmdData[16] = 0; //小电机
//        cmdData[17] = 0; //速度
//        cmdData[18] = (byte) (150&0xFF); //速度值 150
//        cmdData[19] = 0;
//        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    /**
     * 停止移动
     *
     * @param isInnerCmd
     */
    private void moveStop(boolean isInnerCmd) {
        LogMgr.i("执行停止命令");
        if (!isCmdAvaliable && !isInnerCmd) {
            LogMgr.w("当前机器不接收命令");
            return;
        }
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 8; //控制A电机 1000
        //电机A
        cmdData[1] = 0; //小电机
        cmdData[2] = 0; //速度
        cmdData[3] = (byte) (100 & 0xFF); //速度值 100
        cmdData[4] = 0;
        cmdData[5] = 0;
//        //电机D
//        cmdData[16] = 0; //小电机
//        cmdData[17] = 0; //速度
//        cmdData[18] = (byte) (100&0xFF); //速度值 100
//        cmdData[19] = 0;
//        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }

    @Override
    public void function(int functionMode, boolean onOrOff) {
        super.function(functionMode, onOrOff);
        switch (functionMode) {
            case Model.FUNCTION_AUTO_ATTACK:
                setAutoAttack(onOrOff);
                break;
            default:
                break;
        }
    }

    @Override
    public void action(int actionType, ModelCallback callback) {
        super.action(actionType, callback);
        switch (actionType) {
            case ACTION_ATTACK:
                attack(false);
                break;
            case ACTION_RELAX:
                relax();
                break;
            case ACTION_ALERT:
                softAttack(false);
                break;
            case ACTION_ROAR:
                roaringAction();
                break;
            default:
                break;
        }
    }

    private void relax() {
        LogMgr.i("执行放松命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            if (mCallback != null) {
                mCallback.onActionRefused();
                mCallback = null;
            }
            return;
        }
        isCmdAvaliable = false;
        if (mCallback != null) {
            mCallback.onActionStart();
        }
        //设置电机B正转80 等待1秒
        setEngineBSpeed(180);
        // 反转10 等待0.1秒
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(90);
            }
        }, 1000);
        //前进两步 等待2S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(100);
                moveForward(50, true);
            }
        }, 1000 + 100);
        //后退两步，等待2S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveBackward(50, true);
            }
        }, 1000 + 100 + 2000);
        //停止
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveStop(true);
                isCmdAvaliable = true;
                if (mCallback != null) {
                    mCallback.onActionStop();
                    mCallback = null;
                }
            }
        }, 1000 + 100 + 2000 + 2000);
    }

    /**
     * 试探攻击
     */
    private void softAttack(final boolean isAuto) {
        LogMgr.i("执行警惕命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            if (mCallback != null && !isAuto) {
                mCallback.onActionRefused();
                mCallback = null;
            }
            return;
        }
        isCmdAvaliable = false;
        if (mCallback != null && !isAuto) {
            mCallback.onActionStart();
        }
        if (mPlayer == null) {
            mPlayer = new Player();
        }
        mPlayer.play(MUSIC_SOFT_ATTACK);
        //设置电机B正转30 等待0.1秒
        setEngineBSpeed(130);
        // 反转50 等待0.2秒
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(50);
            }
        }, 100);
        //正转50 等待1S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(150);
            }
        }, 100 + 200);
        //反转10等待0.2S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(90);
            }
        }, 100 + 200 + 1000);
        //停止
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(100);
                isCmdAvaliable = true;
                if (mCallback != null && !isAuto) {
                    mCallback.onActionStop();
                    mCallback = null;
                }
            }
        }, 100 + 200 + 1000 + 200);
    }

    /**
     * 警告: 仅播放咆哮声音
     */
    private void roaringAction() {
        LogMgr.i("执行咆哮命令");
        if (mPlayer == null) {
            mPlayer = new Player();
        }
        mPlayer.play(MUSIC_ALERT);
    }

    /**
     * 自动攻击功能打开或关闭
     *
     * @param turnOnOrOff
     */
    private void setAutoAttack(boolean turnOnOrOff) {
        isAutoAttackAble = turnOnOrOff;
    }

    /**
     * 攻击 设置电机B正转30 等待0.1秒  反转100 等待0.5秒 正转60 等待1S  反转10等待0.2S
     */
    private void attack(final boolean isAuto) {
        LogMgr.i("执行攻击命令");
        if (!isCmdAvaliable) {
            LogMgr.w("当前机器不接收命令");
            if (mCallback != null && !isAuto) {
                mCallback.onActionRefused();
                mCallback = null;
            }
            return;
        }
        isCmdAvaliable = false;
        if (mCallback != null && !isAuto) {
            mCallback.onActionStart();
        }
        if (mPlayer == null) {
            mPlayer = new Player();
        }

        mPlayer.play(MUSIC_ATTACK);
        //设置电机B正转30 等待0.1秒
        setEngineBSpeed(130);
        // 反转100 等待0.5秒
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(0);
            }
        }, 100);
        //正转60 等待1S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(160);
            }
        }, 100 + 500);
        //反转10等待0.2S
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(90);
            }
        }, 100 + 500 + 1000);
        //停止
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setEngineBSpeed(100);
                isCmdAvaliable = true;
                if (mCallback != null && !isAuto) {
                    mCallback.onActionStop();
                    mCallback = null;
                }
            }
        }, 100 + 500 + 1000 + 200);
    }

    /**
     * 设置B电机速度
     *
     * @param speedOfEngineB
     */
    private void setEngineBSpeed(int speedOfEngineB) {
        byte[] cmdData = new byte[21];
        cmdData[0] = (byte) 4; //控制B电机 0100
        //电机B
        cmdData[6] = 0; //小电机
        cmdData[7] = 0; //速度
        cmdData[8] = (byte) (speedOfEngineB & 0xFF); //速度值 speedOfEngineB
        cmdData[9] = 0;
        cmdData[10] = 0;
//        //电机D
//        cmdData[16] = 0; //小电机
//        cmdData[17] = 0; //速度
//        cmdData[18] = (byte) (100&0xFF); //速度值 100
//        cmdData[19] = 0;
//        cmdData[20] = 0;
        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, cmdData);
        SP.request(cmd);
    }


//    private void stopAttack(){
//        isStop=true;
//        setMotorBSpeed(0);
//    }


//    private boolean isStop=false;
//    private void wait(int time){
//        int sleepnum = (int) (time/ 10);
//        int n = 0;
//        while (isStop & n < sleepnum) {
//            try {
//                Thread.sleep(100);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            n++;
//        }
//    }


    /**
     * 开始监测传感器值
     */
    private void startDetectTimer() {
        LogMgr.e("开始传感器检测");
        stopDetectTimer();
        detectTimer = new Timer();
        detectTimerTask = new TimerTask() {
            @Override
            public void run() {
                int[] sensorValues = getSensorValues();
                if (isAutoAttackAble && sensorValues[1] >= 1 && sensorValues[1] <= 200) {
                    isDetectObstacle = true;
                    //超声 试探攻击 + 声音
                    softAttack(true);
                } else {
                    isDetectObstacle = false;
                    //触摸状态
                    if (isTouchSensorEnable && sensorValues[2] > 1000) {
                        //头部触摸 攻击 + 声音
                        attack(true);
                    } else if (isTouchSensorEnable && sensorValues[3] > 1000) {
                        //尾部触摸 声音
                        if (mPlayer == null) {
                            mPlayer = new Player();
                        }
                        mPlayer.play(MUSIC_ALERT);
                    } else {
                        //LogMgr.w("当前碰撞传感器无效");
                    }
                }
            }
        };
        detectTimer.schedule(detectTimerTask, 1 * 1000, 200);
    }

    public void stopDetectTimer() {
        if (detectTimer != null) {
            detectTimer.cancel();
        }
        if (detectTimerTask != null) {
            detectTimerTask.cancel();
        }
    }


}
