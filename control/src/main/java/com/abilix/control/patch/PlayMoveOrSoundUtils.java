package com.abilix.control.patch;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.IntRange;
import android.text.TextUtils;
import android.util.Log;

import com.abilix.control.BroadcastResponder;
import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.ControlKernel;
import com.abilix.control.GlobalConfig;
import com.abilix.control.aidl.Brain;
import com.abilix.control.process.ProcessControl;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.Player;
import com.abilix.control.wisdom.ProcedureControl;
import com.abilix.moco.gaitx.kernal.execute.GaitAlgorithmForH5;
import com.abilix.moco.gaitx.kernal.execute.SensorImuServiceForH5;
import com.abilix.robot.walktunner.SensorImuService;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * H,M机器人。解析，播放动作bin文件，同时控制音频/视频的播放。
 *
 * @author Yang
 */
public class PlayMoveOrSoundUtils implements OnCompletionListener {

    // private static final String TAG = "PlayMoveOrSoundUtils";
    private static final float DISTANCE_PER_STEP = 0.219162f;// 单位mm M轮子的最小计量长度
    private boolean isFirstSetTimeSlack;
    private static final int MOVE_TYPE_ACTION = 0x01;
    private static final int MOVE_TYPE_SOUND = 0x02;
    private static final int MOVE_TYPE_BOTH = 0x03;

    private static final float UPPER_LIMIT_COEFFICIENT_COMPARE_DISPLACEMENT = 1.2f;
    private static final float LOWER_LIMIT_COEFFICIENT_COMPARE_DISPLACEMENT = 0.8f;
    private static final float RAISE_SPEED_COEFFICIENT = 1.2f;
    private static final float CUT_SPEED_COEFFICIENT = 0.8f;

    public static final int PLAY_MODE_DEFAULT = -1;
    public static final int PLAY_MODE_NORMAL = 0;
    public static final int PLAY_MODE_LIST = 1;
    public static final int PLAY_MODE_VARIABLE_LENGTH = 2;
    public static final int PLAY_MODE_VEDIO = 4;

    public static final int VIDEO_CONTROL_STOP = 0;
    public static final int VIDEO_CONTROL_PLAY = 1;
    public static final int VIDEO_CONTROL_PAUSE = 2;
    public static final int VIDEO_CONTROL_RESUME = 3;

    private static final boolean isSpeedUpH = true;

    private static PlayMoveOrSoundUtils instance = null;

    private Player mPlayer;
    private Handler mHandlerForReturn;
    private byte mRobotType;

//    public boolean isFall =false; // 是否摔倒，false没有摔倒，true摔倒

    public void setmHandlerForReturn(Handler mHandlerForReturn) {
        this.mHandlerForReturn = mHandlerForReturn;
    }

    private PlayCallBack mPlayCallBack;

    public void setmPlayCallBack(PlayCallBack mPlayCallBack) {
        this.mPlayCallBack = mPlayCallBack;
    }

    private boolean mIsMObstacleAvoidanceOpen = true;
    public void setmIsMObstacleAvoidanceOpen(boolean mIsMObstacleAvoidanceOpen) {
        this.mIsMObstacleAvoidanceOpen = mIsMObstacleAvoidanceOpen;
    }

    private HandlerThread handlerThreadForH5;
    private Handler handlerForH5;

    /**
     * 播放模式
     */
    private int mPlayMode = PLAY_MODE_DEFAULT;

    /**
     * 播放动作列表
     */
    private String[] mPlayMoveList;

    /**
     * 变帧长bin文件中的所有动作列表
     */
    private byte[][] mPlayActionList;
    /**
     * 变帧长bin文件中的所有动作所需时间列表
     */
    private int[] mPlayActionDuration;

    /**用于处理M动作帧下发的线程池*/
    private ThreadPoolExecutor mMFramethreadPoolExecutor;

    public void setPlayMoveList(String[] playMoveList) {
        this.mCurrentCountInList = 0;
        this.mPlayMoveList = playMoveList;
    }

    /**
     * 当前动作在列表中的编号
     */
    private int mCurrentCountInList;

    /**
     * 当前是否在动作中
     */
    private boolean isRobotMoving = false;
    /**
     * 当前是否在恢复初态位置动作中
     */
    private boolean isRecovering = false;
    /**
     * 前一个动作是否是强制停止的
     */
    private boolean isForceStoped = false;

    public boolean isRobotMoving() {
        return isRobotMoving;
    }

    /**
     * 当前动作是否循环
     */
    private boolean isRobotMoveLooping = false;
    /**
     * 当前循环的次数
     */
    private int loopCount = 0;
    /**
     * 当前是否在一个动作的暂停状态
     */
    private boolean isPaused = false;

    /**
     * 是否在下一个动作停止下来
     */
    private boolean isNextMoveStop = false;
    /**
     * 当前机器人动作是否处于初始状态
     */
    private boolean isRobotInitial = true;
    /**
     * 机器人上一个动作类型
     */
    private int lastRobotMoveType = 0;
    /**
     * 机器人上一个动作名
     */
    private String lastRobotAction = null;
    /**
     * 机器人上一个音频名
     */
    private String lastRobotSound = null;
    /**
     * 当前是否在执行停止动作
     */
    private boolean isStopMoveDoing = false;
    /**
     * 当前是否需要向APP端回复状态
     */
    private boolean isNeedToReply = false;
    /**H5,H3是否使用步态调整*/
    private boolean isUseAutoBalance = false;
    /**
     * 下次的停止动作命令
     */
    private String stopRobotMoveCmd = null;
    /**
     * 下次的停止命令的第一帧后的延迟时间
     */
    private int stopRobotMoveDelayTime = 0;

    /**
     * 存储文件数据
     */
    private byte[] mFileBuff = null;
    /**
     * 文件总长度
     */
    private long mFileLength = 0;
    /**
     * 当前解析位置
     */
    private int mCurrentPosition = 0;
    /**
     * 判断是否动作文件第一帧 用于设置
     */
    private boolean mIsFirst = true;
    /**
     * 判断是否动作文件第一帧 用于设置往串口传的命令列表初始值
     */
    private boolean mIsFirstFrame = true;
    /**
     * 从bin文件中解析 判断动作是否可用
     */
    private boolean isMoveEnable = true;
    /**
     * 从bin文件中解析 判断灯光是否可用
     */
    private boolean isLightEnable = false;
    /**
     * 从bin文件中解析 判断手脚灯光是否可用
     */
    private boolean isLightInHandFootEnable = false;
    /**
     * 总帧数
     */
    private int mTotalFrameNum = 0;
    /**bin文件版本*/
    private int mVersion = 0;
    /**
     * 第一帧的延迟时间
     */
    private int mDelayTime = 0;
    /**
     * 当前帧序号
     */
    private int mCurrentFrameNum;
    /**
     * M机器人16个眼睛LED灯光数据
     */
    private byte[] mEyeLightData = new byte[48];
    /**
     * M1机器人10个眼睛LED灯光数据
     */
    private byte[] m1EyeLightData = new byte[30];

    /**
     * 控制往下发指令的timer
     */
    private Timer mTimer;
    private TimerTask mTimerTask;
    /**
     *  控制下发指令的线程池
     * */
    private ScheduledExecutorService scheduledExecutorService;
    /**
     * 监听H机器人是否恢复到初始位置的Timer
     */
    private Timer mRecoverTimer;
    private TimerTask mRecoverTimerTask;
    private byte[] getHCurrent22EngineAngleCMD;
    /**
     * 恢复初态时读舵机角度的次数
     */
    private int mCountOfReadEngineAngle = 0;
    /**
     * 恢复初态时读舵机角度的次数上限 100毫秒读一次
     */
    private final int mMaxCountOfReadEngineAngle = 5 * 10;
    /**
     * H恢复初态的舵机速度
     */
    private final int mHRecoverSpeed = 60;
    /**当前播放的帧数 每125帧重新置零，然后跳过一帧，用于与音乐的同步效果*/
    private int countOfPlayedFrame = 0;
    /**播放H动作时，如果使用播放一帧重复4帧，每梁镇之间间隔8毫秒的方式，使用此值进行帧数计数*/
    private int countOfFrameIncludeRepeat = 0;

    private Handler mHandler;
    // private final int SEND_TIMER = 0x11;
    /**
     * H机器人舵机个数
     */
    private int mServoCount = 23;
    /**
     * S机器人舵机个数 lz 2017-4-24 11:13:06 S系列之后都是用22个舵机的bin文件
     */
    private int sServoCount = 22;
    // /** M机器人舵机个数*/
    // private int mServoCountInM = 4;
    /**
     * 1秒钟发送的帧数
     */
    private final int framesInH = 24;
    private final int framesInM = 24;
    private final int framesInC = 24;
    /**
     * 新增S系列 lz 2017-3-23
     */
    private final int framesInS = 24;
    /**
     * 1帧所需要的毫秒数
     */
    private int mMillisecondPerFrame;
    /**
     * 每多少帧下发一次颜色数据
     */
    private int ColorSpeed = 1;

    /**
     * M机器人一帧的左轮期望位移
     */
    private float mExpectLeftWheelDisplacement;
    /**
     * M机器人一帧的右轮期望位移
     */
    private float mExpectRightWheelDisplacement;
    /**
     * M机器人一帧的左轮实际位移累加值
     */
    private float mActualLeftWheelDisplacement;
    /**
     * M机器人一帧的右轮实际位移累加值
     */
    private float mActualRightWheelDisplacement;
    /**
     * 获取实际位移的timer
     */
    private Timer mDisplacementTimer;
    private TimerTask mDisplacementTimerTask;
    private byte[] mGetActualDisplacementCMD;

    /**
     * M机器人一帧行进到四分之三时的左轮期望位移
     */
    private float mExpectLeftWheelPartDisplacement;
    /**
     * M机器人一帧行进到四分之三时的右轮期望位移
     */
    private float mExpectRightWheelPartDisplacement;
    /**
     * M机器人一帧行进到四分之三时的左轮实际位移
     */
    private float mActualLeftWheelPartDisplacement;
    /**
     * M机器人一帧行进到四分之三时的右轮实际位移
     */
    private float mActualRightWheelPartDisplacement;
    /**
     * 是否需要调整左轮下一帧速度
     */
    private boolean mIsNeedToAdjustSpeedForLeftWheel;
    /**
     * 是否需要调整右轮下一帧速度
     */
    private boolean mIsNeedToAdjustSpeedForRightWheel;
    /**
     * 左轮下一帧速度调整系数
     */
    private float mNextCoefficientForLeftWheel;
    /**
     * 右轮下一帧速度调整系数
     */
    private float mNextCoefficientForRightWheel;

    // C系列
    /**
     * C系列左轮速度
     */
    private int mLeftWheelSpeedForC;
    /**
     * C系列右轮速度
     */
    private int mRightWheelSpeedForC;
    /**
     * C系列肩膀角度
     */
    private int mShoulderAngleForC;
    /**
     * C系列前一帧肩膀角度
     */
    private int mLastShoulderAngleForC;

    /**
     * C系列前一次下发三电机角度时的帧数
     */
    // private int mLastFrameForNeck;
    public void setColorSpeed(int colorSpeed) {
        ColorSpeed = colorSpeed;
    }

    private byte iCount;
    private byte[] pID;

    /**
     * 解析完H的一帧之后，往串口1发的手脚灯光变化命令的列表
     */
    private List<byte[]> cmdToSendToEngine1List;
    private byte lastLeftHandTurnOnOrOff;
    private byte lastRightHandTurnOnOrOff;
    private byte lastLeftFeetTurnOnOrOff;
    private byte lastRightFeetTurnOnOrOff;

    // M系列

    private byte lastLeftWheelSpeed;
    private byte lastRightWheelSpeed;
    private byte lastHeadVerticalAngle;
    private byte lastHeadHorizenAngle;

    private byte lastEyeColorRed;
    private byte lastEyeColorGreen;
    private byte lastEyeColorBlue;

    private byte lastNeckColorRed;
    private byte lastNeckColorGreen;
    private byte lastNeckColorBlue;

    private byte lastBottomColorRed;
    private byte lastBottomColorGreen;
    private byte lastBottomColorBlue;

    private byte[] wheelSpeedCmd;
    private byte[] headVerticalCmd;
    private byte[] headHorizenCmd;
    private byte[] eyesColorCmd;
    private byte[] neckColorCmd;
    private byte[] bottomColorCmd;

    private int sonCount = 0;
    /**
     * 解析完M的一帧之后，往串口发的所有命令的列表
     */
    private List<byte[]> mDownMCmd = new ArrayList<byte[]>();

    // public ArrayList<String> stopRobotMoveCmdList = null;

    @SuppressLint("HandlerLeak")
    private PlayMoveOrSoundUtils() {

        // if(GlobalConfig.CONTROL_TYPE == GlobalConfig.ROBOT_TYPE_H){
        // mMillisecondPerFrame = 1000/framesInH;
        // }else if(GlobalConfig.CONTROL_TYPE == GlobalConfig.ROBOT_TYPE_M){
        // mMillisecondPerFrame = 1000/framesInM;
        // }
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
            mMillisecondPerFrame = 1000 / framesInM + 1;
            mGetActualDisplacementCMD = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_M,
                    GlobalConfig.WHEEL_ACTUAL_DISPLACEMENT_OUT_CMD_1, GlobalConfig.WHEEL_ACTUAL_DISPLACEMENT_OUT_CMD_2,
                    null);
            mRobotType = ControlInitiator.ROBOT_TYPE_M;
            if(mMFramethreadPoolExecutor ==null){
                mMFramethreadPoolExecutor = new ThreadPoolExecutor(0,2,60, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),
                        Executors.defaultThreadFactory(),new ThreadPoolExecutor.DiscardPolicy());
            }
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
            iCount = (byte) mServoCount; // 23
            pID = new byte[mServoCount];
            for (int n = 0; n < mServoCount; n++) {
                pID[n] = (byte) (n); // 0~22
            }
//            if(isSpeedUpH){
//                mMillisecondPerFrame = 8;
//            }else{
            mMillisecondPerFrame = 1000 / framesInH + 1;
//            }
            getHCurrent22EngineAngleCMD = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,
                    GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_ANGLE_FIRMWARE_OUT_CMD_2, null);
            mRobotType = ControlInitiator.ROBOT_TYPE_H;
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C) {
            mMillisecondPerFrame = 1000 / framesInC + 1;
            mRobotType = ControlInitiator.ROBOT_TYPE_C;
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            /** lz 2017-3-23 */
            iCount = (byte) sServoCount; // 22
            pID = new byte[sServoCount];
            for (int n = 0; n < sServoCount; n++) {
                pID[n] = (byte) (n + 1); // 0~21
            }
            mMillisecondPerFrame = 1000 / framesInS + 1;
            if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S){
                mRobotType = ControlInitiator.ROBOT_TYPE_S;
            }else{
                mRobotType = ControlInitiator.ROBOT_TYPE_H3;
            }

        } else {
            LogMgr.e("机器人类型设置错误 不是M也不是H也不是C.");
            mMillisecondPerFrame = 1000 / framesInM + 1;
            mRobotType = ControlInitiator.ROBOT_TYPE_M;
        }
//        startAlarm();
        mHandler = new Handler(ControlApplication.instance.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                LogMgr.d("mHandler handleMessage()");
            }
        };

    }

    public void sendDataToStm32() {
        LogMgr.v("sendDataToStm32() 开始解析一帧动作");
        countOfPlayedFrame++;
        countOfFrameIncludeRepeat++;
        if ((ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H || ControlInfo.getMain_robot_type() ==
                ControlInitiator.ROBOT_TYPE_S || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3)
                && mPlayMode == PLAY_MODE_VARIABLE_LENGTH) {
            sendDataToHWithVariableFrameLength();
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
            sendDataToM();
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
//            if(lastFrameData == null ||countOfFrameIncludeRepeat%5==1){
            sendDataToH();
//            }else{
//                if(isSpeedUpH){
//                    ProtocolUtils.sendEngineAngles(iCount, pID, lastFrameData);
//                }else{
//                    sendDataToH();
//                }
//            }
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C) {
            sendDataToC();
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            sendDataToS();
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1) {
//            sendDataToM1();
        } else {
            LogMgr.e("机器人类型设置错误 不是M也不是H、S也不是C.");
        }
        LogMgr.v("sendDataToStm32() 开始解析一帧动作");
    }

    /**
     * 获取实例
     *
     * @return
     */
    public static final PlayMoveOrSoundUtils getInstance() {
        if (instance == null) {
            synchronized (PlayMoveOrSoundUtils.class) {
                if (instance == null) {
                    instance = new PlayMoveOrSoundUtils();
                }
            }
        }
        return instance;
    }

    public void handlePlayList(String[] moveList) {
        mCurrentCountInList = 0;
        setPlayMoveList(moveList);
        handlePlayCmd(mPlayMoveList[mCurrentCountInList], null, false, true, 0, false, PLAY_MODE_LIST, false, true, null);
    }

    /**
     * 播放动作文件的入口
     *
     * @param actionFileName  要播放的动作文件完整路径
     * @param soundFileName   要播放的音频/视频文件完整路径
     * @param isLoop          是否循环播放 针对动作文件 只放音频时设为false
     * @param isNeedToReply   是否需要向上层APP反馈消息
     * @param delayTime       播放第一帧后的延迟时间 通常为2000 针对动作文件 只放音频时设为0
     * @param isProcedureMove 是否是Procedure中的动作
     * @param playMode        播放模式
     * @param isUseAutoBalance H5,H3是否使用步态调整
     * @param isMObstacleAvoidanceEnable M系列避障功能是否开启
     *@param playCallBack    播放回调  @return true 执行动作成功 false 不执行当前动作
     */
    public synchronized boolean handlePlayCmd(String actionFileName, String soundFileName, boolean isLoop,
                                              boolean isNeedToReply, int delayTime, boolean isProcedureMove, int
                                                      playMode, boolean isUseAutoBalance, boolean isMObstacleAvoidanceEnable, PlayCallBack playCallBack) {
        boolean result = false;

        if (TextUtils.isEmpty(actionFileName) && TextUtils.isEmpty(soundFileName)) {
            LogMgr.e("actionFileName,soundFileName都为空，退出");
            return false;
        }
        LogMgr.i("isProcedureMove = " + isProcedureMove + " ProcedureControl.getInstance().isProcedureRunning() = "
                + ProcedureControl.getInstance().isProcedureRunning());
        if (isProcedureMove == false && ProcedureControl.getInstance().isProcedureRunning()) {// TODO
            // 还需要判断是否是同一个Procedure
            // 当前正在一个Procedure当中，不执行动作命令
            LogMgr.e("当前正在一个Procedure当中，不执行动作命令");
            return false;
        }
        if (isPaused() == true) {
            LogMgr.i("当前为暂停状态，收到新的播放命令，停止当前动作。");
            stopCurrentMove();
        }
        LogMgr.i("handlePlayCmd() actionFileName = " + actionFileName + " soundFileName = " + soundFileName
                + " isLoop = " + isLoop + " isNeedToReply = " + isNeedToReply + " delayTime = " + delayTime + " isProcedureMove = "+isProcedureMove +
                " playMode = "+playMode+" isUseAutoBalance = "+isUseAutoBalance + " isMObstacleAvoidanceEnable = "+isMObstacleAvoidanceEnable);
        LogMgr.i("handlePlayCmd() isRobotMoving = " + isRobotMoving + " isMoveLegalAfterLastMove = "
                + isMoveLegalAfterLastMove(actionFileName) + " isStopCmd(actionFileName) = "
                + isStopCmd(actionFileName) + "isRecovering = " + isRecovering);

        // 当前机器人不在动作中，且下一个动作合法
        if (isRobotMoving == false && isMoveLegalAfterLastMove(actionFileName) && isRecovering == false) {
            // || (isRobotMoving == true && isPaused() == true && isRecovering
            // == false && isNeedToReply == true)){

            // skillplayer在机器人暂停时，播放其他动作
            // if(isRobotMoving == true && isPaused() == true && isRecovering ==
            // false && isNeedToReply == true){
            // LogMgr.d("暂停状态下开始恢复初态");
            // forceStop();
            // isRecovering = true;
            // recoverToMoveInitailPosition(true,
            // actionFileName,soundFileName,isLoop,isNeedToReply,delayTime,isProcedureMove);
            // return true;
            // }
            // 上一个动作是强制停止的
            if (isForceStoped == true && (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M)) {
                // ||　ControlInfo.getMain_robot_type() ==
                // ControlInitiator.ROBOT_TYPE_H)) {
                LogMgr.i("上一个动作是强制停止的 开始恢复初态 isForceStoped = " + isForceStoped + " ControlKernel.type = "
                        + ControlInfo.getMain_robot_type());
                isRecovering = true;
                isForceStoped = false;
                recoverToMoveInitailPosition(true, actionFileName, soundFileName, isLoop, isNeedToReply, delayTime,
                        isProcedureMove, playMode, playCallBack, isUseAutoBalance, isMObstacleAvoidanceEnable);
                return true;
            }

            // C系列定位到预定的起始位置
//            initForC();

            this.mPlayCallBack = playCallBack;
            this.isUseAutoBalance = isUseAutoBalance;
            if (isNeedToReply == true) {
                this.isNeedToReply = true;
                byte[] returnData = new byte[5];
                returnData[0] = (byte) 0x00;
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_PLAY, returnData);
            }
            if (mPlayCallBack != null) {
                mPlayCallBack.onStart();
            }
            isRobotMoving = true;
            setPaused(false);
            isForceStoped = false;
            isRobotMoveLooping = isLoop;
            mDelayTime = delayTime;
            mLastShoulderAngleForC = 0;
            mPlayMode = playMode;
            mIsMObstacleAvoidanceOpen = isMObstacleAvoidanceEnable;

            if (TextUtils.isEmpty(actionFileName) && !TextUtils.isEmpty(soundFileName)) {
                LogMgr.i("actionFileName为空,soundFileName不为空");
                // 播放音频文件
                lastRobotMoveType = MOVE_TYPE_SOUND;
                lastRobotSound = soundFileName;

                if (mPlayer == null) {
                    mPlayer = new Player();
                    mPlayer.setOnCompletionListener(this);
                }
                mPlayer.playSoundFile(soundFileName);

                result = true;
            } else if (!TextUtils.isEmpty(actionFileName) && TextUtils.isEmpty(soundFileName)) {
                LogMgr.i("actionFileName不为空,soundFileName为空");
                // 播放动作文件
                lastRobotMoveType = MOVE_TYPE_ACTION;
                lastRobotAction = actionFileName;

                play(actionFileName);

                result = true;
            } else {
                LogMgr.i("actionFileName不为空,soundFileName不为空");
                // 播放动作，音频文件
                lastRobotMoveType = MOVE_TYPE_BOTH;
                lastRobotAction = actionFileName;
                lastRobotSound = soundFileName;

                if((mPlayMode & PLAY_MODE_VEDIO) == 0) {
                    if (mPlayer == null) {
                        mPlayer = new Player();
                    }
                    mPlayer.playSoundFile(soundFileName);
                }else{
                    controlVedio(VIDEO_CONTROL_PLAY, soundFileName);
                }
                play(actionFileName);

                result = true;
            }
        }
        // 接受到的命令是停止命令 且停止命令合法时
        else if (isStopCmd(actionFileName) && isMoveLegalAfterLastMove(actionFileName)) {
            LogMgr.i("接收到停止命令 actionFileName = " + actionFileName);

            if ((isNextMoveStop == true && !TextUtils.isEmpty(stopRobotMoveCmd)) || isStopMoveDoing) {
                // 已经接受到停止命令，或正在执行停止动作，不重复设定停止动作
                LogMgr.i("已经接受到停止命令，或正在执行停止动作，不重复设定停止动作 " + actionFileName);
                result = false;
            } else {
                LogMgr.i("设置了下一个动作为停止动作 " + actionFileName);
                isNextMoveStop = true;
                stopRobotMoveDelayTime = delayTime;
                isRobotMoveLooping = false;
                stopRobotMoveCmd = actionFileName;
                result = true;
            }
        } else {
            LogMgr.w("不执行当前动作");
            result = false;
        }

        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S) {
            sendLightToBrainS5(10, 1);
        }

        return result;
    }

    /**
     *
     * @param videoControl
     * @param videoPath
     */
    private void controlVedio(int videoControl, String videoPath) {
        BroadcastResponder.sendVedioControl(videoControl, videoPath);
    }

    // private boolean play(String moveName, int delayTime){
    // this.mMoveName = moveName;
    // this.mDelayTime = delayTime;
    //
    // return play(moveName);
    // }

    /**
     * 开始播放动作
     *
     * @param moveName 动作文件名
     */
    @SuppressLint("DefaultLocale")
    private boolean play(String moveName) {

        FileInputStream fileRead = null;
        long fileLength = 0;
        int fileReadLineNum = 0;
        // String filePath = GlobalConfig.DOWNLOAD_PATH +File.separator +
        // moveName;

        if (moveName.toLowerCase().endsWith("bin")) {
            try {
                File file = new File(moveName);
                fileLength = file.length();
                fileRead = new FileInputStream(file);
                mFileBuff = new byte[(int) fileLength];

                fileReadLineNum = fileRead.read(mFileBuff);
                fileRead.close();

                if (fileReadLineNum == (int) fileLength) {

                    StopTimer();
                    Thread.sleep(10);

                    mFileLength = fileReadLineNum;
                    mCurrentPosition = 0;
                    mIsFirst = true;
                    mIsFirstFrame = true;
                    analyzeFileHead();
                } else {
                    LogMgr.e("setPosAll读取长度错：fileReadLineNum = " + fileReadLineNum + "  fileLength = " + fileLength);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("打开文件失败 " + moveName);
                if (mPlayer != null) {
                    LogMgr.d("mediaPlayer.stop()");
                    mPlayer.stop();
                }
                if((mPlayMode&PLAY_MODE_VEDIO)!=0){
                    controlVedio(VIDEO_CONTROL_STOP, null);
                }
                isRobotMoving = false;
                isForceStoped = false;
                ProcedureControl.getInstance().afterStopCurrentProcedure();
                return false;
            }
            // 打开文件成功后，启动定时器发送
            if ((ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M)
                    && (GlobalConfig.isDisplacementMatterForMBin == true)) {
                // 使用位移定位法顺序执行M的bin文件 TODO
                LogMgr.d("使用位移定位法顺序执行M的bin文件");
//                sendDataToMWithDisplacement();
            } else if ((ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H || ControlInfo
                    .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3 || ControlInfo
                    .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S)
                    && mPlayMode == PLAY_MODE_VARIABLE_LENGTH) {
                // 进行变长动作文件的解析.
                boolean result = analyzeVariableFrameBinFile();
                if (result == false) {
                    stopHandle();
                }
            } else {
                StartTimer();
            }

        } else {
            LogMgr.e("动作文件没有以.bin结尾");
            if (mPlayer != null) {
                LogMgr.d("mediaPlayer.stop()");
                mPlayer.stop();
            }
            if((mPlayMode&PLAY_MODE_VEDIO)!=0){
                controlVedio(VIDEO_CONTROL_STOP, null);
            }
            isRobotMoving = false;
            isForceStoped = false;
            ProcedureControl.getInstance().afterStopCurrentProcedure();
            return false;
        }
        return true;
    }

    private void StopTimer() {
        if(scheduledExecutorService != null){
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
//        if (mTimerTask != null) {
//            mTimerTask.cancel();
//            mTimerTask = null;
//        }
//        if (mTimer != null) {
//            mTimer.cancel();
//            mTimer = null;
//        }
    }

    private void StartTimer() {
        countOfPlayedFrame = 0;
        countOfFrameIncludeRepeat = 0;
        StopTimer();
        isFirstSetTimeSlack = true;
        scheduledExecutorService = Executors.newScheduledThreadPool(10);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendDataToStm32();
            }
        }, 0, 41667, TimeUnit.MICROSECONDS);
//        mTimer = new Timer();
//        mTimerTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (isFirstSetTimeSlack){
//                    ProcessControl.getInstance().timerslack(Process.myTid(),mMillisecondPerFrame);
//                    isFirstSetTimeSlack = false;
//                }
//                sendDataToStm32();
//            }
//        };
////        mTimer.schedule(mTimerTask, 0, mMillisecondPerFrame);
//        mTimer.scheduleAtFixedRate(mTimerTask, 0, mMillisecondPerFrame);
    }



    /**
     * 41毫秒发一帧，每一帧的动作解析 S机器人解析
     * <p>
     * lz 2017-3-23
     */
    private void sendDataToS() {
        byte[] pPos = new byte[sServoCount * 2];
        byte[] pColor = new byte[1 * 3];
        float rlGyro = 0;
        float fbGyro = 0;
        double roll = 0;
        double pitch = 0;
        double yaw = 0;
        float[] gyroValues = SensorImuService.getGyroValues();
//        float angle_pitch = SensorImuService.getAngle_Pitch();
        if (null != gyroValues){
//            rlGyro = -gyroValues[2] * 30;
//            fbGyro = gyroValues[1] * 30;
            fbGyro = SensorImuService.getGyroX()*30;
            rlGyro = SensorImuService.getGyroY()*30;
//            pitch = angle_pitch;
        }

        // 从文件中获取动作数据
        if (mCurrentPosition + 4 < mFileLength) {
            while (mCurrentPosition + 4 < mFileLength) {
                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
                    // 检测头
                    // 帧长度 从机器人型号开始 没有算前四帧
                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
                            + 3] & 0xFF) << 8));
                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                        byte check = 0x00;
                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m到数据位结束加和
                            check += mFileBuff[n];
                        }
                        check = (byte) check;

                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_S) {
                                LogMgr.v("机器人型号正确 mFileBuff[4] = " + (mFileBuff[4] & 0xFF));
                            } else {
                                LogMgr.v("机器人型号错误 mFileBuff[4] = " + (mFileBuff[4] & 0xFF));
                            }
                            int frameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));

                            if (isMoveEnable) {
                                // 正常情况下 都应该有动作
                                LogMgr.v("动作位有效");
                                System.arraycopy(mFileBuff, mCurrentPosition + 11, pPos, 0, pPos.length);
                            }
//							LogMgr.e("pPos:" + Utils.bytesToString(pPos, pPos.length));
//							pPos = testSwitch(pPos);
//							LogMgr.e("new pPos:" + Utils.bytesToString(pPos, pPos.length));
                            LogMgr.v("bFirst = " + mIsFirst + " mSerialPortActivity.loopCount = " + loopCount);
                            if (mIsFirst && loopCount == 0 && ControlInfo.getMain_robot_type() == ControlInitiator
                                    .ROBOT_TYPE_S) { // 第一次运动特殊处理，将速度降低
                                LogMgr.i("第一帧动作，减慢速度为100 ， 停顿" + 2000 + "毫秒后继续");
                                mIsFirst = false;
                                StopTimer();

                                ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 100);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//                                if(isUseAutoBalance && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                                if(true){
//                                    auto_balance(iCount, pID, pPos, rlGyro, fbGyro, roll, pitch, yaw);
                                }
                                ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 0);
                                StartTimer();
                            } else if (mIsFirst && loopCount == 0) {
                                LogMgr.i("第一帧动作，不停顿");
                                mIsFirst = false;
                                if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3 &&
                                        lastRobotAction.endsWith(GlobalConfig.MOVE_BOOT_RECOVER)) {
                                    LogMgr.i("当前是开机复位，速度值为100");
                                    ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 100);
                                } else {
                                    ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 0);
                                }
                            } else {
//                                if(isUseAutoBalance && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                                if(true){
//                                    auto_balance(iCount, pID, pPos, rlGyro, fbGyro, roll, pitch, yaw);
                                }
                                ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
                            }
                            //开机复位动作不显示灯光，执行复位和防摔倒动作文件不显示灯光
                            if (isLightEnable && !(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3 &&
                                    ( lastRobotAction.endsWith(GlobalConfig.MOVE_BOOT_RECOVER) || lastRobotAction.endsWith(GlobalConfig.MOVE_RISE_BACK) ||
                                    lastRobotAction.endsWith(GlobalConfig.MOVE_RISE_FRONT) || lastRobotAction.endsWith(GlobalConfig.MOVE_MARK_TIME))  )    ) {
                                LogMgr.v("光效有效");
//								LogMgr.e("mCurrentPosition:" + mCurrentPosition);

                                System.arraycopy(mFileBuff, mCurrentPosition + 11 + pPos.length, pColor, 0, pColor
                                        .length);
//								LogMgr.e("pColor:" + Utils.bytesToString(pColor, pColor.length));
                                try {
                                    Thread.sleep(5);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                headColor(pColor);

//								if (isLightInHandFootEnable && GlobalConfig.isLightInHandFootEnableControl) {
//									LogMgr.v("手脚光效有效");
//									setAndExeColorCmd1(pColor);
//									setColorCmd2(pColor);
//								} else {
//									LogMgr.v("手脚光效无效");
//									if (frameNum % ColorSpeed == 0) {
//										setAndExeColorCmd1(pColor);
//									}
//
//								}
                            } else {
                                LogMgr.v("光效无效");
                            }

                            int stepLength = 1;
                            LogMgr.i("成功解析一帧: 当前帧序号 frameNum = " + frameNum + " mCurrentPosition = " + mCurrentPosition);
                            if(countOfPlayedFrame >= 125){
                                countOfPlayedFrame = 0;
                                stepLength = 2;
                                LogMgr.i("跳过下一帧 序号 = "+(frameNum + 1));
                            }

                            mCurrentPosition += stepLength*(frameLength + 4);

                            return;
                        } else {
                            mCurrentPosition++;
                            LogMgr.e("校验和错误");
                        }
                    } else {
                        mCurrentPosition++;
                        LogMgr.e("最后一帧损坏");
                        return;
                    }
                } else {
                    mCurrentPosition++;
                    LogMgr.e("帧头错误");
                }
            }
        } else {
            stopHandle();
        }
    }

    /**
     * S5给brain发送消息
     *
     * @param parms1
     * @param parms2
     */
    public void sendLightToBrainS5(int parms1, int parms2) {
        byte[] bytes = new byte[]{(byte) ((parms2 * 16 + parms1) & 0xFF)};
        Brain mscratch = new Brain(2, null);
        mscratch.setModeState(14);
        mscratch.setSendByte(bytes);
        ControlKernel.getInstance().responseCmdToBrain(mscratch);
    }




    public void auto_balance_h5(byte iCount, byte[] pID, byte[] pPos) {  //rlgyro为陀螺仪的左右方向瞬时速度，fbgyro为陀螺仪前后方向的瞬时速度。
//        float angle_pitch = SensorImuService.getAngle_Pitch();
        float rlgyro = 0;
        float fbgyro = 0;
        double roll = 0;
        double pitch = 0;
        double yaw = 0;
        float[] gyroValues = SensorImuServiceForH5.getGyroValues();
//        float angle_pitch = SensorImuService.getAngle_Pitch();
        if (null != gyroValues){
//            rlGyro = -gyroValues[2] * 30;
//            fbGyro = gyroValues[1] * 30;
//            fbgyro = SensorImuServiceForH5.getGyroX()*30;
//            fbgyro = -(gyroValues[0] - 2)*30;
            fbgyro = -gyroValues[0]*30;
//            rlgyro = SensorImuServiceForH5.getGyroY()*30;
//            rlgyro = gyroValues[2]*30;
            rlgyro = gyroValues[2]*30;
//            pitch = angle_pitch;
        }else{
            return;
        }

        LogMgr.e("rlgyro = "+rlgyro+" fbgyro = "+fbgyro);
        double BALANCE_HIP_PITCH_GAIN = 0.3;
//        double BALANCE_HIP_PITCH_GAIN = 0.2;
//        double BALANCE_HIP_ROLL_GAIN = 0.02777777;
//        double BALANCE_KNEE_GAIN = 0.083333;
//        double BALANCE_ANKLE_PITCH_GAIN = 0.083333;
//        double BALANCE_ANKLE_ROLL_GAIN = 0.0277777;
        double BALANCE_HIP_ROLL_GAIN = 0.3;
        double BALANCE_KNEE_GAIN = 0.3;
        double BALANCE_ANKLE_PITCH_GAIN = 0.3;
        double BALANCE_ANKLE_ROLL_GAIN = 0.3;
//        double BALANCE_ANKLE_PITCH_GAIN = 0.6;
//        double BALANCE_ANKLE_ROLL_GAIN = 0.6;
        int pos;
        int lowByte;
        int hiByte;
//        SensorImuService.startSensorImuService();
        if (iCount < 1 || iCount > 30)
            return;

        for (int i = 0; i < iCount; i++) {
            if (pID[i] < 7 || pID[i] > 16)
                continue;
            hiByte = 0x000000FF & (int) pPos[i * 2 + 1];
            lowByte = 0x000000FF & (int) pPos[i * 2];
            pos = (int) ((hiByte << 8 | lowByte) & 0xFFFFFFFF);
            switch (pID[i]) {
			/*case 1:// R_HIP_PITCH
				pos -= (int) (fbgyro * BALANCE_HIP_PITCH_GAIN);
				break;
			case 2:// L_HIP_PITCH
				pos += (int) (fbgyro * BALANCE_HIP_PITCH_GAIN);
				break;*/
                case 3:// R_HIP_ROLL
                    pos -= (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);
//                    pos += (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);

                    break;
                case 4:// L_HIP_ROLL
                    pos -= (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);
//                    pos += (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);

                    break;
                case 5:// R_KNEE
                    pos -= (int) (fbgyro * BALANCE_KNEE_GAIN);
                    break;
                case 6:// L_KNEE
                    pos += (int) (fbgyro * BALANCE_KNEE_GAIN);
                    break;
                case 7:// R_ANKLE_PITCH
                    pos -= (int) (fbgyro * BALANCE_ANKLE_PITCH_GAIN);
                    break;
                case 8:// L_ANKLE_PITCH
                    pos += (int) (fbgyro * BALANCE_ANKLE_PITCH_GAIN);
                    break;
                case 9:// R_ANKLE_ROLL
                    pos -= (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);
//                    pos += (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);

                    break;
                case 10://L_ANKLE_ROLL
                    pos -= (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);
//                    pos += (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);

                    break;
                default:
                    break;
            }
            pPos[i * 2 + 1] = (byte) ((pos >> 8) & 0xff);
            pPos[i * 2] = (byte) (pos & 0xff);
        }
    }
    
    /**
     * 20毫秒发一帧，每一帧的动作解析 H机器人解析
     */
    private void sendDataToH() {
        LogMgr.i("sendDataToH()");

        byte[] pPos = new byte[mServoCount * 2];
        byte[] pColor = new byte[9 * 3];

//        float rlGyro = 0;
//        float fbGyro = 0;
//        double roll = 0;
//        double pitch = 0;
//        double yaw = 0;
//        float[] gyroValues = SensorImuService.getGyroValues();
////        float angle_pitch = SensorImuService.getAngle_Pitch();
//        if (null != gyroValues){
////            rlGyro = -gyroValues[2] * 30;
////            fbGyro = gyroValues[1] * 30;
//            fbGyro = SensorImuService.getGyroX()*30;
//            rlGyro = SensorImuService.getGyroY()*30;
////            pitch = angle_pitch;
//        }

        // 从文件中获取动作数据
        if (mCurrentPosition + 4 < mFileLength) {
            while (mCurrentPosition + 4 < mFileLength) {
                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
                    // 检测头
                    // 帧长度 从机器人型号开始 没有算前四帧
                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
                            + 3] & 0xFF) << 8));
                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                        byte check = 0x00;
                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m到数据位结束加和
                            check += mFileBuff[n];
                        }
                        check = (byte) check;

                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                            if (mFileBuff[mCurrentPosition + 4] == (byte) ControlInfo.getMain_robot_type()) {
                                LogMgr.v("机器人型号正确 mFileBuff[4] = " + mFileBuff[4]);
                            } else {
                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[4]);
                            }
                            int frameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));

                            if (isMoveEnable) {
                                // 正常情况下 都应该有动作
                                LogMgr.v("动作位有效");
                                System.arraycopy(mFileBuff, mCurrentPosition + 11, pPos, 0, pPos.length);
                            }

                            LogMgr.v("bFirst = " + mIsFirst + " mSerialPortActivity.loopCount = " + loopCount);
                            if (mIsFirst && loopCount == 0 && mDelayTime > 0) { // 第一次运动特殊处理，将速度降低
                                LogMgr.i("第一帧动作，减慢速度为100 ， 停顿" + mDelayTime + "毫秒后继续");
                                mIsFirst = false;
                                StopTimer();

                                ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 50);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 0);
                                StartTimer();
                            } else if (mIsFirst && loopCount == 0 && mDelayTime == 0) {
                                LogMgr.i("第一帧动作，不停顿");
                                mIsFirst = false;
                                if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H &&
                                        (lastRobotAction.endsWith(GlobalConfig.MOVE_BOOT_RECOVER_H) || lastRobotAction.endsWith(GlobalConfig.MOVE_BOOT_RECOVER_XIADUN))) {
                                    LogMgr.i("当前是开机复位，速度值为100");
                                    ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 100);
                                } else {
                                    ProtocolUtils.setEngineSpeed(iCount, pID, pPos, 0);
                                }
                                // try {
                                // Thread.sleep(5);
                                // } catch (InterruptedException e) {
                                // e.printStackTrace();
                                // }
                                // sendEngineAngles(iCount, pID, pPos);
                            } else {
                                final byte[] lastPos = pPos;
                                final Long lastTime = System.currentTimeMillis();
//                                if(isSpeedUpH){
//                                    Runnable runnable = new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if(System.currentTimeMillis() - lastTime > 30 ){
//                                                return;
//                                            }
//                                            byte[] pos = GaitAlgorithmForH5.getInstance().adjustMove(lastPos);
//                                            auto_balance_h5(iCount, pID, pos);
//                                            ProtocolUtils.sendEngineAngles(iCount, pID, pos);
//                                        }
//                                    };
//                                    if(handlerForH5!=null){
//                                        handlerForH5.postDelayed(runnable,14);
//                                        handlerForH5.postDelayed(runnable,28);
//                                    }else{
//                                        LogMgr.e("handlerForH5没有初始化");
//                                    }
//
//                                }
//                                if(sonCount >= Integer.MAX_VALUE){
//                                    sonCount = 0;
//                                }
//                                sonCount++;
//                                if(sonCount % 5 == 0){
//                                    pPos = GaitAlgorithmForH5.getInstance().adjustMove(pPos);
//                                    auto_balance_h5(iCount, pID, pPos);
//                                }
                                SensorImuServiceForH5.startSensorImuServiceForH5();
                                byte[] pos = pPos;
//                                pPos = GaitAlgorithmForH5.getInstance().adjustMove(pPos);
                                auto_balance_h5(iCount, pID, pPos);
                                ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
                            }

                            if (isLightEnable && GlobalConfig.isLightEnableControl) {
                                LogMgr.v("光效有效");
                                System.arraycopy(mFileBuff, mCurrentPosition + 11 + pPos.length, pColor, 0,
                                        pColor.length);
                                if (isLightInHandFootEnable && GlobalConfig.isLightInHandFootEnableControl) {
                                    LogMgr.v("手脚光效有效");
                                    setAndExeColorCmd1(pColor);
//                                    setColorCmd2(pColor);
                                } else {
                                    LogMgr.v("手脚光效无效");
                                    if (frameNum % ColorSpeed == 0) {
                                        setAndExeColorCmd1(pColor);
                                    }
                                }
                            } else {
                                LogMgr.v("光效无效");
                            }

                            LogMgr.i("成功解析一帧: 当前帧序号 frameNum = " + frameNum + " mCurrentPosition = " +
                                    mCurrentPosition);

                            mCurrentPosition += (frameLength + 4);
                            int stepLength = 1;
                            LogMgr.i("成功解析一帧: 当前帧序号 frameNum = " + frameNum + " mCurrentPosition = " + mCurrentPosition + "::" + (System.currentTimeMillis() - mCurrentTime));
                            mCurrentTime =  System.currentTimeMillis();
//                            if(countOfPlayedFrame >= 125){
//                                countOfPlayedFrame = 0;
//                                stepLength = 2;
//                                LogMgr.i("跳过下一帧 序号 = "+(frameNum + 1));
//                            }

//                            mCurrentPosition += stepLength*(frameLength + 4);

                            return;
                        } else {
                            mCurrentPosition++;
                            LogMgr.e("校验和错误");
                        }
                    } else {
                        mCurrentPosition++;
                        LogMgr.e("最后一帧损坏");
                        return;
                    }
                } else {
                    mCurrentPosition++;
                    LogMgr.e("帧头错误");
                }
            }
        } else {
            stopHandle();
        }
    }
    private long mCurrentTime;
    /**
     * 发送变长Bin文件帧至stm32
     */
    private void sendDataToHWithVariableFrameLength() {
        // mPlayActionList;
        // mPlayActionDuration;

        LogMgr.i("mCurrentFrameNum = " + mCurrentFrameNum + " mPlayActionList.length = " + mPlayActionList.length
                + " mPlayActionDuration.length = " + mPlayActionDuration.length);
        int currentVariableFrameNum;
        for (currentVariableFrameNum = 1; currentVariableFrameNum < mPlayActionList.length; currentVariableFrameNum++) {
            if (mCurrentFrameNum * 20 < getSumOfTimeOfFirstNFrame(currentVariableFrameNum)) {
                break;
            }
        }
        if (currentVariableFrameNum == mPlayActionList.length) {
            LogMgr.i("文件运行完成");
            stopHandle();
            return;
        }
        LogMgr.i("当前是动作的第" + (currentVariableFrameNum - 1) + "帧到第" + currentVariableFrameNum + "帧");
        int currentPiece = mCurrentFrameNum * 20 - getSumOfTimeOfFirstNFrame(currentVariableFrameNum - 1);
        byte[] angleData = new byte[44];
        for (int i = 0; i < 44; i = i + 2) {
            int startAngle = (int) (((mPlayActionList[currentVariableFrameNum - 1][i] & 0xFF) << 8) |
                    (mPlayActionList[currentVariableFrameNum - 1][i + 1] & 0xFF));
            int endAngle = (int) (((mPlayActionList[currentVariableFrameNum][i] & 0xFF) << 8) |
                    (mPlayActionList[currentVariableFrameNum][i + 1] & 0xFF));
            int currentPosition = startAngle
                    + (int) (1.0 * (endAngle - startAngle) * (currentPiece / 20 + 1) /
                    (mPlayActionDuration[currentVariableFrameNum - 1] / 20));
//            LogMgr.d("第" + (i / 2 + 1) + "个舵机 " + " startAngle = " + startAngle + " endAngle = " + endAngle
//                    + " currentPosition = " + currentPosition);
            angleData[i] = (byte) ((currentPosition >> 8) & 0xFF);
            angleData[i + 1] = (byte) (currentPosition & 0xFF);
        }

        // byte[] angleDataToSend = new byte[46];
        //
        // System.arraycopy(angleData, 0, angleDataToSend, 2, angleData.length);
        // sendEngineAngles(iCount, pID, angleDataToSend);
        byte[] angleCmdData = new byte[1 + 22 + 22 * 2];
        angleCmdData[0] = (byte) 22;
        for (int i = 1; i <= 22; i++) {
            angleCmdData[i] = (byte) i;
        }
        System.arraycopy(angleData, 0, angleCmdData, 23, angleData.length);
        byte[] angleCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_SET_ANGLE_OUT_CMD_2, angleCmdData);
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
/*         SP.writeMotor(angleCmd, 10);
            SP.readMotor(new byte[20]);
            */
            SP.requestVice(angleCmd);
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
/*            SP.write(angleCmd, 20);
            SP.read(new byte[20]);*/
//            SP.request(angleCmd);
            SP.write(angleCmd);
        }

        // if(mCurrentFrameNum > 100){
        // stopHandle();
        // }

        mCurrentFrameNum++;
    }

    /**
     * 获取前N帧的总时长
     *
     * @param currentVariableFrameNum
     * @return
     */
    private int getSumOfTimeOfFirstNFrame(int currentVariableFrameNum) {
        int result = 0;
        for (int i = 0; i < currentVariableFrameNum; i++) {
            result += mPlayActionDuration[i];
        }
        return result;
    }

    /**
     * 解析变长BIN文件
     */
    private boolean analyzeVariableFrameBinFile() {
        LogMgr.i("analyzeVariableFrameBinFile()");

        // byte[] pPos = new byte[mServoCount * 2];
        // byte[] pColor = new byte[9 * 3];

        mPlayActionList = new byte[mTotalFrameNum + 1][44];
        mPlayActionDuration = new int[mTotalFrameNum];
        byte[] tempCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_ANGLE_FIRMWARE_OUT_CMD_2, null);
        byte[] angleCurrentData = new byte[56];
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
/*            SP.readMotor(angleCurrentData);
            SP.writeMotor(tempCmd, 80);
            SP.readMotor(angleCurrentData);*/
            byte[] bytes = SP.requestVice(tempCmd);
            System.arraycopy(bytes, 0, angleCurrentData, 0, bytes.length);
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            LogMgr.e("获取角度");
            byte[] bytes = SP.request(tempCmd, 100);
            System.arraycopy(bytes, 0, angleCurrentData, 0, bytes.length);
        }
        LogMgr.i("analyzeVariableFrameBinFile()1");
        LogMgr.d("获取到当前角度数据 = " + Utils.bytesToString(angleCurrentData));

        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
            for (int i = 0; i < 22; i++) {
                int angle = (int) (((angleCurrentData[11 + i * 2] & 0xFF) << 8) | (angleCurrentData[11 + i * 2 + 1] &
                        0xFF));
//                LogMgr.d("第" + (i + 1) + "个舵机角度 = " + angle);
                if (angle <= 0 || angle >= 1023) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < 22; i++) {
                int angle = (int) (((angleCurrentData[11 + i * 2] & 0xFF) << 8) | (angleCurrentData[11 + i * 2 + 1] &
                        0xFF));
//                LogMgr.d("第" + (i + 1) + "个舵机角度 = " + angle);
            }
        }

        if (angleCurrentData[0] == (byte) GlobalConfig.CMD_0 && angleCurrentData[1] == (byte) GlobalConfig.CMD_1
                && angleCurrentData[5] == (byte) GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1
                && angleCurrentData[6] == (byte) GlobalConfig.ENGINE_ANGLE_FIRMWARE_IN_CMD_2) {
            System.arraycopy(angleCurrentData, 11, mPlayActionList[0], 0, mPlayActionList[0].length);
            LogMgr.i("mPlayActionList[0].length == 44 is " + (mPlayActionList[0].length == 44));
        } else {
            return false;
        }

        int currentFrameNum = 1;
        for (int i = 0; i < mTotalFrameNum; i++) {
            if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) { // 检测头
                // 帧长度 从机器人型号开始 没有算前四帧
                int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition + 3]
                        & 0xFF) << 8));
                if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                    byte check = 0x00;
                    for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m到数据位结束加和
                        check += mFileBuff[n];
                    }
                    check = (byte) check;

                    if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                        if (mFileBuff[mCurrentPosition + 4] == ControlInfo.getMain_robot_type()) {
                            LogMgr.v("机器人型号正确 mFileBuff[mCurrentPosition + 4] = " + mFileBuff[mCurrentPosition + 4]);
                        } else {
                            LogMgr.e("机器人型号错误 mFileBuff[mCurrentPosition + 4] = " + mFileBuff[mCurrentPosition + 4]);
                        }
                        int frameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | ((mFileBuff[mCurrentPosition
                                + 6] & 0xFF) << 8));
                        if ((frameNum + 1) != currentFrameNum || (frameNum + 1) > mTotalFrameNum) {
                            LogMgr.e("帧序号错误 frameNum = " + frameNum + " currentFrameNum = " + currentFrameNum
                                    + " mTotalFrameNum = " + mTotalFrameNum);
                            return false;
                        }

                        int variableFrameTime = (int) ((mFileBuff[mCurrentPosition + 7] & 0xFF) | (
                                (mFileBuff[mCurrentPosition + 8] & 0xFF) << 8));
                        if (variableFrameTime % 20 != 0) {
                            LogMgr.e("帧时长不是20ms的倍数，异常");
                            return false;
                        }

                        if (isMoveEnable) {
                            // 正常情况下 都应该有动作
                            LogMgr.v("动作位有效 + mPlayActionList[currentFrameNum].length = "
                                    + mPlayActionList[currentFrameNum].length);
                            System.arraycopy(mFileBuff, mCurrentPosition + 11, mPlayActionList[currentFrameNum], 0,
                                    mPlayActionList[currentFrameNum].length);
                            mPlayActionDuration[currentFrameNum - 1] = variableFrameTime;
                        } else {
                            LogMgr.e("动作位无效");
                            return false;
                        }

                        LogMgr.i("成功解析一帧: 当前帧序号 currentFrameNum = " + currentFrameNum + " mCurrentPosition = "
                                + mCurrentPosition + " variableFrameTime = " + variableFrameTime);
                        currentFrameNum++;
                        mCurrentPosition += (frameLength + 4);
                    } else {
                        LogMgr.e("校验和错误");
                        return false;
                    }
                } else {
                    LogMgr.e("最后一帧损坏");
                    return false;
                }
            } else {
                LogMgr.e("帧头错误");
                return false;
            }
        }

        LogMgr.i("解析bin文件成功，启动timer，开始运行bin文件");
        mCurrentFrameNum = 0;
        ProtocolUtils.setEngineSpeed(iCount, pID, null, 0);
        StartTimer();
        return true;
    }

//    /**
//     * @Description 41毫秒发一帧，每一帧的动作解析 M1机器人解析
//     * @author lz
//     * @time 2017-4-21 下午2:47:58
//     */
//    private void sendDataToM1() {
//        // byte[] pPos = new byte[mServoCountInM*2];
//        // byte[] pColor = new byte[18*3];
//
//        // 从文件中获取动作数据
//        LogMgr.i("sendDataToM1");
//        if (mCurrentPosition + 4 < mFileLength) {
//            while (mCurrentPosition + 4 < mFileLength) {
//                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
//                    // 检测头
//                    // 帧长度 从机器人型号开始 没有算前四帧
//                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
//                            + 3] & 0xFF) << 8));
//                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
//                        byte check = 0x00;
//                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m
//                            // 到
//                            // 数据位结束
//                            // 加和
//                            check += mFileBuff[n];
//                        }
//                        check = (byte) check;
//
//                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
//                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_M1) {
//                                LogMgr.d("机器人型号正确 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
//                            } else {
//                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
//                            }
//                            mCurrentFrameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
//                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));
//
//                            byte leftWheelSpeed = mFileBuff[mCurrentPosition + 11];
//                            byte rightWheelSpeed = mFileBuff[mCurrentPosition + 13];
//                            LogMgr.d("leftWheelSpeed = " + (int) leftWheelSpeed + " rightWheelSpeed = "
//                                    + (int) rightWheelSpeed);
//
//                            byte headVerticalAngle = mFileBuff[mCurrentPosition + 15];
//                            LogMgr.d("headVerticalAngle = " + (int) headVerticalAngle);
//                            byte headHorizenAngle = mFileBuff[mCurrentPosition + 17];
//                            byte headHorizenAngle2 = mFileBuff[mCurrentPosition + 18];
//                            LogMgr.d("headHorizenAngle = " + (int) headHorizenAngle);
//
//                            readEyesColorM1();
//
//                            byte neckColorRed = mFileBuff[mCurrentPosition + 19 + 48];
//                            byte neckColorGreen = mFileBuff[mCurrentPosition + 19 + 49];
//                            byte neckColorBlue = mFileBuff[mCurrentPosition + 19 + 50];
//
//                            byte bottomColorRed = mFileBuff[mCurrentPosition + 19 + 51];
//                            byte bottomColorGreen = mFileBuff[mCurrentPosition + 19 + 52];
//                            byte bottomColorBlue = mFileBuff[mCurrentPosition + 19 + 53];
//
//                            if (mDownMCmd != null) {
//                                mDownMCmd.clear();
//                            } else {
//                                mDownMCmd = new ArrayList<byte[]>();
//                            }
//
//                            // 轮子速度命令
//                            if (leftWheelSpeed != lastLeftWheelSpeed || rightWheelSpeed != lastRightWheelSpeed
//                                    || mIsFirstFrame == true || leftWheelSpeed == (byte) 100
//                                    || rightWheelSpeed == (byte) 100) {
//                                LogMgr.d("组成轮子速度命令 当前帧数 mCurrentFrameNum = " + mCurrentFrameNum);
//                                if (GlobalConfig.isAdjustSpeedForMBin == true) {
//                                    if ((int) (leftWheelSpeed & 0xFF) != 100
//                                            && mIsNeedToAdjustSpeedForLeftWheel == true) {
//                                        LogMgr.d("左轮速度补偿 补偿前速度 leftWheelSpeed = " + (int) (leftWheelSpeed & 0xFF)
//                                                + " 补偿系数 = " + mNextCoefficientForLeftWheel);
//                                        mIsNeedToAdjustSpeedForLeftWheel = false;
//                                        leftWheelSpeed = getAdjustedWheelSpeed(leftWheelSpeed,
//                                                mNextCoefficientForLeftWheel);
//                                    }
//                                    if ((int) (rightWheelSpeed & 0xFF) != 100
//                                            && mIsNeedToAdjustSpeedForRightWheel == true) {
//                                        LogMgr.d("右轮速度补偿 补偿前速度 rightWheelSpeed = " + (int) (rightWheelSpeed & 0xFF)
//                                                + " 补偿系数 = " + mNextCoefficientForRightWheel);
//                                        mIsNeedToAdjustSpeedForRightWheel = false;
//                                        rightWheelSpeed = getAdjustedWheelSpeed(rightWheelSpeed,
//                                                mNextCoefficientForRightWheel);
//                                    }
//                                }
//                                LogMgr.d("速度补偿后 leftWheelSpeed = " + (int) (leftWheelSpeed & 0xFF)
//                                        + " rightWheelSpeed = " + (int) (rightWheelSpeed & 0xFF));
//
//                                wheelSpeedCmd = ProtocolUtils.getM1WheelSpeedCmd(leftWheelSpeed, rightWheelSpeed);
//                                lastLeftWheelSpeed = leftWheelSpeed;
//                                lastRightWheelSpeed = rightWheelSpeed;
//                                mDownMCmd.add(wheelSpeedCmd);
//                            }
//
//                            // 头部左右命令
//                            if (headHorizenAngle != lastHeadHorizenAngle || mIsFirstFrame == true) {
//                                headHorizenCmd = ProtocolUtils.getM1NeckMoveCmd((byte) 0x00, headHorizenAngle2,
//                                        headHorizenAngle);
//                                lastHeadHorizenAngle = headHorizenAngle;
//                                mDownMCmd.add(headHorizenCmd);
//                            }
//
//                            // 脖子灯光命令
//                            if (neckColorRed != lastNeckColorRed || neckColorGreen != lastNeckColorGreen
//                                    || neckColorBlue != lastNeckColorBlue || mIsFirstFrame == true) {
//                                byte[] bs = ProtocolUtils.getWaveform(1);
//                                mDownMCmd.add(bs);
//                                neckColorCmd = ProtocolUtils.getM1NeckColorCmd((byte) 0x32, neckColorRed,
//                                        neckColorGreen, neckColorBlue);
//                                lastNeckColorRed = neckColorRed;
//                                lastNeckColorGreen = neckColorGreen;
//                                lastNeckColorBlue = neckColorBlue;
//                                mDownMCmd.add(neckColorCmd);
//                            }
//
//                            // 底部灯光命令
//                            if (bottomColorRed != lastBottomColorRed || bottomColorGreen != lastBottomColorGreen
//                                    || bottomColorBlue != lastBottomColorBlue || mIsFirstFrame == true) {
//                                byte[] bs = ProtocolUtils.getWaveform(3);
//                                mDownMCmd.add(bs);
//                                bottomColorCmd = ProtocolUtils.getM1NeckColorCmd((byte) 0x33, bottomColorRed,
//                                        bottomColorGreen, bottomColorBlue);
//                                lastBottomColorRed = bottomColorRed;
//                                lastBottomColorGreen = bottomColorGreen;
//                                lastBottomColorBlue = bottomColorBlue;
//                                mDownMCmd.add(bottomColorCmd);
//                            }
//
//                            // 头部上下命令
//                            if (headVerticalAngle != lastHeadVerticalAngle || mIsFirstFrame == true) {
//                                headVerticalCmd = ProtocolUtils.getM1NeckMoveCmd((byte) 0x01, (byte) 0,
//                                        headVerticalAngle);
//                                lastHeadVerticalAngle = headVerticalAngle;
//                                mDownMCmd.add(headVerticalCmd);
//                            }
//
//                            // 眼部灯光命令
//                            // if(eyeColorRed != lastEyeColorRed ||
//                            // eyeColorGreen != lastEyeColorGreen ||
//                            // eyeColorBlue != lastEyeColorBlue || mIsFirstFrame
//                            // == true){
//                            // eyesColorCmd =
//                            // ProtocolUtils.getMEyeColorCmd(eyeColorRed,
//                            // eyeColorGreen, eyeColorBlue);
//                            eyesColorCmd = ProtocolUtils.buildProtocol((byte) ControlInitiator.ROBOT_TYPE_M1,
//                                    (byte) 0xa6, (byte) 0x31, m1EyeLightData);
//                            // lastEyeColorRed = eyeColorRed;
//                            // lastEyeColorGreen = eyeColorGreen;
//                            // lastEyeColorBlue = eyeColorBlue;
//                            mDownMCmd.add(eyesColorCmd);
//                            // }
//
//                            mIsFirstFrame = false;
//
//                            if (!mDownMCmd.isEmpty()) {
////                                new MFrameRunnable(mDownMCmd, mCurrentFrameNum).start();
//                                new Thread(new MFrameRunnable(mDownMCmd, mCurrentFrameNum)).start();
//                            }
//                            // Display(1, "成功解析一帧: " + m_sPos + "   总长度: " +
//                            // m_sFileLen);
//                            LogMgr.i("成功解析一帧: 当前帧序号 mCurrentFrameNum = " + mCurrentFrameNum + " mCurrentPosition = "
//                                    + mCurrentPosition);
//                            mCurrentPosition += (frameLength + 4);
//
//                            return;
//                        } else {
//                            mCurrentPosition++;
//                            LogMgr.e("校验和错误");
//                        }
//                    } else {
//                        mCurrentPosition++;
//                        LogMgr.e("最后一帧损坏");
//                        return;
//                    }
//                } else {
//                    mCurrentPosition++;
//                }
//            }
//        } else {
//            stopHandle();
//        }
//    }

//    /**
//     * @Description 读取M1眼睛灯光
//     * @author lz
//     * @time 2017-5-2 下午1:44:40
//     */
//    private void readEyesColorM1() {
//        for (int i = 0; i < 10; i++) {
//            if (i < 5) {
//                m1EyeLightData[(4 - i) * 3] = mFileBuff[mCurrentPosition + 19 + i * 3 + 9];
//                m1EyeLightData[(4 - i) * 3 + 1] = mFileBuff[mCurrentPosition + 19 + i * 3 + 1 + 9];
//                m1EyeLightData[(4 - i) * 3 + 2] = mFileBuff[mCurrentPosition + 19 + i * 3 + 2 + 9];
//            } else if (i == 9) {
//                m1EyeLightData[5 * 3] = mFileBuff[mCurrentPosition + 19 + i * 3 + 18];
//                m1EyeLightData[5 * 3 + 1] = mFileBuff[mCurrentPosition + 19 + i * 3 + 1 + 18];
//                m1EyeLightData[5 * 3 + 2] = mFileBuff[mCurrentPosition + 19 + i * 3 + 2 + 18];
//            } else {
//                m1EyeLightData[(i + 1) * 3] = mFileBuff[mCurrentPosition + 19 + i * 3 + 9];
//                m1EyeLightData[(i + 1) * 3 + 1] = mFileBuff[mCurrentPosition + 19 + i * 3 + 1 + 9];
//                m1EyeLightData[(i + 1) * 3 + 2] = mFileBuff[mCurrentPosition + 19 + i * 3 + 2 + 9];
//            }
//        }
//    }



    /**
     * 41毫秒发一帧，每一帧的动作解析 M机器人解析
     */
    private void sendDataToM() {
        // byte[] pPos = new byte[mServoCountInM*2];
        // byte[] pColor = new byte[18*3];

        // 从文件中获取动作数据
        LogMgr.i("sendDataToM");
        long time = System.currentTimeMillis();
        if (mCurrentPosition + 4 < mFileLength) {
            while (mCurrentPosition + 4 < mFileLength) {
                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
                    // 检测头帧长度 从机器人型号开始 没有算前四帧
                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
                            + 3] & 0xFF) << 8));
                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                        byte check = 0x00;
                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) {
                            // 从帧头到数据位结束加和
                            check += mFileBuff[n];
                        }
                        check = (byte) check;

                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_M) {
                                LogMgr.d("机器人型号正确 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
                            } else {
                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
                            }
                            mCurrentFrameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));

                            byte leftWheelSpeed = mFileBuff[mCurrentPosition + 11];
                            byte rightWheelSpeed = mFileBuff[mCurrentPosition + 13];
                            LogMgr.d("leftWheelSpeed = " + (int) (leftWheelSpeed&0xFF) + " rightWheelSpeed = "
                                    + (int) (rightWheelSpeed&0xFF));

                            byte headVerticalAngle = mFileBuff[mCurrentPosition + 15];
                            LogMgr.d("headVerticalAngle = " + (int) headVerticalAngle);
                            byte headHorizenAngle = mFileBuff[mCurrentPosition + 17];
                            byte headHorizenAngle2 = mFileBuff[mCurrentPosition + 18];
                            LogMgr.d("headHorizenAngle = " + (int) headHorizenAngle);

                            for (int i = 0; i < 16; i++) {
                                mEyeLightData[i * 3] = mFileBuff[mCurrentPosition + 19 + i * 3];
                                mEyeLightData[i * 3 + 1] = mFileBuff[mCurrentPosition + 19 + i * 3 + 1];
                                mEyeLightData[i * 3 + 2] = mFileBuff[mCurrentPosition + 19 + i * 3 + 2];
                            }


                            byte neckColorRed = mFileBuff[mCurrentPosition + 19 + 48];
                            byte neckColorGreen = mFileBuff[mCurrentPosition + 19 + 49];
                            byte neckColorBlue = mFileBuff[mCurrentPosition + 19 + 50];

                            byte bottomColorRed = mFileBuff[mCurrentPosition + 19 + 51];
                            byte bottomColorGreen = mFileBuff[mCurrentPosition + 19 + 52];
                            byte bottomColorBlue = mFileBuff[mCurrentPosition + 19 + 53];

                            if (mDownMCmd != null) {
                                mDownMCmd.clear();
                            } else {
                                mDownMCmd = new ArrayList<byte[]>();
                            }

                            // 轮子速度命令
                            if (leftWheelSpeed != lastLeftWheelSpeed || rightWheelSpeed != lastRightWheelSpeed
                                    || mIsFirstFrame == true || leftWheelSpeed == (byte) 100
                                    || rightWheelSpeed == (byte) 100) {
                                LogMgr.d("组成轮子速度命令 当前帧数 mCurrentFrameNum = " + mCurrentFrameNum);

                                wheelSpeedCmd = ProtocolUtils.getMWheelSpeedCmd(leftWheelSpeed, rightWheelSpeed);
                                lastLeftWheelSpeed = leftWheelSpeed;
                                lastRightWheelSpeed = rightWheelSpeed;
                                mDownMCmd.add(wheelSpeedCmd);
                            }

                            // 头部左右命令
                            if (headHorizenAngle != lastHeadHorizenAngle || mIsFirstFrame == true) {
                                headHorizenCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x00, headHorizenAngle,
                                        headHorizenAngle2);
                                lastHeadHorizenAngle = headHorizenAngle;
                                mDownMCmd.add(headHorizenCmd);
                            }

                            // 脖子灯光命令
                            if (neckColorRed != lastNeckColorRed || neckColorGreen != lastNeckColorGreen
                                    || neckColorBlue != lastNeckColorBlue || mIsFirstFrame == true) {
                                neckColorCmd = ProtocolUtils.getMNeckColorCmd(neckColorRed, neckColorGreen,
                                        neckColorBlue);
                                lastNeckColorRed = neckColorRed;
                                lastNeckColorGreen = neckColorGreen;
                                lastNeckColorBlue = neckColorBlue;
                                mDownMCmd.add(neckColorCmd);
                            }

                            // 底部灯光命令
                            if (bottomColorRed != lastBottomColorRed || bottomColorGreen != lastBottomColorGreen
                                    || bottomColorBlue != lastBottomColorBlue || mIsFirstFrame == true) {
                                bottomColorCmd = ProtocolUtils.getMBottomColorCmd(bottomColorRed, bottomColorGreen,
                                        bottomColorBlue);
                                lastBottomColorRed = bottomColorRed;
                                lastBottomColorGreen = bottomColorGreen;
                                lastBottomColorBlue = bottomColorBlue;
                                mDownMCmd.add(bottomColorCmd);
                            }

                            // 头部上下命令
                            if (headVerticalAngle != lastHeadVerticalAngle || mIsFirstFrame == true) {
                                headVerticalCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x01, headVerticalAngle);
                                lastHeadVerticalAngle = headVerticalAngle;
                                mDownMCmd.add(headVerticalCmd);
                            }

                            // 眼部灯光命令
                            eyesColorCmd = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_M,
                                    GlobalConfig.M_EYE_LIHGT_CMD_1, GlobalConfig.M_EYE_LIHGT_CMD_2, mEyeLightData);
                            mDownMCmd.add(eyesColorCmd);
                            // }

                            //播放动作过程中，监测前后超声
                            if(mCurrentFrameNum%2 == 1 && mIsMObstacleAvoidanceOpen){
                                byte[] tempCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
                                        GlobalConfig.M_SENSOR_OUT_CMD_1,GlobalConfig.M_SENSOR_OUT_CMD_2,null);
                                mDownMCmd.add(tempCmd);
                            }

                            mIsFirstFrame = false;

                            if (!mDownMCmd.isEmpty()) {
//                                new MFrameRunnable(mDownMCmd, mCurrentFrameNum).start();
//                                new Thread(new MFrameRunnable(mDownMCmd, mCurrentFrameNum)).start();
                                mMFramethreadPoolExecutor.execute(new MFrameRunnable(mDownMCmd, mCurrentFrameNum));
                            }

                            int stepLength = 1;
                            LogMgr.i("成功解析一帧: 当前帧序号 mCurrentFrameNum = " + mCurrentFrameNum + " mCurrentPosition = " + mCurrentPosition);
                            if(countOfPlayedFrame >= 125){
                                countOfPlayedFrame = 0;
                                stepLength = 2;
                                LogMgr.i("跳过下一帧 序号 = "+(mCurrentFrameNum + 1));
                            }

                            mCurrentPosition += stepLength*(frameLength + 4);

                            return;
                        } else {
                            mCurrentPosition++;
                            LogMgr.e("校验和错误");
                        }
                    } else {
                        mCurrentPosition++;
                        LogMgr.e("最后一帧损坏");
                        return;
                    }
                } else {
                    mCurrentPosition++;
                }
            }
        } else {
            stopHandle();
        }
    }

    /**
     * 41毫秒发一帧，每一帧的动作解析 C机器人解析
     */
    private void sendDataToC() {
        LogMgr.i("sendDataToC()");
        if (mCurrentPosition + 4 < mFileLength) {
            while (mCurrentPosition + 4 < mFileLength) {
                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
                    // 检测头
                    // 帧长度 从机器人型号开始 没有算前四帧
                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
                            + 3] & 0xFF) << 8));
                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                        byte check = 0x00;
                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m
                            // 到
                            // 数据位结束
                            // 加和
                            check += mFileBuff[n];
                        }
                        check = (byte) check;

                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_C) {
                                LogMgr.d("机器人型号正确 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
                            } else {
                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
                            }
                            mCurrentFrameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));

                            mLeftWheelSpeedForC = (int) (mFileBuff[mCurrentPosition + 11]) + 100;
                            mRightWheelSpeedForC = (int) (mFileBuff[mCurrentPosition + 13]) + 100;
                            mShoulderAngleForC = (int) ((mFileBuff[mCurrentPosition + 15] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 16] & 0xFF) << 8));
                            if (mShoulderAngleForC > 32768) {
                                mShoulderAngleForC -= 65536;
                            }

                            // 这一帧肩膀的相对位移
                            int shoulderRelativeAngleForC = mShoulderAngleForC - mLastShoulderAngleForC;
                            // 这一帧肩膀的速度
                            int shoulderRelativeSpeedForC = (int) (1.0 * shoulderRelativeAngleForC * (Math.PI * 6)
                                    / 360 * framesInC);
                            LogMgr.i("mLeftWheelSpeedForC = " + (int) mLeftWheelSpeedForC + " mRightWheelSpeedForC = "
                                    + (int) mRightWheelSpeedForC + " mShoulderAngleForC = " + mShoulderAngleForC
                                    + " mLastShoulderAngleForC = " + mLastShoulderAngleForC
                                    + " shoulderRelativeSpeedForC = " + shoulderRelativeSpeedForC);

                            int tempShoulderRelativeSpeedForC;
                            if (shoulderRelativeSpeedForC > 0) {
                                tempShoulderRelativeSpeedForC = 200;
                            } else if (shoulderRelativeSpeedForC < 0) {
                                tempShoulderRelativeSpeedForC = 0;
                            } else {
                                tempShoulderRelativeSpeedForC = 100;
                            }

                            byte[] data = new byte[21];
                            if (mCurrentFrameNum % 6 == 5 || mCurrentFrameNum == mTotalFrameNum) {
                                LogMgr.d("下发脖子角度 mCurrentFrameNum = " + mCurrentFrameNum
                                        + " tempShoulderRelativeSpeedForC = " + tempShoulderRelativeSpeedForC);
                                data = new byte[21];
                                data[0] = (byte) 0x0E;
                                // 左轮数据
                                byte[] dataOfLeftWheel = new byte[5];
                                dataOfLeftWheel[0] = (byte) 0x01; // 电机类型：1为大电机，0为小电机；
                                dataOfLeftWheel[1] = (byte) 0x00; // 闭环参数：0为速度，1为圈数，2为角度；
                                dataOfLeftWheel[2] = (byte) mLeftWheelSpeedForC;
                                // 右轮数据
                                byte[] dataOfRightWheel = new byte[5];
                                dataOfRightWheel[0] = (byte) 0x01; // 电机类型：1为大电机，0为小电机；
                                dataOfRightWheel[1] = (byte) 0x00; // 闭环参数：0为速度，1为圈数，2为角度；
                                dataOfRightWheel[2] = (byte) mRightWheelSpeedForC;
                                // 肩膀数据
                                byte[] dataOfShoulder = new byte[5];
                                dataOfShoulder[0] = (byte) 0x00; // 电机类型：1为大电机，0为小电机；
                                dataOfShoulder[1] = (byte) 0x02; // 闭环参数：0为速度，1为圈数，2为角度；
                                dataOfShoulder[2] = (byte) tempShoulderRelativeSpeedForC;
                                dataOfShoulder[3] = (byte) ((Math.abs(shoulderRelativeAngleForC) >> 8) & 0xFF);
                                dataOfShoulder[4] = (byte) (Math.abs(shoulderRelativeAngleForC) & 0xFF);

                                System.arraycopy(dataOfLeftWheel, 0, data, 1, dataOfLeftWheel.length);
                                System.arraycopy(dataOfRightWheel, 0, data, 6, dataOfRightWheel.length);
                                System.arraycopy(dataOfShoulder, 0, data, 11, dataOfShoulder.length);

                                mLastShoulderAngleForC = mShoulderAngleForC;
                                // mLastFrameForNeck = mCurrentFrameNum;
                            } else {
                                data = new byte[21];
                                data[0] = (byte) 0x0C;
                                // 左轮数据
                                byte[] dataOfLeftWheel = new byte[5];
                                dataOfLeftWheel[0] = (byte) 0x01; // 电机类型：1为大电机，0为小电机；
                                dataOfLeftWheel[1] = (byte) 0x00; // 闭环参数：0为速度，1为圈数，2为角度；
                                dataOfLeftWheel[2] = (byte) mLeftWheelSpeedForC;
                                // 右轮数据
                                byte[] dataOfRightWheel = new byte[5];
                                dataOfRightWheel[0] = (byte) 0x01; // 电机类型：1为大电机，0为小电机；
                                dataOfRightWheel[1] = (byte) 0x00; // 闭环参数：0为速度，1为圈数，2为角度；
                                dataOfRightWheel[2] = (byte) mRightWheelSpeedForC;

                                System.arraycopy(dataOfLeftWheel, 0, data, 1, dataOfLeftWheel.length);
                                System.arraycopy(dataOfRightWheel, 0, data, 6, dataOfRightWheel.length);
                            }

                            byte[] cmdForC = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C,
                                    GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_1,
                                    GlobalConfig.C_CLOSED_LOOP_MOTOR_OUT_CMD_2, data);

                            try {
                                SP.write(cmdForC);

                                if (!ProtocolUtils.isFeedbackCorrect(ProtocolUtils.getFeedbackData())) {
                                    LogMgr.e("C系列sendDataToC 底层反馈错误");
                                } else {
                                    LogMgr.d("C系列sendDataToC 底层反馈正确");
                                }
                            } catch (Exception e) {
                                LogMgr.e("C系列sendDataToC异常" + e.getMessage());
                                e.printStackTrace();
                            }

                            mIsFirstFrame = false;

                            LogMgr.i("成功解析一帧: 当前帧序号 mCurrentFrameNum = " + mCurrentFrameNum + " mCurrentPosition = "
                                    + mCurrentPosition);

                            mCurrentPosition += (frameLength + 4);

                            return;
                        } else {
                            LogMgr.e("校验和错误 check期望值 = " + check + " 实际值 = "
                                    + mFileBuff[mCurrentPosition + frameLength + 3]);
                            mCurrentPosition++;
                        }
                    } else {
                        mCurrentPosition++;
                        LogMgr.e("最后一帧损坏");
                        return;
                    }
                } else {
                    mCurrentPosition++;
                }
            }
        } else {
            stopHandle();
        }
    }

//    /**
//     * 使用位移定位法顺序执行M的bin文件
//     */
//    private void sendDataToMWithDisplacement() {
//        // 从文件中获取动作数据
//        if (mCurrentPosition + 4 < mFileLength) {
//            while (mCurrentPosition + 4 < mFileLength) {
//                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
//                    // 检测头
//                    // 帧长度 从机器人型号开始 没有算前四帧
//                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
//                            + 3] & 0xFF) << 8));
//                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
//                        byte check = 0x00;
//                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m
//                            // 到
//                            // 数据位结束
//                            // 加和
//                            check += mFileBuff[n];
//                        }
//                        check = (byte) check;
//
//                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
//                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_M) {
//                                LogMgr.d("机器人型号正确 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
//                            } else {
//                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[mCurrentPosition + 4]);
//                            }
//                            mCurrentFrameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
//                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));
//
//                            byte leftWheelSpeed = mFileBuff[mCurrentPosition + 11];
//                            byte rightWheelSpeed = mFileBuff[mCurrentPosition + 13];
//
//                            int leftWheelSpeedInt = (int) (leftWheelSpeed & 0xFF);
//                            int rightWheelSpeedInt = (int) (rightWheelSpeed & 0xFF);
//                            if (leftWheelSpeedInt >= 96 && leftWheelSpeedInt <= 104) {
//                                leftWheelSpeed = (byte) (100 & 0xFF);
//                            }
//                            if (rightWheelSpeedInt >= 96 && rightWheelSpeedInt <= 104) {
//                                rightWheelSpeed = (byte) (100 & 0xFF);
//                            }
//                            LogMgr.d("leftWheelSpeed = " + (int) leftWheelSpeed + " rightWheelSpeed = "
//                                    + (int) rightWheelSpeed);
//
//                            byte headVerticalAngle = mFileBuff[mCurrentPosition + 15];
//                            LogMgr.d("headVerticalAngle = " + (int) headVerticalAngle);
//                            byte headHorizenAngle = mFileBuff[mCurrentPosition + 17];
//                            byte headHorizenAngle2 = mFileBuff[mCurrentPosition + 18];
//                            LogMgr.d("headHorizenAngle = " + (int) headHorizenAngle);
//
//                            byte eyeColorRed = mFileBuff[mCurrentPosition + 19];
//                            byte eyeColorGreen = mFileBuff[mCurrentPosition + 20];
//                            byte eyeColorBlue = mFileBuff[mCurrentPosition + 21];
//
//                            byte neckColorRed = mFileBuff[mCurrentPosition + 19 + 48];
//                            byte neckColorGreen = mFileBuff[mCurrentPosition + 19 + 49];
//                            byte neckColorBlue = mFileBuff[mCurrentPosition + 19 + 50];
//
//                            byte bottomColorRed = mFileBuff[mCurrentPosition + 19 + 51];
//                            byte bottomColorGreen = mFileBuff[mCurrentPosition + 19 + 52];
//                            byte bottomColorBlue = mFileBuff[mCurrentPosition + 19 + 53];
//
//                            if (mDownMCmd != null) {
//                                mDownMCmd.clear();
//                            } else {
//                                mDownMCmd = new ArrayList<byte[]>();
//                            }
//
//                            // 轮子速度命令
//                            if (leftWheelSpeed != lastLeftWheelSpeed || rightWheelSpeed != lastRightWheelSpeed
//                                    || mIsFirstFrame == true || leftWheelSpeed == (byte) 100
//                                    || rightWheelSpeed == (byte) 100) {
//                                LogMgr.d("组成轮子速度命令");
//                                wheelSpeedCmd = ProtocolUtils.getMWheelSpeedCmd(leftWheelSpeed, rightWheelSpeed);
//                                lastLeftWheelSpeed = leftWheelSpeed;
//                                lastRightWheelSpeed = rightWheelSpeed;
//                                mDownMCmd.add(wheelSpeedCmd);
//                            }
//
//                            // 头部左右命令
//                            if (headHorizenAngle != lastHeadHorizenAngle || mIsFirstFrame == true) {
//                                headHorizenCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x00, headHorizenAngle,
//                                        headHorizenAngle2);
//                                lastHeadHorizenAngle = headHorizenAngle;
//                                mDownMCmd.add(headHorizenCmd);
//                            }
//
//                            // 眼部灯光命令
//                            if (eyeColorRed != lastEyeColorRed || eyeColorGreen != lastEyeColorGreen
//                                    || eyeColorBlue != lastEyeColorBlue || mIsFirstFrame == true) {
//                                eyesColorCmd = ProtocolUtils.getMEyeColorCmd(eyeColorRed, eyeColorGreen, eyeColorBlue);
//                                lastEyeColorRed = eyeColorRed;
//                                lastEyeColorGreen = eyeColorGreen;
//                                lastEyeColorBlue = eyeColorBlue;
//                                mDownMCmd.add(eyesColorCmd);
//                            }
//
//                            // 脖子灯光命令
//                            if (neckColorRed != lastNeckColorRed || neckColorGreen != lastNeckColorGreen
//                                    || neckColorBlue != lastNeckColorBlue || mIsFirstFrame == true) {
//                                neckColorCmd = ProtocolUtils.getMNeckColorCmd(neckColorRed, neckColorGreen,
//                                        neckColorBlue);
//                                lastNeckColorRed = neckColorRed;
//                                lastNeckColorGreen = neckColorGreen;
//                                lastNeckColorBlue = neckColorBlue;
//                                mDownMCmd.add(neckColorCmd);
//                            }
//
//                            // 底部灯光命令
//                            if (bottomColorRed != lastBottomColorRed || bottomColorGreen != lastBottomColorGreen
//                                    || bottomColorBlue != lastBottomColorBlue || mIsFirstFrame == true) {
//                                bottomColorCmd = ProtocolUtils.getMBottomColorCmd(bottomColorRed, bottomColorGreen,
//                                        bottomColorBlue);
//                                lastBottomColorRed = bottomColorRed;
//                                lastBottomColorGreen = bottomColorGreen;
//                                lastBottomColorBlue = bottomColorBlue;
//                                mDownMCmd.add(bottomColorCmd);
//                            }
//
//                            // 头部上下命令
//                            if (headVerticalAngle != lastHeadVerticalAngle || mIsFirstFrame == true) {
//                                headVerticalCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x01, headVerticalAngle);
//                                lastHeadVerticalAngle = headVerticalAngle;
//                                mDownMCmd.add(headVerticalCmd);
//                            }
//
//                            mIsFirstFrame = false;
//
//                            // if(!mDownMCmd.isEmpty()){
////                            new MFrameRunnable(mDownMCmd, mCurrentFrameNum).start();
//                            new Thread(new MFrameRunnable(mDownMCmd, mCurrentFrameNum)).start();
//                            // }
//                            // Display(1, "成功解析一帧: " + m_sPos + "   总长度: " +
//                            // m_sFileLen);
//                            LogMgr.v("成功解析一帧: 当前帧序号 frameNum = " + mCurrentFrameNum + " mCurrentPosition = "
//                                    + mCurrentPosition);
//
//                            mCurrentPosition += (frameLength + 4);
//
//                            return;
//                        } else {
//                            mCurrentPosition++;
//                            LogMgr.e("校验和错误");
//                        }
//                    } else {
//                        mCurrentPosition++;
//                        LogMgr.e("最后一帧损坏");
//                        return;
//                    }
//                } else {
//                    mCurrentPosition++;
//                }
//            }
//        } else {
//            stopHandle();
//        }
//    }

    /**
     * 设置舵机速度
     * @param speed
     */
    public void setEngineSpeed(int speed){
        try {
            ProtocolUtils.setEngineSpeed(iCount, pID, null, speed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据是否有要发的手脚灯光命令 往串口1发送命令
     */
    private void handleHandAndFootLight() {
        try {
            if (cmdToSendToEngine1List != null && cmdToSendToEngine1List.size() > 0) {
                LogMgr.v("往底层串口1传数据 cmdToSendToEngine1List.size = " + cmdToSendToEngine1List.size());
                for (int h = 0; h < cmdToSendToEngine1List.size(); h++) {
                    SP.requestVice(cmdToSendToEngine1List.get(h));
                }
            } else {
                LogMgr.v("cmdToSendToEngine1List == null is " + (cmdToSendToEngine1List == null));
                if (cmdToSendToEngine1List != null) {
                    LogMgr.v("cmdToSendToEngine1List.size = " + cmdToSendToEngine1List.size());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("handleHandAndFootLight()");
        } finally {
            if (cmdToSendToEngine1List != null) {
                cmdToSendToEngine1List.clear();
            }

        }
    }

    /**
     * 头部灯光
     *
     * @Description 方法描述
     * @author lz
     * @time 2017-7-15 上午11:37:06
     */
    private void headColor(byte[] buf) {
        byte[] bs = new byte[buf.length + 2];
        System.arraycopy(buf, 0, bs, 0, buf.length);
        bs[buf.length] = (byte) 10;
        bs[buf.length + 1] = (byte) 10;
        byte[] bytesToSend = ProtocolBuilder.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                (byte) 0xA3, (byte) 0x74, bs);
//		LogMgr.e("眼睛命令：" + Utils.bytesToString(bytesToSend, bytesToSend.length));
        SP.write(bytesToSend);
    }

    /**
     * @Description 关闭S灯光
     * @author lz
     * @time 2017-7-15 下午2:43:51
     */
    private void closeSColor() {
        // 关闭头部灯光
        headColor(new byte[3]);
    }

    /**
     * 构造但不执行控制脚部，手部灯光的命令
     *
     * @param pColor 头部0-2，左耳3-5，左眼6-8，左脚9-11，左手12-14，右耳15-17，右眼18-20，右脚21-23，右手24-
     *               26 9*3
     * @return
     * @author Yang
     */
    private boolean setColorCmd2(byte[] pColor) {

        LogMgr.v("setColorCmd2()");

        if (cmdToSendToEngine1List == null) {
            cmdToSendToEngine1List = new ArrayList<byte[]>();
        } else {
            cmdToSendToEngine1List.clear();
        }
        // List<byte[]> cmdToSendToEngine1List = new ArrayList<byte[]>();

        // 左手灯光构造
        byte leftHandTurnOnOrOff = (pColor[12] == 0x00 && pColor[13] == 0x00 && pColor[14] == 0x00) ? (byte) 0x00
                : (byte) 0x01;
        if (leftHandTurnOnOrOff != lastLeftHandTurnOnOrOff || mIsFirstFrame == true) {
            LogMgr.v("左手灯光构造");
            lastLeftHandTurnOnOrOff = leftHandTurnOnOrOff;
            byte[] leftHandCmd = getOnOrOffCmd(19, leftHandTurnOnOrOff);
            cmdToSendToEngine1List.add(leftHandCmd);
        }
        // 右手灯光构造
        byte rightHandTurnOnOrOff = (pColor[24] == 0x00 && pColor[25] == 0x00 && pColor[26] == 0x00) ? (byte) 0x00
                : (byte) 0x01;
        if (rightHandTurnOnOrOff != lastRightHandTurnOnOrOff || mIsFirstFrame == true) {
            LogMgr.v("右手灯光构造");
            lastRightHandTurnOnOrOff = rightHandTurnOnOrOff;
            byte[] rightHandCmd = getOnOrOffCmd(20, rightHandTurnOnOrOff);
            cmdToSendToEngine1List.add(rightHandCmd);
        }
        // 左脚灯光构造
        byte leftFeetTurnOnOrOff = (pColor[9] == 0x00 && pColor[10] == 0x00 && pColor[11] == 0x00) ? (byte) 0x01
                : (byte) 0x00;
        if (leftFeetTurnOnOrOff != lastLeftFeetTurnOnOrOff || mIsFirstFrame == true) {
            LogMgr.v("左脚灯光构造");
            lastLeftFeetTurnOnOrOff = leftFeetTurnOnOrOff;
            byte[] leftFeetCmd = getOnOrOffCmd(23, leftFeetTurnOnOrOff);
            cmdToSendToEngine1List.add(leftFeetCmd);
        }
        // 右脚灯光构造
        byte rightFeetTurnOnOrOff = (pColor[21] == 0x00 && pColor[22] == 0x00 && pColor[23] == 0x00) ? (byte) 0x01
                : (byte) 0x00;
        if (rightFeetTurnOnOrOff != lastRightFeetTurnOnOrOff || mIsFirstFrame == true) {
            LogMgr.v("右脚灯光构造");
            lastRightFeetTurnOnOrOff = rightFeetTurnOnOrOff;
            byte[] rightFeetCmd = getOnOrOffCmd(24, rightFeetTurnOnOrOff);
            cmdToSendToEngine1List.add(rightFeetCmd);
        }

        // if(leftHandTurnOnOrOff == (byte)0x01){
        // LogMgr.d(TAG, "左手亮");
        // }else{
        // LogMgr.d(TAG, "左手灭");
        // }
        // if(rightHandTurnOnOrOff == (byte)0x01){
        // LogMgr.d(TAG, "右手亮");
        // }else{
        // LogMgr.d(TAG, "右手灭");
        // }
        // if(leftFeetTurnOnOrOff == (byte)0x00){
        // LogMgr.d(TAG, "左脚亮");
        // }else{
        // LogMgr.d(TAG, "左脚灭");
        // }
        // if(rightFeetTurnOnOrOff == (byte)0x00){
        // LogMgr.d(TAG, "右脚亮");
        // }else{
        // LogMgr.d(TAG, "右脚灭");
        // }

        mIsFirstFrame = false;
        LogMgr.v("setColorCmd2() cmdToSendToEngine1List.size = " + cmdToSendToEngine1List.size());

        // byte[] leftHandCmd = getOnOrOffCmd(19,leftHandTurnOnOrOff );
        // byte[] rightHandCmd = getOnOrOffCmd(20,rightHandTurnOnOrOff);
        // byte[] leftFeetCmd = getOnOrOffCmd(23,leftFeetTurnOnOrOff );
        // byte[] rightFeetCmd = getOnOrOffCmd(24,rightFeetTurnOnOrOff);
        //
        // cmdToSendToEngine1List.add(leftHandCmd);
        // cmdToSendToEngine1List.add(rightHandCmd);
        // cmdToSendToEngine1List.add(leftFeetCmd);
        // cmdToSendToEngine1List.add(rightFeetCmd);

        // try {
        // if(cmdToSendToEngine1List != null &&
        // cmdToSendToEngine1List.size()>0){
        // LogMgr.d(TAG, "往底层串口1传数据3");
        // for(int h = 0 ; h<cmdToSendToEngine1List.size() ; h++){
        // SerialPortCommunicator.getInstance().writeH1(cmdToSendToEngine1List.get(h),0);
        // Thread.sleep(5);
        // }
        // }else{
        // LogMgr.d(TAG, "cmdToSendToEngine1List == null is " +
        // (cmdToSendToEngine1List == null));
        // if(cmdToSendToEngine1List!= null){
        // LogMgr.d(TAG, "cmdToSendToEngine1List.size = " +
        // cmdToSendToEngine1List.size());
        // }
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // return false;
        // }

        return true;
    }

    /**
     * 构造并执行控制头部，眼部，耳部灯光的命令
     *
     * @param pColor 头部0-2，左耳3-5，左眼6-8，左脚9-11，左手12-14，右耳15-17，右眼18-20，右脚21-23，右手24-
     *               26 9*3
     * @return
     * @author Yang
     */
    private boolean setAndExeColorCmd1(byte[] pColor) {

        LogMgr.v("getColorCmd1()");

        byte[] send = new byte[10];
        send[0] = 2;
        send[1] = 2;
        send[2] = 2;
        send[3] = pColor[6];
        send[4] = pColor[7];
        send[5] = pColor[8];
        send[6] = 3;
        send[7] = pColor[18];
        send[8] = pColor[19];
        send[9] = pColor[20];
//        for (int i = 0; i < 5; i++) {
//            send[2+4*i] = (byte) (i+1);
//            send[2 + 4*i + 1] = pColor[3*i];
//            send[2 + 4*i + 2] = pColor[3*i + 1];
//            send[2 + 4*i + 3] = pColor[3*i + 2];
//        }
//        byte[] colorCmd = new byte[20];
//        byte[] colorCmd = new byte[15];
//
////        colorCmd[0] = (byte) 0xAA;
////        colorCmd[1] = (byte) 0x53;
//        // 头部颜色
//        colorCmd[0] = pColor[0];
//        colorCmd[1] = pColor[1];
//        colorCmd[2] = pColor[2];
//        LogMgr.v("头部 pColor[0] = " + pColor[0] + " pColor[1] = " + pColor[1] + " pColor[2] = " + pColor[2]);
//        // 左眼灯光
//        colorCmd[3] = pColor[6];
//        colorCmd[4] = pColor[7];
//        colorCmd[5] = pColor[8];
//        LogMgr.v("左眼 pColor[6] = " + pColor[6] + " pColor[7] = " + pColor[7] + " pColor[8] = " + pColor[8]);
//        // 右眼灯光
//        colorCmd[6] = pColor[18];
//        colorCmd[7] = pColor[19];
//        colorCmd[8] = pColor[20];
//        LogMgr.v("右眼 pColor[18] = " + pColor[18] + " pColor[19] = " + pColor[19] + " pColor[20] = " + pColor[20]);
//        // 左耳灯光
//        colorCmd[9] = pColor[3];
//        colorCmd[10] = pColor[4];
//        colorCmd[11] = pColor[5];
//        LogMgr.v("左耳 pColor[3] = " + pColor[3] + " pColor[4] = " + pColor[4] + " pColor[5] = " + pColor[5]);
//        // 右耳灯光
//        colorCmd[12] = pColor[15];
//        colorCmd[13] = pColor[16];
//        colorCmd[14] = pColor[17];
//        LogMgr.v("右耳 pColor[15] = " + pColor[15] + " pColor[16] = " + pColor[16] + " pColor[17] = " + pColor[17]);
//        // 结尾3字节
////        colorCmd[17] = (byte) 0x00;
////        colorCmd[18] = (byte) 0x00;
////        colorCmd[19] = (byte) 0x00;

//        byte[] cmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),(byte)0xA3,(byte)0x68,colorCmd);
        byte[] cmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),(byte)0xA3,(byte)0xC0,send);

        LogMgr.v("往底层串口0传数据");
        try {
            Thread.sleep(5);
            SP.write(cmd);
            LogMgr.v("往底层串口0传头部灯光数据");
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("writeH0 write data error::" + e);
        }
        // try {
        // Log.d(TAG, "往底层串口0传数据");
        // main.mOutputStream.write(colorCmd);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        return true;
    }

    /**
     * 构建控制手脚上灯光的下传命令
     *
     * @param engineNo    舵机号
     * @param TurnOnOrOff 打开还是关闭 手臂1是打开 脚0是打开
     * @return
     * @author Yang
     */
    private byte[] getOnOrOffCmd(int engineNo, byte TurnOnOrOff) {

        LogMgr.v("手脚 engineNo = " + engineNo + " TurnOnOrOff = " + TurnOnOrOff);

        byte checkSum = 0;

        byte[] Cmd = new byte[20];
        Cmd[0] = (byte) 0xFE;
        Cmd[1] = (byte) 0x68;
        Cmd[2] = (byte) 'Z';
        Cmd[3] = (byte) 0x00;
        Cmd[4] = (byte) 0x00;
        Cmd[5] = (byte) 0x08;
        Cmd[6] = (byte) 0xFF;
        Cmd[7] = (byte) 0xFF;
        Cmd[8] = (byte) engineNo;
        // Cmd[8] = (byte) 19;
        Cmd[9] = (byte) 0x04;
        Cmd[10] = (byte) 0x03;
        Cmd[11] = (byte) 0x19;
        Cmd[12] = TurnOnOrOff;
        // if(lightFlag == false ){
        // Cmd[12] = (byte) 0x01;
        // lightFlag = true;
        // }else{
        // Cmd[12] = (byte) 0x00;
        // lightFlag = false;
        // }

        for (int k = 8; k <= 12; k++) {
            checkSum += Cmd[k];
        }
        checkSum = (byte) ~checkSum;
        checkSum = (byte) (checkSum & 0xFF);

        Cmd[13] = checkSum;
        Cmd[14] = (byte) 0xAA;
        Cmd[15] = (byte) 0x16;
        Cmd[16] = (byte) 0x00;
        Cmd[17] = (byte) 0x00;
        Cmd[18] = (byte) 0x00;
        Cmd[19] = (byte) 0x00;

        // LogMgr.d(TAG, "Cmd = " + Arrays.toString(Cmd));

        return Cmd;

    }

    /**
     * 判断动作结束时是否将机器人定为初始状态
     *
     * @param actionName
     * @return
     */
    private boolean isMoveLeadToInitialStatus(String actionName) {
        if (actionName.contains("beg") || actionName.contains("mid")) {
            return false;
        }
        return true;
    }

    /**
     * 是否属于停止动作命令
     *
     * @param cmd
     * @return
     */
    private boolean isStopCmd(String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            LogMgr.e("命令为空 错误");
            return false;
        }
        if (cmd.contains(GlobalConfig.MOVE_FORWARD_END) || cmd.contains(GlobalConfig.MOVE_BACK_END)
                || cmd.contains(GlobalConfig.MOVE_LEFT_END) || cmd.contains(GlobalConfig.MOVE_RIGH_END)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 解析文件头
     *
     * @return 文件头是否合法
     */
    private boolean analyzeFileHead() {
        boolean result = true;

        if (mFileBuff[0] == (byte) 0xAA && (mFileBuff[1] == (byte) 0x57 || mFileBuff[1] == (byte) 0x58)) {
            if ((mFileBuff[2] & 0x01) != 0) {
                isMoveEnable = true;
            } else {
                isMoveEnable = false;
            }
            if ((mFileBuff[2] & 0x02) != 0) {
                isLightEnable = true;
                if ((mFileBuff[3] & 0x01) != 0) {
                    isLightInHandFootEnable = true;
                } else {
                    isLightInHandFootEnable = false;
                }
            } else {
                isLightEnable = false;
                isLightInHandFootEnable = false;
            }

            mTotalFrameNum = (int) ((mFileBuff[8] & 0xFF) | ((mFileBuff[9] & 0xFF) << 8)
                    | ((mFileBuff[10] & 0xFF) << 16) | ((mFileBuff[11] & 0xFF) << 18));
            mVersion = (int)( (mFileBuff[12] & 0xFF) | ((mFileBuff[13] & 0xFF) << 8)  );
            LogMgr.i("总帧数 mTotalFrameNum = " + mTotalFrameNum + " isMoveEnable = " + isMoveEnable + " isLightEnable = "
                    + isLightEnable + " isLightInHandFootEnable = " + isLightInHandFootEnable + " mVersion = "+mVersion +
                    " ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H is " + (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H));
            if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
                if(mVersion > 0){
                    mServoCount = 22;
                    iCount = (byte) mServoCount;
                    pID = new byte[mServoCount];
                    for (int n = 0; n < mServoCount; n++) {
                        pID[n] = (byte) (n + 1);
                    }
                }else{
                    mServoCount = 23;
                    iCount = (byte) mServoCount;
                    pID = new byte[mServoCount];
                    for (int n = 0; n < mServoCount; n++) {
                        pID[n] = (byte) (n); // 0~22
                    }
                }

                if(isSpeedUpH){
                    if(handlerThreadForH5 == null){
                        handlerThreadForH5 = new HandlerThread("handlerThreadForH5");
                        handlerThreadForH5.start();
                        handlerForH5 = new Handler(handlerThreadForH5.getLooper());
                    }
                }
            }
            mCurrentPosition = 112; // 2 + 1 + 1 + 4 + 4 + 100

        } else {
            result = false;
            LogMgr.e("bin文件头错误");
        }

        return result;
    }

    /**
     * 判断动作在上一个的动作的后面是否合法
     *
     * @param action
     * @return
     */
    private boolean isMoveLegalAfterLastMove(String action) {

        if (!GlobalConfig.isMoveFilterActive) {
            return true;
        }

        // if(isSoundFile(action)){
        // return true;
        // }

        if (TextUtils.isEmpty(action)) {
            return true;
        }

        if (action.toLowerCase().endsWith(".bin")) {
            // 带“beg”的命令 要在机器人在初始状态时才有效
            if (action.contains(GlobalConfig.MOVE_FORWARD_BEGIN) || action.contains(GlobalConfig.MOVE_BACK_BEGIN)) {
                if (isRobotInitial) {
                    return true;
                } else {
                    return false;
                }
            }
            // 带“mid”的命令 要在beg或mid之后时才有效
            else if (action.contains(GlobalConfig.MOVE_FORWARD_MIDDLE)) {
                // if(GlobalConfig.MOVE_FORWARD_BEGIN.equals(lastRobotAction) ||
                // GlobalConfig.MOVE_FORWARD_MIDDLE.equals(lastRobotAction)){
                if (!TextUtils.isEmpty(lastRobotAction)
                        && (lastRobotAction.contains(GlobalConfig.MOVE_FORWARD_BEGIN) || lastRobotAction
                        .contains(GlobalConfig.MOVE_FORWARD_MIDDLE))) {
                    return true;
                } else {
                    return false;
                }
            } else if (action.contains(GlobalConfig.MOVE_BACK_MIDDLE)) {
                // if(GlobalConfig.MOVE_BACK_BEGIN.equals(lastRobotAction) ||
                // GlobalConfig.MOVE_BACK_MIDDLE.equals(lastRobotAction)){
                if (!TextUtils.isEmpty(lastRobotAction)
                        && (lastRobotAction.contains(GlobalConfig.MOVE_BACK_BEGIN) || lastRobotAction
                        .contains(GlobalConfig.MOVE_BACK_MIDDLE))) {
                    return true;
                } else {
                    return false;
                }
            }
            // 带“end”的命令 要在mid之后时才有效
            else if (action.contains(GlobalConfig.MOVE_FORWARD_END)) {
                // LogMgr.d(TAG, "lastRobotMove = "+lastRobotAction);
                // if(GlobalConfig.MOVE_FORWARD_BEGIN.equals(lastRobotAction) ||
                // GlobalConfig.MOVE_FORWARD_MIDDLE.equals(lastRobotAction)){
                if (!TextUtils.isEmpty(lastRobotAction)
                        && (lastRobotAction.contains(GlobalConfig.MOVE_FORWARD_BEGIN) || lastRobotAction
                        .contains(GlobalConfig.MOVE_FORWARD_MIDDLE))) {
                    return true;
                } else {
                    return false;
                }
            } else if (action.contains(GlobalConfig.MOVE_BACK_END)) {
                // if(GlobalConfig.MOVE_BACK_BEGIN.equals(lastRobotAction) ||
                // GlobalConfig.MOVE_BACK_MIDDLE.equals(lastRobotAction)){
                if (!TextUtils.isEmpty(lastRobotAction)
                        && (lastRobotAction.contains(GlobalConfig.MOVE_BACK_BEGIN) || lastRobotAction
                        .contains(GlobalConfig.MOVE_BACK_MIDDLE))) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断文件是否是音频文件 通过文件格式是否是wav或mp3确定
     *
     * @param fileName
     * @return
     */
    @SuppressLint("DefaultLocale")
    private boolean isSoundFile(String fileName) {

        if (TextUtils.isEmpty(fileName)) {
            return false;
        }

        boolean result = false;
        if (fileName.toLowerCase().endsWith(".wav") || fileName.toLowerCase().endsWith(".mp3")) {
            result = true;
        }
        return result;
    }

    /**
     * 设置音频/视频播放结束时的回调
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (lastRobotMoveType == MOVE_TYPE_SOUND) {
            LogMgr.d("音频播放结束，设置机器人为不在动作状态");
            isRobotMoving = false;
            isForceStoped = false;
            if (isNeedToReply) {
                // 当前命令是SkillPaly发出的
                isNeedToReply = false;
                setPaused(false);
                LogMgr.d("当前命令是SkillPlay发出的 播放结束 发送返回命令 ");
                // 需要向SkillPlay发送播放结束广播
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_COMPLETE, null);
                // mSerialPortActivity.mServerTcp.mAcceptClient.mHandler.obtainMessage(3).sendToTarget();
            }
            if (mPlayCallBack != null) {
                mPlayCallBack.onStop();
                mPlayCallBack = null;
            }
        }
    }

    /**
     * 播放完后续处理
     */
    private void stopHandle() {
        // 文件播放结束
        StopTimer();
//        stopGetDisplacementTimer();
        LogMgr.i("文件播放结束");
        if (mPlayer != null) {
            LogMgr.d("mediaPlayer.stop()");
            mPlayer.stop();
        }
//        if((mPlayMode&PLAY_MODE_VEDIO)!=0){
//            LogMgr.i("PLAY_MODE_VEDIO");
//            controlVedio(VIDEO_CONTROL_STOP, null);
//        }

        // isRobotMoving = false;
        // mSerialPortActivity.lastRobotMove = lib_H.getActionName();
        if ((lastRobotAction != null && isMoveLeadToInitialStatus(lastRobotAction)) || lastRobotAction == null) {
            isRobotInitial = true;
        } else {
            isRobotInitial = false;
        }
        if (lastRobotAction != null) {
            LogMgr.d("动作" + lastRobotAction + "结束 isRobotInitial = " + isRobotInitial);
        }

        if (isStopMoveDoing == true) {
            isStopMoveDoing = false;
        }

        if (isNeedToReply) {
            // 当前命令是SkillPaly发出的
            isNeedToReply = false;
            setPaused(false);
            LogMgr.d("当前命令是SkillPlay发出的 播放结束 发送返回命令 ");
            // 需要向SkillPlay发送播放结束广播
            sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_COMPLETE, null);
            // mSerialPortActivity.mServerTcp.mAcceptClient.mHandler.obtainMessage(3).sendToTarget();
        }

        isForceStoped = false;

        // 停止后下一步动作
        LogMgr.d("isNextMoveStop = " + isNextMoveStop + " isRobotMoveLooping = " + isRobotMoveLooping);
        if (isNextMoveStop) {
            isStopMoveDoing = true;
            isNextMoveStop = false;
            loopCount = 0;
            // 停止动作
            isRobotMoving = false;
            if (mPlayCallBack != null) {
                mPlayCallBack.onStop();
                mPlayCallBack = null;
            }
            handlePlayCmd(stopRobotMoveCmd, null, false, false, stopRobotMoveDelayTime, ProcedureControl.getInstance()
                    .isProcedureRunning(), PLAY_MODE_NORMAL, false, true, null);
            LogMgr.i("停止动作触发 " + stopRobotMoveCmd);
        } else if (isRobotMoveLooping) {
            // 继续当前动作
            // if(ProcedureControl.getInstance().isProcedureRunning()){
            // ProcedureControl.getInstance().getmProcedure().startNextState();
            // isRobotMoving = false;
            // return;
            // }
            if (mPlayCallBack != null) {
                mPlayCallBack.onSingleMoveStopWhileLoop();
            }
            loopCount++;
            isRobotMoving = false;
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    handlePlayCmd(lastRobotAction, null, true, false, 0, ProcedureControl.getInstance()
//                            .isProcedureRunning(), PLAY_MODE_NORMAL, isUseAutoBalance, true, null);
//                }
//            },3*60*1000);
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (lastRobotMoveType == MOVE_TYPE_ACTION) {
//                        handlePlayCmd(lastRobotAction, null, true, false, 0, ProcedureControl.getInstance()
//                                .isProcedureRunning(), PLAY_MODE_NORMAL, isUseAutoBalance, true, null);
//                    } else if (lastRobotMoveType == MOVE_TYPE_SOUND) {
//                        LogMgr.e("声音不推荐循环");
//                        // handlePlayCmd(null, lastRobotSound, true, false, 0);
//                    } else if (lastRobotMoveType == MOVE_TYPE_BOTH) {
//                        handlePlayCmd(lastRobotAction, lastRobotSound, true, false, 0, ProcedureControl.getInstance()
//                                .isProcedureRunning(), PLAY_MODE_NORMAL, isUseAutoBalance, true, null);
//                    }
//                }
//            },3*60*1000);
            if (lastRobotMoveType == MOVE_TYPE_ACTION) {
                handlePlayCmd(lastRobotAction, null, true, false, 0, ProcedureControl.getInstance()
                        .isProcedureRunning(), PLAY_MODE_NORMAL, isUseAutoBalance, true, null);
            } else if (lastRobotMoveType == MOVE_TYPE_SOUND) {
                LogMgr.e("声音不推荐循环");
                // handlePlayCmd(null, lastRobotSound, true, false, 0);
            } else if (lastRobotMoveType == MOVE_TYPE_BOTH) {
                handlePlayCmd(lastRobotAction, lastRobotSound, true, false, 0, ProcedureControl.getInstance()
                        .isProcedureRunning(), PLAY_MODE_NORMAL, isUseAutoBalance, true, null);
            }
            LogMgr.i("动作循环一次");
        } else if (mPlayMode == PLAY_MODE_LIST) {
            if (mPlayCallBack != null) {
                mPlayCallBack.onStop();
                mPlayCallBack = null;
            }
            mCurrentCountInList++;
            isRobotMoving = false;
            if (mPlayMoveList != null && mCurrentCountInList < mPlayMoveList.length) {
                handlePlayCmd(mPlayMoveList[mCurrentCountInList], null, false, true, 0, false, PLAY_MODE_LIST, false, true, null);
            } else if (mPlayMoveList != null && mCurrentCountInList >= mPlayMoveList.length) {
                sendReturnToSkillplayer(GlobalConfig.S_PROGRAM_PROJECT_OUT_CMD_1,
                        GlobalConfig.S_PROGRAM_PROJECT_OUT_CMD_2_PLAY_LIST_COMPLETE, new byte[]{0x00});
            }
        } else {
            isRobotMoving = false;
            if (mPlayCallBack != null) {
                mPlayCallBack.onNormalStop();
                mPlayCallBack.onStop();
                mPlayCallBack = null;
            }
            if (ProcedureControl.getInstance().isProcedureRunning()) {
                ProcedureControl.getInstance().getmProcedure().startNextState();
            }
        }
    }

    /**
     * 强制停止动作及后续动作
     *
     * @param isNeedToReplyToSkillPlayer 是否需要给skillplayer回复
     */
    public void forceStop(boolean isNeedToReplyToSkillPlayer) {


        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S) {
            sendLightToBrainS5(0, 0);
        }
        // 文件播放结束
        StopTimer();
        LogMgr.i("文件播放强制结束");
        stopRecoverTimer();
        if (mPlayer != null) {
            LogMgr.d("mediaPlayer.stop()");
            mPlayer.stop();
        }
        if((mPlayMode&PLAY_MODE_VEDIO)!=0){
            controlVedio(VIDEO_CONTROL_STOP, null);
        }
        // M速度归零处理
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
            // M机器人设置轮子速度为零
            sendStopSpeedToM();
            recoverMToMoveInitailPosition();
        }
        // M1速度归零处理
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1) {
            // M机器人设置轮子速度为零
            sendStopSpeedToM1();
        }

        //S舵机归零处理
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S) {
            //TODO
            try {
                Thread.sleep(30);
                sendLastFrameToStm32OfS();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LogMgr.e("sendLastFrameToStm32OfS() 异常 e = " + e.getCause().toString());
                e.printStackTrace();
            }
        }

        // 关闭H系列灯光
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            ProtocolUtils.closeH3Led();
        }

        isRobotInitial = true;
        isForceStoped = true;
        setPaused(false);
        if (lastRobotAction != null) {
            LogMgr.d("动作" + lastRobotAction + "结束 isRobotInitial = " + isRobotInitial);
        }

        if (isStopMoveDoing == true) {
            isStopMoveDoing = false;
        }

        if (isNeedToReply) {
            // 当前命令是SkillPlay发出的
            isNeedToReply = false;
            if (isNeedToReplyToSkillPlayer) {
                LogMgr.d("当前命令是SkillPlay发出的 播放结束 发送返回命令 ");
                // 需要向SkillPlay发送播放结束广播
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_COMPLETE, null);
            }
        }
        if (mPlayCallBack != null) {
            mPlayCallBack.onForceStop();
            mPlayCallBack.onStop();
            mPlayCallBack = null;
        }

        // 停止后下一步动作
        LogMgr.d("isNextMoveStop = " + isNextMoveStop + " isRobotMoveLooping = " + isRobotMoveLooping);

        isNextMoveStop = false;
        loopCount = 0;
        isRobotMoving = false;
        ProcedureControl.getInstance().afterStopCurrentProcedure();

    }

    /**
     * 发送当前S bin文件的最后一帧，完成舵机归零
     */
    private void sendLastFrameToStm32OfS() {
        byte[] pPos = new byte[sServoCount * 2];

        mCurrentPosition = Utils.getLastPositionOfArrayInArray(new byte[]{(byte) 0xAA, (byte) 0x55}, mFileBuff);
        if (mCurrentPosition < 0 || mCurrentPosition > mFileBuff.length) {
            return;
        }
        // 从文件中获取动作数据
        if (mCurrentPosition + 4 < mFileLength) {
            while (mCurrentPosition + 4 < mFileLength) {
                if (mFileBuff[mCurrentPosition] == (byte) 0xAA && mFileBuff[mCurrentPosition + 1] == (byte) 0x55) {
                    // 检测头
                    // 帧长度 从机器人型号开始 没有算前四帧
                    int frameLength = (int) ((mFileBuff[mCurrentPosition + 2] & 0xFF) | ((mFileBuff[mCurrentPosition
                            + 3] & 0xFF) << 8));
                    if (mCurrentPosition + frameLength + 4 <= mFileLength) { // 判断总长度
                        byte check = 0x00;
                        for (int n = mCurrentPosition; n < mCurrentPosition + frameLength + 3; n++) { // 从帧头m到数据位结束加和
                            check += mFileBuff[n];
                        }
                        check = (byte) check;

                        if ((byte) check == (byte) mFileBuff[mCurrentPosition + frameLength + 3]) { // 校验正确，提取数据并发送
                            if (mFileBuff[mCurrentPosition + 4] == ControlInitiator.ROBOT_TYPE_S) {
                                LogMgr.v("机器人型号正确 mFileBuff[4] = " + mFileBuff[4]);
                            } else {
                                LogMgr.e("机器人型号错误 mFileBuff[4] = " + mFileBuff[4]);
                            }
                            int frameNum = (int) ((mFileBuff[mCurrentPosition + 5] & 0xFF) | (
                                    (mFileBuff[mCurrentPosition + 6] & 0xFF) << 8));

                            if (isMoveEnable) {
                                // 正常情况下 都应该有动作
                                LogMgr.v("动作位有效");
                                System.arraycopy(mFileBuff, mCurrentPosition + 11, pPos, 0, pPos.length);
                            }
                            LogMgr.e("pPos:" + Utils.bytesToString(pPos));
                            // pPos = testSwitch(pPos);
                            // LogMgr.e("new pPos:" + Utils.bytesToString(pPos,
                            // pPos.length));
                            LogMgr.v("bFirst = " + mIsFirst + " mSerialPortActivity.loopCount = " + loopCount);
//                            if (mIsFirst && loopCount == 0 && mDelayTime > 0) { // 第一次运动特殊处理，将速度降低
//                                LogMgr.i("第一帧动作，减慢速度为100 ， 停顿" + mDelayTime + "毫秒后继续");
//                                mIsFirst = false;
//                                StopTimer();
//
//                                setEngineSpeed(iCount, pID, pPos, 50);
//                                try {
//                                    Thread.sleep(50);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                sendEngineAngles(iCount, pID, pPos);
//                                try {
//                                    Thread.sleep(2000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                setEngineSpeed(iCount, pID, pPos, 0);
//                                StartTimer();
//                            } else if (mIsFirst && loopCount == 0 && mDelayTime == 0) {
//                                LogMgr.i("第一帧动作，不停顿");
//                                mIsFirst = false;
//                                setEngineSpeed(iCount, pID, pPos, 0);
//                            } else {
                            ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
//                            }

                            LogMgr.i("成功解析一帧: 当前帧序号 frameNum = " + frameNum + " mCurrentPosition = " +
                                    mCurrentPosition);

                            mCurrentPosition += (frameLength + 4);

                            return;
                        } else {
                            mCurrentPosition++;
                            LogMgr.e("校验和错误");
                            return;
                        }
                    } else {
                        mCurrentPosition++;
                        LogMgr.e("最后一帧损坏");
                        return;
                    }
                } else {
                    mCurrentPosition++;
                    LogMgr.e("帧头错误");
                    return;
                }
            }
        }
    }

    private long time = System.currentTimeMillis();
    private long lasttime = System.currentTimeMillis();
    private static final int OBSTACLE_AVOIDANCE_STATE_NORMAL = 0;
    private static final int OBSTACLE_AVOIDANCE_STATE_GO_FRONT = 1;
    private static final int OBSTACLE_AVOIDANCE_STATE_GO_BACK = 2;
    private static final int OBSTACLE_AVOIDANCE_STATE_STAY = 3;
    private static final int OBSTACLE_AVOIDANCE_TIME = 1000;
    @IntRange(from = OBSTACLE_AVOIDANCE_STATE_NORMAL,to = OBSTACLE_AVOIDANCE_STATE_STAY)
    private int mMObstacleAvoidanceState = OBSTACLE_AVOIDANCE_STATE_NORMAL;

    /**
     * 对避障状态的处理
     * @param state
     */
    private void mObstacleAvoidanceHandle(int state){
        LogMgr.e("设定当前的避障状态 = "+ state);
        mMObstacleAvoidanceState = state;
        if(state == OBSTACLE_AVOIDANCE_STATE_GO_FRONT || state == OBSTACLE_AVOIDANCE_STATE_GO_BACK || state == OBSTACLE_AVOIDANCE_STATE_STAY){

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LogMgr.e("避障状态结束");
                    mMObstacleAvoidanceState = OBSTACLE_AVOIDANCE_STATE_NORMAL;
                    byte[] stopCmd = ProtocolUtils.getMWheelSpeedCmd((byte)(100&0xFF), (byte)(100&0xFF));

                    for (int i = 0; i < 3; i++) {
                        if (!isRobotMoving) {
                            LogMgr.e("避障状态结束，停止轮子运动");
                            SP.write(stopCmd);
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, OBSTACLE_AVOIDANCE_TIME);
        }else {
            LogMgr.e("参数异常");
        }
    }

//    private class MObstacleAvoidanceRunnable implements Runnable{
//        int state;
//        /**
//         *
//         * @param state
//         */
//        public MObstacleAvoidanceRunnable(int state) {
//            this.state = state;
//        }
//
//        @Override
//        public void run() {
//            if(state == OBSTACLE_AVOIDANCE_STATE_GO_FRONT || state == OBSTACLE_AVOIDANCE_STATE_GO_BACK){
//                LogMgr.e("设定当前的避障状态 = "+ state);
//                mMObstacleAvoidanceState = state;
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        LogMgr.e("避障状态结束");
//                        mMObstacleAvoidanceState = OBSTACLE_AVOIDANCE_STATE_NORMAL;
//                        byte[] stopCmd = ProtocolUtils.getMWheelSpeedCmd((byte)(100&0xFF), (byte)(100&0xFF));
//
//                        for (int i = 0; i < 3; i++) {
//                            if (!isRobotMoving) {
//                                LogMgr.e("避障状态结束，停止轮子运动");
//                                SP.write(stopCmd);
//                            }
//                            try {
//                                Thread.sleep(50);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }, OBSTACLE_AVOIDANCE_TIME);
//            }else {
//                LogMgr.e("参数异常");
//            }
//        }
//    }
    /**
     * M机器人往串口发送命令的线程
     *
     * @author newpartner
     */
    class MFrameRunnable implements Runnable {

        private List<byte[]> tempList;
        private int currentFrameNum;
        private final int obstacleAvoidanceSpeed = 35;
        private final int obstacleAvoidance = 20;//CM


        MFrameRunnable(List<byte[]> downMList, int currentFrameNum) {
            super();
            this.tempList = downMList;
            this.currentFrameNum = currentFrameNum;
        }

        @Override
        public void run() {
            time = System.currentTimeMillis();
            LogMgr.d("MFrameRunnable()开始 当前帧数 = " + currentFrameNum + " tempList.size = " + tempList.size() +"与前一帧的间隔 = "+(time - lasttime));
//            long time = System.currentTimeMillis();
            lasttime = time;
            try {
                for (int i = 0; i < tempList.size(); i++) {
                    //协议以AA 55开头，为眼睛协议或获取传感器协议
                    if(tempList.get(i)!=null && tempList.get(i).length>=12 && tempList.get(i)[0] == GlobalConfig.CMD_0 && tempList.get(i)[1] == GlobalConfig.CMD_1){
                        //此命令为获取传感器协议，当前以及在避障调整状态，不需要下发超声获取协议
                        if(mMObstacleAvoidanceState!=OBSTACLE_AVOIDANCE_STATE_NORMAL &&
                                tempList.get(i)[5] == GlobalConfig.M_SENSOR_OUT_CMD_1 && tempList.get(i)[6] == GlobalConfig.M_SENSOR_OUT_CMD_2){
                            LogMgr.w("当前已经在避障调整状态，不需要下发超声获取协议");
                            continue;
                        }
                        byte[] result = SP.request(tempList.get(i),10);
                        //获取传感器协议回复
                        if(result!=null && result.length>=19 && result[5] == GlobalConfig.M_SENSOR_IN_CMD_1 && result[6] == GlobalConfig.M_SENSOR_M5_IN_CMD_2){
                            LogMgr.w("MFrameRunnable()传感 当前帧数 = " + currentFrameNum + " 前端超声 = "+(int)(result[11]&0xFF) +" 后端超声 = "+ (int)(result[12]&0xFF));
                            int front = (int)(result[11]&0xFF);
                            int back  = (int)(result[12]&0xFF);
                            int collision = (int)(result[13]&0xFF);
                            if(collision > 0){
                                LogMgr.w("前端碰撞，开始后退");
                                mObstacleAvoidanceHandle(OBSTACLE_AVOIDANCE_STATE_GO_BACK);
//                                mMFramethreadPoolExecutor.execute(new MObstacleAvoidanceRunnable(OBSTACLE_AVOIDANCE_STATE_GO_BACK));
                                continue;
                            }
                            if(front < obstacleAvoidance){
                                if(back > obstacleAvoidance){
                                    //后退
                                    LogMgr.w("前端障碍，开始后退");
                                    mObstacleAvoidanceHandle(OBSTACLE_AVOIDANCE_STATE_GO_BACK);
//                                    mMFramethreadPoolExecutor.execute(new MObstacleAvoidanceRunnable(OBSTACLE_AVOIDANCE_STATE_GO_BACK));
                                }else{
                                    //停止动作
                                    LogMgr.w("前后端障碍，停止动作");
                                    mObstacleAvoidanceHandle(OBSTACLE_AVOIDANCE_STATE_STAY);
//                                    mMFramethreadPoolExecutor.execute(new MObstacleAvoidanceRunnable(OBSTACLE_AVOIDANCE_STATE_STAY));
//                                    stopCurrentMove();
                                }
                            }
//                            else if(front < obstacleAvoidance){
//                                if(back < obstacleAvoidance){
//                                    //停止动作
//                                    LogMgr.w("后端障碍，停止动作");
//                                    stopCurrentMove();
//                                }
//                            }
                            else{
                                if(back < obstacleAvoidance){
                                    //前进
                                    LogMgr.w("后端障碍，开始前进");
                                    mObstacleAvoidanceHandle(OBSTACLE_AVOIDANCE_STATE_GO_FRONT);
//                                    mMFramethreadPoolExecutor.execute(new MObstacleAvoidanceRunnable(OBSTACLE_AVOIDANCE_STATE_GO_FRONT));
                                }
                            }
                        }
                    }else{
                        if(mMObstacleAvoidanceState!=OBSTACLE_AVOIDANCE_STATE_NORMAL && ProtocolUtils.isMWheelSpeedCmd(tempList.get(i))){
                            if(mMObstacleAvoidanceState == OBSTACLE_AVOIDANCE_STATE_GO_FRONT){
                                LogMgr.e("当前为后方避障状态，控制机器人前进");
                                byte[] tempCmd = ProtocolUtils.getMWheelSpeedCmd( (byte)((100 + obstacleAvoidanceSpeed)&0xFF) , (byte)((100 + obstacleAvoidanceSpeed)&0xFF));
                                SP.write(tempCmd);
                            }else if(mMObstacleAvoidanceState == OBSTACLE_AVOIDANCE_STATE_GO_BACK){
                                LogMgr.e("当前为前方避障状态，控制机器人后退");
                                byte[] tempCmd = ProtocolUtils.getMWheelSpeedCmd( (byte)((100 - obstacleAvoidanceSpeed)&0xFF) , (byte)((100 - obstacleAvoidanceSpeed)&0xFF));
                                SP.write(tempCmd);
                            }else if(mMObstacleAvoidanceState == OBSTACLE_AVOIDANCE_STATE_STAY){
                                LogMgr.e("当前为前后方避障状态，控制机器人静止");
                                byte[] tempCmd = ProtocolUtils.getMWheelSpeedCmd( (byte)((100)&0xFF) , (byte)((100)&0xFF));
                                SP.write(tempCmd);
                            }else{
                                LogMgr.e("状态异常");
                            }
                        }else{
                            SP.write(tempList.get(i));
                        }
                    }
                    LogMgr.d("MFrameRunnable()一帧 当前帧数 = " + currentFrameNum + " 下发第" + i + "条命令结束");
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("M机器人下发控制命令出错");
            }
            LogMgr.d("MFrameRunnable()结束 当前帧数 = " + currentFrameNum + " 消耗时间 = " + (System.currentTimeMillis() - time) +"毫秒");
        }
    }





    /**
     * 暂停当前的播放的动作
     *
     * @return
     */
    public synchronized boolean pauseCurrentMove() {
        LogMgr.i("pauseCurrentMove()");
        if (isRobotMoving() == true && isPaused() == false) {
            // 当前机器人在动作中
            // 需要向SkillPlay发送暂停回应
            if (isNeedToReply) {
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_PAUSE, null);
            }
            if (mPlayCallBack != null) {
                mPlayCallBack.onPause();
            }

            // 暂停处理
            if (lastRobotMoveType == MOVE_TYPE_ACTION || lastRobotMoveType == MOVE_TYPE_BOTH) {
                StopTimer();
            }

            if((mPlayMode&PLAY_MODE_VEDIO)!=0){
                controlVedio(VIDEO_CONTROL_PAUSE, null);
            }else{
                if (lastRobotMoveType == MOVE_TYPE_SOUND || lastRobotMoveType == MOVE_TYPE_BOTH) {
                    mPlayer.pause();
                }
            }
            // M速度归零处理
            if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
                // M机器人设置轮子速度为零
                sendStopSpeedToM();
            }
            setPaused(true);
            LogMgr.i("暂停当前动作命令  成功");
            return true;
        } else {
            LogMgr.w("暂停当前动作命令  失败");
            return false;
        }
    }

    /**
     * 继续当前的播放的动作
     *
     * @return
     */
    public synchronized boolean resumeCurrentMove() {
        LogMgr.i("resumeCurrentMove()");
        if (isRobotMoving() == true && isPaused() == true) {
            // 当前机器人在动作暂停中
            // 需要向SkillPlay发送续播回应
            if (isNeedToReply) {
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_RESUME, null);
            }
            if (mPlayCallBack != null) {
                mPlayCallBack.onResume();
            }
            if (lastRobotMoveType == MOVE_TYPE_ACTION || lastRobotMoveType == MOVE_TYPE_BOTH) {
                StartTimer();
            }

            if((mPlayMode&PLAY_MODE_VEDIO)!=0){
                controlVedio(VIDEO_CONTROL_RESUME, null);
            }else{
                if (lastRobotMoveType == MOVE_TYPE_SOUND || lastRobotMoveType == MOVE_TYPE_BOTH) {
                    mPlayer.resume();
                }
            }
            setPaused(false);
            LogMgr.i("续播当前动作命令  成功");
            return true;
        } else {
            LogMgr.w("续播当前动作命令  失败");
            return false;
        }
    }

    /**
     * 停止当前的播放的动作
     *
     * @return
     */
    public synchronized boolean stopCurrentMove() {
        LogMgr.i("stopCurrentMove()");
        if (isRobotMoving() == true) {
            LogMgr.i("stopCurrentSkillPlayMove() isRobotMoving() == true");
            if (isNeedToReply) {
                LogMgr.d("isNeedToReply == true");
                sendReturnToSkillplayer(GlobalConfig.PLAY_OUT_CMD_1, GlobalConfig.PLAY_OUT_CMD_2_STOP, null);
            }
            if (mPlayCallBack != null) {
                mPlayCallBack.onForceStop();
                mPlayCallBack.onStop();
                mPlayCallBack = null;
            }
            forceStop(false);
            return true;
        } else {
            LogMgr.i("stopCurrentSkillPlayMove() isRobotMoving() == false");
            return false;
        }
    }

    /**
     * 恢复至机器人的初始位置
     * @param actionFileName
     * @param soundFileName
     * @param isLoop
     * @param isSkillPlayCmd
     * @param delayTime
     * @param isProcedureMove
     * @param isUseAutoBalance
     * @param isMObstacleAvoidanceEnable
     */
    public synchronized void recoverToMoveInitailPosition(final boolean hasNextMove, final String actionFileName,
                                                          final String soundFileName, final boolean isLoop, final
                                                          boolean isSkillPlayCmd, final int delayTime,
                                                          final boolean isProcedureMove, final int playMode, final
                                                          PlayCallBack playCallBack, final boolean isUseAutoBalance, final boolean isMObstacleAvoidanceEnable) {
        LogMgr.i("recoverToMoveInitailPosition() 机器人恢复初态");
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
            LogMgr.v("H机器人恢复初态");
            ProtocolUtils.setEngineSpeed(iCount, pID, GlobalConfig.H_MOVE_START_POSITION, mHRecoverSpeed);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ProtocolUtils.sendEngineAngles(iCount, pID, GlobalConfig.H_MOVE_START_POSITION);
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
            LogMgr.v("M机器人恢复初态");
            // M停止处理
            recoverMToMoveInitailPosition();
        }
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
            startRecoverTimer(hasNextMove, actionFileName, soundFileName, isLoop, isSkillPlayCmd, delayTime,
                    isProcedureMove, playMode, playCallBack, isUseAutoBalance);
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isRecovering = false;
                    if (hasNextMove) {
                        LogMgr.i("M恢复至初态，开始执行下一个动作");
                        handlePlayCmd(actionFileName, soundFileName, isLoop, isSkillPlayCmd, delayTime,
                                isProcedureMove, playMode, isUseAutoBalance, isMObstacleAvoidanceEnable, playCallBack);
                    }
                }
            }, 100);
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C) {
            handlePlayCmd(actionFileName, soundFileName, isLoop, isSkillPlayCmd, delayTime, isProcedureMove,
                    playMode, isUseAutoBalance, isMObstacleAvoidanceEnable, playCallBack);
        }
    }

    /**
     * M停止操作：轮子停止，头部恢复，颜色关闭
     */
    private void recoverMToMoveInitailPosition() {
        LogMgr.i("recoverMToMoveInitailPosition()");
        if (mDownMCmd != null) {
            mDownMCmd.clear();
        } else {
            mDownMCmd = new ArrayList<byte[]>();
        }
        // 头部左右命令
        headHorizenCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x00, (byte) (130 & 0xFF), (byte) 0x00);
        mDownMCmd.add(headHorizenCmd);

        // 脖子灯光命令
        neckColorCmd = ProtocolUtils.getMNeckColorCmd((byte) 0x00, (byte) 0x00, (byte) 0x00);
        mDownMCmd.add(neckColorCmd);

        // 底部灯光命令
        bottomColorCmd = ProtocolUtils.getMBottomColorCmd((byte) 0x00, (byte) 0x00, (byte) 0x00);
        mDownMCmd.add(bottomColorCmd);

        // 头部上下命令
        headVerticalCmd = ProtocolUtils.getMNeckMoveCmd((byte) 0x01, (byte) (15 & 0xFF));
        mDownMCmd.add(headVerticalCmd);

        // 轮子速度命令
        wheelSpeedCmd = ProtocolUtils.getMWheelSpeedCmd((byte) 100, (byte) 100);
        mDownMCmd.add(wheelSpeedCmd);

        // 眼部灯光命令
        mEyeLightData = new byte[48];
        eyesColorCmd = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_M, GlobalConfig.M_EYE_LIHGT_CMD_1,
                GlobalConfig.M_EYE_LIHGT_CMD_2, mEyeLightData);
        mDownMCmd.add(eyesColorCmd);

//        new MFrameRunnable(mDownMCmd, -1).start();
//        new Thread(new MFrameRunnable(mDownMCmd, -1)).start();
        mMFramethreadPoolExecutor.execute(new MFrameRunnable(mDownMCmd, -1));
    }

    /**
     * 开启监听H机器人是否恢复到初始位置的timer
     *  @param hasNextMove     恢复后是否立即执行下一个动作
     * @param actionFileName
     * @param soundFileName
     * @param isLoop
     * @param isSkillPlayCmd
     * @param delayTime
     * @param isProcedureMove
     * @param playCallBack
     * @param isUseAutoBalance
     */
    private void startRecoverTimer(final boolean hasNextMove, final String actionFileName, final String soundFileName,
                                   final boolean isLoop, final boolean isSkillPlayCmd, final int delayTime, final
                                   boolean isProcedureMove,
                                   final int playMode, final PlayCallBack playCallBack, final boolean isUseAutoBalance) {
        LogMgr.i("startRecoverTimer()");
        stopRecoverTimer();
        mCountOfReadEngineAngle = 0;
        mRecoverTimer = new Timer();
        mRecoverTimerTask = new TimerTask() {
            @Override
            public void run() {
                byte[] tempCmd = getHCurrent22EngineAngleCMD;
                byte[] buffer = new byte[56];
                try {
                    mCountOfReadEngineAngle++;
                    if (mCountOfReadEngineAngle > mMaxCountOfReadEngineAngle) {
                        LogMgr.w("恢复时间超过5秒，认为已恢复");
                        stopRecoverTimer();
                        Thread.sleep(100);
                        ProtocolUtils.setEngineSpeed(iCount, pID, null, 0);
                        isRecovering = false;
                        if (hasNextMove) {
                            LogMgr.i("H恢复至初态，开始执行下一个动作");
                            handlePlayCmd(actionFileName, soundFileName, isLoop, isSkillPlayCmd, delayTime,
                                    isProcedureMove, playMode, isUseAutoBalance, true, playCallBack);
                        }
                        return;
                    }
                    SP.write(tempCmd);
                    LogMgr.v("所有舵机角度的命令 获取到的数据1 = " + Arrays.toString(buffer));
                    byte[] dataOf22Engine = new byte[44];
                    System.arraycopy(buffer, 11, dataOf22Engine, 0, dataOf22Engine.length);
                    for (int d = 0; d < dataOf22Engine.length; d = d + 2) {
                        int k = d / 2 + 1;
                        int angle = (int) (((dataOf22Engine[0 + d] & 0xFF) << 8) | (dataOf22Engine[1 + d] & 0xFF));
                        int expectAngle = (int) ((GlobalConfig.H_MOVE_START_POSITION[d + 3] & 0xFF) << 8 |
                                (GlobalConfig.H_MOVE_START_POSITION[d + 2] & 0xFF));
                        LogMgr.d("舵机号 = " + k + " 角度 = " + angle + " 期望角度 = " + expectAngle);
                        if (Math.abs(angle - expectAngle) > 40) {
                            LogMgr.d("角度与期望角度不符");
                            return;
                        }
                    }
                    LogMgr.i("所有舵机角度都符合期望");
                    stopRecoverTimer();
                    Thread.sleep(100);
                    ProtocolUtils.setEngineSpeed(iCount, pID, null, 0);
                    isRecovering = false;
                    if (hasNextMove) {
                        LogMgr.i("H恢复至初态，开始执行下一个动作");
                        handlePlayCmd(actionFileName, soundFileName, isLoop, isSkillPlayCmd, delayTime,
                                isProcedureMove, playMode, isUseAutoBalance, true, playCallBack);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        mRecoverTimer.schedule(mRecoverTimerTask, 100, 100);
    }

    /**
     * 停止监听H机器人是否恢复到初始位置的timer
     */
    private void stopRecoverTimer() {
        if (mRecoverTimer != null) {
            mRecoverTimer.cancel();
        }
        if (mRecoverTimerTask != null) {
            mRecoverTimerTask.cancel();
        }
    }

    /**
     * 发送信息回skillplayer
     *
     * @param cmd1
     * @param cmd2
     * @param returnData 不需要时取null
     */
    private void sendReturnToSkillplayer(byte cmd1, byte cmd2, byte[] returnData) {
        if (mHandlerForReturn != null) {
            byte[] skillPlayerCmd = ProtocolUtils.buildProtocol(mRobotType, cmd1, cmd2, returnData);
            Message message = mHandlerForReturn.obtainMessage();
            message.obj = skillPlayerCmd;
            mHandlerForReturn.sendMessage(message);
        } else {
            LogMgr.e("mHandlerForReturn is null");
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    /**
     * M速度归零处理
     */
    private void sendStopSpeedToM() {
        LogMgr.w("sendStopSpeedToM()  M速度归零处理  执行暂停或停止命令");
        try {
            Thread.sleep(220);

            byte[] tempWHeelSpeedCmdNew = ProtocolUtils.getMWheelSpeedCmdNew((byte) 100, (byte) 100);

            for (int i = 0; i < 3; i++) {
                byte[] buffer = SP.request(tempWHeelSpeedCmdNew);
                // byte[] buffer = new byte[13];
                // SerialPortCommunicator.getInstance().write(buffer);
                LogMgr.d("sendStopSpeedToM() buffer = " + Arrays.toString(buffer));
                boolean isMFeedbackCorrect = ProtocolUtils.isFeedbackCorrect(buffer);
                if (isMFeedbackCorrect) {
                    LogMgr.i("M新协议速度归零处理  成功");
                    return;
                } else {
                    LogMgr.e("M新协议速度归零处理 返回错误");
                }
                // if(buffer[0] == GlobalConfig.CMD_0 && buffer[1] ==
                // GlobalConfig.CMD_1 &&
                // buffer[5] == GlobalConfig.M_FEEDBACK_IN_CMD_1 && buffer[6] ==
                // GlobalConfig.M_FEEDBACK_IN_CMD_2){
                // if(buffer[11] == GlobalConfig.M_FEEDBACK_OK){
                // LogMgr.d("M新协议速度归零处理成功");
                // return;
                // }else if(buffer[11] == GlobalConfig.M_FEEDBACK_WRONG_HEAD){
                // LogMgr.e("M新协议速度归零处理 数据头出错(非AA 55)");
                // }else if(buffer[11] == GlobalConfig.M_FEEDBACK_WRONG_CHECK){
                // LogMgr.e("M新协议速度归零处理 校验值出错");
                // }else{
                // LogMgr.e("M新协议速度归零处理 返回错误1");
                // }
                // }else{
                // LogMgr.e("M新协议速度归零处理 返回错误2");
                // }
            }

            byte[] tempWheelSpeedCmd = ProtocolUtils.getMWheelSpeedCmd((byte) 100, (byte) 100);

            for (int i = 0; i < 5; i++) {
                SP.write(tempWheelSpeedCmd);
            }

        } catch (Exception e) {
            LogMgr.e("pauseCurrentSkillPlayerMove() 暂停时M设置速度为0时异常");
            e.printStackTrace();
        }
    }

    /**
     * @Description M1初始化
     * @author lz
     * @time 2017-4-21 下午5:49:37
     */
    private void sendStopSpeedToM1() {
        LogMgr.w("sendStopSpeedToM()  M速度归零处理  执行暂停或停止命令");
        try {
            Thread.sleep(220);
            // M1初始化
            byte[] tempWHeelSpeedCmdNew = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xa6, (byte) 0x40,
                    new byte[0]);

            for (int i = 0; i < 3; i++) {
                SP.write(tempWHeelSpeedCmdNew);
            }
            // M1脖子点击归位
            byte[] tempWheelSpeedCmd = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xa6, (byte) 0x42, new byte[0]);

            for (int i = 0; i < 5; i++) {
                SP.write(tempWheelSpeedCmd);
            }

        } catch (Exception e) {
            LogMgr.e("pauseCurrentSkillPlayerMove() 暂停时M1设置速度为0时异常");
            e.printStackTrace();
        }
    }
    private double BALANCE_HIP_PITCH_GAIN = 0.1;
    private double BALANCE_HIP_ROLL_GAIN = 0.3;
    private double BALANCE_KNEE_GAIN = 0.3;
    private double BALANCE_ANKLE_PITCH_GAIN = 0.3;
    private double BALANCE_ANKLE_ROLL_GAIN = 0.3;

//    private int left_act_pose = 512;
//    private int right_act_pose = 512;
//
//    private int count = 0;
//
//    PID_S1 pidCal= new PID_S1();

    /**
     * Auto balance
     *
     * @param iCount
     * @param pID
     * @param pPos
     * @param rlgyro
     * @param fbgyro
     */
    public void auto_balance(byte iCount, byte[] pID, byte[] pPos, double rlgyro, double fbgyro, double roll, double pitch, double yaw) {
        int iLength = 0;
        byte bChecksum = 0;
        int i = 0;
        int pos;
        int lowByte;
        int hiByte;
//        Log.e("auto_balance 111111", "id=" + pID[i] + ",i=" + i + ",iCount=" + iCount + ",rlgyro=" + rlgyro + ",fbgyro=" + fbgyro);
        if (iCount < 1 || iCount > 30) {
            return;
        }
        SensorImuService.startSensorImuService();
        for (i = 0; i < iCount; i++) {
            Log.e("auto_balance", "id=" + pID[i] + ",i=" + i + ",iCount=" + iCount + ",rlgyro=" + rlgyro +
                    ",fbgyro=" + fbgyro);
            if (pID[i] < 7 || pID[i] > 16)
                continue;
            hiByte = 0x000000FF & (int) pPos[i * 2 + 1];
            lowByte = 0x000000FF & (int) pPos[i * 2];
            pos = (int) ((hiByte << 8 | lowByte) & 0xFFFFFFFF);
            //pos = 512;

//             Log.e("auto_balance", "id=" + pID[i] + ",pos=" + pos + ",rlgyro=" + rlgyro + ",fbgyro=" + fbgyro + ",pitch=" + pitch + ",hi=" + hiByte + ",low=" + lowByte);
            switch (pID[i]) {
                case 9:// R_HIP_PITCH
//                    double increaseRight = pidCal.PID_realize(pitch, 0, 1);
//                    pos -= increaseRight;
//                    right_act_pose = (right_act_pose * 95 + pos * 5) / 100;
//                    pos = right_act_pose;
                    pos += (int) (fbgyro * BALANCE_HIP_PITCH_GAIN);
                    break;
                case 10:// L_HIP_PITCH
//                    double increaseLeft = pidCal.PID_realize(pitch, 0, 0);
//                    pos += increaseLeft;
//                    left_act_pose = (left_act_pose * 95 + pos * 5) / 100;
//                    pos = left_act_pose;
                    pos -= (int) (fbgyro * BALANCE_HIP_PITCH_GAIN);
                    break;
                case 7:// R_HIP_ROLL
                    pos -= (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);
                    break;
                case 8:// L_HIP_ROLL
                    pos -= (int) (rlgyro * BALANCE_HIP_ROLL_GAIN);
                    break;
                case 11:// R_KNEE
                    pos += (int) (fbgyro * BALANCE_KNEE_GAIN);
                    break;
                case 12:// L_KNEE
                    pos -= (int) (fbgyro * BALANCE_KNEE_GAIN);
                    break;
                case 13:// R_ANKLE_PITCH
                    pos += (int) (fbgyro * BALANCE_ANKLE_PITCH_GAIN);
                    break;
                case 14:// L_ANKLE_PITCH
                    pos -= (int) (fbgyro * BALANCE_ANKLE_PITCH_GAIN);
                    break;
                case 15:// R_ANKLE_ROLL
                    pos -= (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);
                    break;
                case 16://L_ANKLE_ROLL
                    pos -= (int) (rlgyro * BALANCE_ANKLE_ROLL_GAIN);
                    break;
            }

            pPos[i * 2 + 1] = (byte) ((pos >> 8) & 0xff);
            pPos[i * 2] = (byte) (pos & 0xff);
        }
    }

    public interface PlayCallBack {
        void onStart();

        void onPause();

        void onResume();

        void onStop();

        /**
         * 在循环中的单次动作结束
         */
        void onSingleMoveStopWhileLoop();

        /**
         * 动作正常播放完后停止，而不是强制停止。
         */
        void onNormalStop();

        void onForceStop();
    }
}
