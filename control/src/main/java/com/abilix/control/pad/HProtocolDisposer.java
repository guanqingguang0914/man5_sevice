package com.abilix.control.pad;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.sp.PushMsg;
import com.abilix.control.utils.SharedPreferenceTools;
import com.abilix.moco.gaitx.kernal.execute.GaitAlgorithmForH5;
import com.abilix.moco.gaitx.kernal.execute.SensorImuServiceForH5;
import com.abilix.robot.walktunner.GaitAlgorithm;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.SensorImuService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.abilix.control.ControlApplication.instance;

public class HProtocolDisposer extends AbstractProtocolDisposer {

    private final static String TAG = "HProtocolDisposer";

    private final int CMD_TYPE_ERROR = 0;
    private final int CMD_TYPE_NEED_ONLY_RETURN = 1;
    private final int CMD_TYPE_NEED_SERIAL_0 = 2;
    private final int CMD_TYPE_NEED_RETURN_AND_SERIAL_0 = 3;
    private final int CMD_TYPE_NEED_SERIAL_1 = 4;
    private final int CMD_TYPE_NEED_SERIAL_1_LIST = 5;
    private final int CMD_TYPE_NEED_SERIAL_0_AND_SERIAL_1_LIST = 6;
    private final int CMD_TYPE_NEED_RETURN_AND_SERIAL_1 = 7;
    //	private long lastTime = 0 ;
    private byte[] waveByteArray;
    private int mCmdType;
    /**踏步动作：0关闭，1打开*/
    public int isNeedMarkTimeAfterMove; // 踏步检测：0关闭检测，1打开检测
    /**当前是否在步态状态中*/
    public static boolean isInStepState = false;

    private static int times;

    public HProtocolDisposer(Handler mHandler) {
        super(mHandler);
        waveByteArray = new byte[20];
        waveByteArray[0] = (byte) 0xAA;
        waveByteArray[1] = (byte) 0x54;
        for (int i = 2; i < 20; i++) {
            waveByteArray[i] = (byte) 0x00;
        }

    }

    @Override
    public void DisposeProtocol(Message msg) {
        LogMgr.d(TAG, "H_DisposeProtocol()");
        try {
            final byte[] data = (byte[]) msg.obj;
            LogMgr.d("pad cmd::" + Utils.bytesToString(data));
//            int i = data[0] & 0xff;
//            int j = data[1] & 0xff;

            if ((data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                    .ENGINE_GET_ULTRASONIC_OUT_CMD_2) //获取超声
                    || (data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                    .ENGINE_GET_GYRO_OUT_CMD_2) //获取陀螺仪；新的
                    || (data[0] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1_OLD && data[1] == GlobalConfig
                    .ENGINE_GET_GYRO_OUT_CMD_2_OLD) //获取陀螺仪；新旧的
                    || (data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                    .HEAD_TOUCH_FIRMWARE_OUT_CMD_2) //获取头部触控
                    || (data[5] == GlobalConfig.GET_SN_CMD_1 && data[6] == GlobalConfig.GET_SN_CMD_2) //获取SN
                    || (data[5] == GlobalConfig.SET_CHILDREN_TYPE_CMD_1 && data[6] == GlobalConfig
                    .SET_CHILDREN_TYPE_CMD_2)//设置类型
                    || (data[5] == GlobalConfig.SET_CHILDREN_TYPE_CMD_1 && data[6] == GlobalConfig
                    .GET_CHILDREN_TYPE_CMD_2)//获取类型

                    ) {
                //需要返回上层应用端 50毫秒超时
                byte[] returnData = SP.request(data, 50);
                Message msg_return = mHandler.obtainMessage();
                msg_return.obj = returnData;
                LogMgr.v("返回应用端的数据::" + Arrays.toString(returnData));
//                LogMgr.v("返回应用端的数据::" + ((returnData!=null&&returnData.length>=12)?String.valueOf((int)
// returnData[12]):"NULL"));
                mHandler.sendMessage(msg_return);
            } else if ((data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                    .ENGINE_ANGLE_FIRMWARE_OUT_CMD_2) //获取舵机角度
                    || (data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                    .MACHINE_CALIBRATION_CMD_2) //获取整机标定反馈
                    ) {
                //需要返回上层应用端 200毫秒超时
                byte[] returnData = SP.request(data, 200);
                Message msg_return = mHandler.obtainMessage();
                msg_return.obj = returnData;
                LogMgr.v(TAG, "返回应用端的数据::" + Arrays.toString(returnData));
                mHandler.sendMessage(msg_return);
            } else if (data[5] == (byte) 0x20 && data[6] == (byte) 0x03) {//从android获取陀螺仪数值
                //陀螺仪，指南针
                Message sensorMsg = mHandler.obtainMessage();
                if (mSensor.getmO() != null && mSensor.getmG() != null && mSensor.getmS() != null) {
                    byte[] datas = Utils.byteMerger(Utils.byteMerger(
                            Utils.floatsToByte(mSensor.getmO()),
                            Utils.floatsToByte(mSensor.getmG())), Utils
                            .floatsToByte(mSensor.getmS()));
                    byte[] responseBytes = ProtocolBuilder.buildProtocol(
                            (byte) ControlInitiator.ROBOT_TYPE_S,
                            ProtocolBuilder.CMD_GYRO, datas);
                    sensorMsg.obj = responseBytes;
                    mHandler.sendMessage(sensorMsg);
                    LogMgr.d("返回陀螺仪数值" + Utils.bytesToString(responseBytes));
                }
            } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_PAUSE) {
                //skillplayer暂停命令
                PlayMoveOrSoundUtils.getInstance().pauseCurrentMove();
                return;
            } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_RESUME) {
                //skillplayer续播命令
                PlayMoveOrSoundUtils.getInstance().resumeCurrentMove();
                return;
            } else if (data[5] == (byte) 0x06 && data[6] == (byte) 0x02) {
                LogMgr.d("释放H舵机命令");
//                ProtocolUtils.engineStateChangeAll(ProtocolUtils.ENGINE_STATE_RELEASE);
            } else if (ProtocolUtils.isMultiMediaCmd(data)) {
                if (ProtocolUtils.isMultiMediaAudioPlayCmd(data)) {
                    LogMgr.i("收到多媒体音频播放命令");
                    byte[] temp = new byte[data.length - 12 - 6];
                    System.arraycopy(data, 17, temp, 0, temp.length);
                    String filePath = new String(temp, "UTF-8");
                    String totalPath = Environment.getExternalStorageDirectory().getPath() + File.separator +
                            "Abilix" + File.separator + "media" + filePath;
                    File file = new File(totalPath);
                    if (file.exists() && file.isFile()) {
                        LogMgr.i("文件存在 file = " + totalPath);
                        mPlayer.setOnCompletionListener(new OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,
                                        GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                                        GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY_COMPLETE, null);
                                Message message = mHandler.obtainMessage();
                                message.obj = returnData;
                                mHandler.sendMessage(message);
                            }
                        });
                        mPlayer.playSoundFile(totalPath);
                        byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, GlobalConfig
                                        .MULTI_MEDIA_OUT_CMD_1,
                                GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY, new byte[]{(byte) 0x00});
                        Message message = mHandler.obtainMessage();
                        message.obj = returnData;
                        mHandler.sendMessage(message);
                    } else {
                        LogMgr.w("文件不存在 file = " + totalPath);
                        byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, GlobalConfig
                                        .MULTI_MEDIA_OUT_CMD_1,
                                GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY, new byte[]{(byte) 0x01});
//						returnToClient(returnData);
                        Message message = mHandler.obtainMessage();
                        message.obj = returnData;
                        mHandler.sendMessage(message);
                    }
                } else if (ProtocolUtils.isMultiMediaPauseCmd(data)) {
                    LogMgr.i("收到多媒体暂停命令");
                    mPlayer.pause();
                    byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, GlobalConfig
                                    .MULTI_MEDIA_OUT_CMD_1,
                            GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PAUSE, null);
                    Message message = mHandler.obtainMessage();
                    message.obj = returnData;
                    mHandler.sendMessage(message);
                } else if (ProtocolUtils.isMultiMediaResumeCmd(data)) {
                    LogMgr.i("收到多媒体续播命令");
                    mPlayer.resume();
                    byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, GlobalConfig
                                    .MULTI_MEDIA_OUT_CMD_1,
                            GlobalConfig.MULTI_MEDIA_OUT_CMD_2_RESUME, null);
                    Message message = mHandler.obtainMessage();
                    message.obj = returnData;
                    mHandler.sendMessage(message);
                } else if (ProtocolUtils.isMultiMediaStopCmd(data)) {
                    LogMgr.i("收到多媒体停止命令");
                    mPlayer.stop();
                    byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, GlobalConfig
                                    .MULTI_MEDIA_OUT_CMD_1,
                            GlobalConfig.MULTI_MEDIA_OUT_CMD_2_STOP, null);
                    Message message = mHandler.obtainMessage();
                    message.obj = returnData;
                    mHandler.sendMessage(message);
                } else {
                    LogMgr.e("收到无效的多媒体命令");
                }
                return;
            } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_PLAY) {
                // 播放文件命令
                LogMgr.d(TAG, "播放文件命令");
                LogMgr.d(TAG, Utils.bytesToString(data));
                int dataLength = (int) ((data[3] & 0xFF) | ((data[2] & 0xFF) << 8));
                byte[] tempbuff = new byte[dataLength - 13];
                System.arraycopy(data, 16, tempbuff, 0, tempbuff.length); // 文件名
                String fileNameTemp = new String(tempbuff, "UTF-8");
                //文件路径
                String actionFilePath = null;
                String soundFilePath = null;
                String folderPath = null;
                String actionFileName = null;
                String soundFileName = null;
                if (TextUtils.isEmpty(fileNameTemp)) {
                    Log.e(TAG, "文件名为空");
                    return;
                }
                //上级App
                final byte upperAppType = data[11];
                //播放类型
                byte playType = data[12];
                //循环控制
                boolean loopCtr = false;
                if (data[13] == (byte) 0x00) {
                    loopCtr = false;
                } else if (data[13] == (byte) 0x01) {
                    loopCtr = true;
                } else {
                    LogMgr.e(TAG, "循环控制位错误");
                    loopCtr = false;
                }
                //第一帧后延迟时间
                int delayTime = (int) ((data[14] & 0xFF) << 8 | data[15] & 0xFF);
                boolean isNeedToReply = false;
                //根据上级app 设置文件路径
                if (upperAppType == GlobalConfig.APP_TYPE_SKILL_PLAYER) {
                    isNeedToReply = true;
                    PlayMoveOrSoundUtils.getInstance().setmHandlerForReturn(mHandler);
                    String[] folderAndFiles = fileNameTemp.split("\\\\");
                    if (folderAndFiles.length == 2) {
                        folderPath = folderAndFiles[0];
                        String fileNames = folderAndFiles[1];
                        if (fileNames.contains("&")) {
                            actionFileName = fileNames.split("&")[0];
                            soundFileName = fileNames.split("&")[1];
                            actionFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File
                                    .separator + actionFileName;
                            soundFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File
                                    .separator + soundFileName;
                        } else if (fileNames.endsWith(".bin")) {
                            actionFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File
                                    .separator + fileNames;
                            soundFilePath = null;
                        } else if (fileNames.endsWith(".wav") || fileNames.endsWith(".avi")) {
                            actionFilePath = null;
                            soundFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File
                                    .separator + fileNames;
                        }
                    }

                } else if (upperAppType == GlobalConfig.APP_TYPE_PROGRAM_ROBOT) {
                    //项目编程
                    LogMgr.d(TAG, "H项目编程 fileNameTemp = " + fileNameTemp + " playType = " + playType);
                    isNeedToReply = false;
                    if (fileNameTemp.endsWith(".bin")) {
                        if (playType == GlobalConfig.PLAY_TYPE_ACTION) {
                            //只播放动作
                            actionFilePath = GlobalConfig.PROGRAM_PATH + File.separator + fileNameTemp;
                            soundFilePath = null;
                        } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_SOUND) {
                            //播放动作和音频
                            actionFilePath = GlobalConfig.PROGRAM_PATH + File.separator + fileNameTemp;
                            soundFilePath = actionFilePath.replace(".bin", ".wav");
                        }
                    }
                } else if (upperAppType == GlobalConfig.APP_TYPE_KNOW_ROBOT) {
                    //认识机器人
                    LogMgr.d(TAG, "H认识机器人播放动作 fileNameTemp = " + fileNameTemp);
                    isNeedToReply = true;
                    PlayMoveOrSoundUtils.getInstance().setmHandlerForReturn(mHandler);
                    if (fileNameTemp.endsWith(".bin")) {
                        if (playType == GlobalConfig.PLAY_TYPE_ACTION) {
                            //只播放动作
                            actionFilePath = GlobalConfig.KNOW_ROBOT_PATH + File.separator + fileNameTemp;
                            soundFilePath = null;
                        } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_SOUND) {
                            //播放动作和音频
                            actionFilePath = GlobalConfig.KNOW_ROBOT_PATH + File.separator + fileNameTemp;
                            soundFilePath = actionFilePath.replace(".bin", ".wav");
                        }
                    }
//                    if (fileNameTemp.endsWith(".mp3") || fileNameTemp.endsWith(".wav")) {
//                        LogMgr.d(TAG, "paly short music");
//                        mPlayer.play(fileNameTemp);
//                    } else {
//                        mPlayer.stop();
//                    }
                } else {
                    isNeedToReply = false;
                    if (fileNameTemp.endsWith(".bin")) {
                        actionFilePath = GlobalConfig.DOWNLOAD_PATH + File.separator + fileNameTemp;
                        soundFilePath = actionFilePath.replace(".bin", ".wav");
                    } else if (fileNameTemp.endsWith(".wav") || fileNameTemp.endsWith(".avi")) {
                        soundFilePath = GlobalConfig.DOWNLOAD_PATH + File.separator + fileNameTemp;
                    }

                }
                LogMgr.d(TAG, "actionfilePath = " + actionFilePath + " soundfilePath = " + soundFilePath);

                LogMgr.d(TAG, "延迟时间 delayTime = " + delayTime);
                //根据播放类型的不同作不同的处理
                if (playType == GlobalConfig.PLAY_TYPE_ACTION) {
                    //只播放动作
                    if (ControlApplication.RobotFallCheck && ControlApplication.IsRobotFall && upperAppType ==
                            GlobalConfig.APP_TYPE_KNOW_ROBOT
                            && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) { // 机器人摔倒了，不执行bin文件
                        LogMgr.e(TAG, "机器人摔倒了，不执行bin文件");
                        return;
                    }
                    LogMgr.v(TAG, "H_DisposeProtocol 只播放动作 actionfilePath = " + actionFilePath);
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, null, loopCtr, isNeedToReply,
                            delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, new PlayMoveOrSoundUtils
                                    .PlayCallBack() {


                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onPause() {

                                }

                                @Override
                                public void onResume() {

                                }

                                @Override
                                public void onStop() {
//                            PlayMoveOrSoundUtils.getInstance().stopCurrentMove();

                                }

                                @Override
                                public void onSingleMoveStopWhileLoop() {

                                }

                                @Override
                                public void onNormalStop() {
                                    resumeMove();
                                }

                                @Override
                                public void onForceStop() {

                                }
                            });
                } else if (playType == GlobalConfig.PLAY_TYPE_SOUND) {
                    //只播放声音
                    LogMgr.v(TAG, "H_DisposeProtocol 只播放声音 soundfilePath = " + soundFilePath);
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(null, soundFilePath, loopCtr, isNeedToReply,
                            delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_SOUND) {
                    //动作声音同时播放
//							String soundFilePath = filePath.replace(".bin", ".wav");
                    LogMgr.v(TAG, "H_DisposeProtocol动作声音同时播放 actionfilePath = " + actionFilePath + " soundFilePath = " +
                            "" + soundFilePath);
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, loopCtr,
                            isNeedToReply, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                } else if (playType == GlobalConfig.PLAY_TYPE_VIDEO) {
                    //只播视频文件
                    LogMgr.v(TAG, "H_DisposeProtocol只播视频文件 soundfilePath = " + soundFilePath);
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(null, soundFilePath, loopCtr, isNeedToReply,
                            delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_VIDEO) {
                    //动作视频同时播放
//							String videoFilePath = filePath.replace(".bin", ".avi");
                    LogMgr.v(TAG, "H_DisposeProtocol动作声音同时播放 actionfilePath = " + actionFilePath + " soundfilePath = " +
                            "" + soundFilePath);
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, loopCtr,
                            isNeedToReply, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                } else {
                    //播放类型错误
                    LogMgr.e(TAG, "H_DisposeProtocol 播放类型错误");
                    return;
                }
            } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_STOP) {
                //停止播放命令
                LogMgr.v(TAG, "收到播放停止命令");
                if (mPlayer != null) {
                    mPlayer.stop();
                }
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                return;
            } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig
                    .KNOW_ROBOT_IN_CMD_2_PLAY_SOUND) {
                LogMgr.v("收到播放音频命令");
                if (data[11] == (byte) 0x00) {
                    int length = (int) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
                    byte[] name = new byte[length + 4 - 12 - 1];
                    System.arraycopy(data, 12, name, 0, name.length);
                    String playName = new String(name, "utf-8");
                    LogMgr.d("paly short music playName = " + playName);
                    if (playName.endsWith(".mp3") || playName.endsWith(".wav")) {
                        mPlayer.play(playName);
                    } else {
                        mPlayer.stop();
                    }
                } else if (data[11] == (byte) 0x01) {
                    if (mPlayer != null) {
                        mPlayer.stop();
                    }
                }
            } else if (data.length >= 15 && data[5] == GlobalConfig.SELF_CHECK_IN_CMD_1 && data[6] == GlobalConfig
                    .SELF_CHECK_IN_CMD_2_JOINT) {
                LogMgr.v("关节检测自检命令");
                int length = (int) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
                byte[] cmdData = new byte[length + 4 - 12];
                System.arraycopy(data, 11, cmdData, 0, cmdData.length);
                int targetEngineId = (int) (data[11] & 0xFF);
                int targetAngle = (int) (((data[12] & 0xFF) << 8) | (data[13] & 0xFF));
                LogMgr.i("目标舵机号 = " + targetEngineId + " 目标角度 = " + targetAngle);
                if (targetEngineId < 1 || targetEngineId > 22 || targetAngle < 0 || targetAngle > 1023) {
                    LogMgr.e("参数异常 目标舵机号 = " + targetEngineId + " 目标角度 ");
                    return;
                }
                ProtocolUtils.setAMotor(targetEngineId,targetAngle);
//                byte[] engineCmd = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,
//                        GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_SET_SINGLE_ANGLE_OUT_CMD_2,
//                        cmdData);
//                SP.request(engineCmd);
//                for (int i = 0; i < 5; i++) {
//                    int positon = ProtocolUtils.getEngineAngle(targetEngineId);
//                    LogMgr.i("获取到当前角度 positon = " + positon);
//                    if (positon <= 0) {
//                        LogMgr.e("获取到角度不正确 positon = " + positon);
//                    } else if (Math.abs(targetAngle - positon) < 25) {
//                        byte[] returnCmd = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,
//                                GlobalConfig.SELF_CHECK_OUT_CMD_1, GlobalConfig.SELF_CHECK_OUT_CMD_2_JOINT, new
//                                        byte[]{(byte) 0x01});
//                        Message message = mHandler.obtainMessage();
//                        message.obj = returnCmd;
//                        mHandler.sendMessage(message);
//                        return;
//                    }
//                    Thread.sleep(20);
//                }
//                byte[] returnCmd = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,
//                        GlobalConfig.SELF_CHECK_OUT_CMD_1, GlobalConfig.SELF_CHECK_OUT_CMD_2_JOINT, new byte[]{(byte)
//                                0x02});
//                Message message = mHandler.obtainMessage();
//                message.obj = returnCmd;
//                mHandler.sendMessage(message);
            } else if (data[5] == (byte) 0x20 && data[6] == (byte) 0x03) {
                //陀螺仪，指南针
                if (mSensor.getmO() != null && mSensor.getmG() != null && mSensor.getmS() != null) {
                    Message sensorMsg = mHandler.obtainMessage();
                    byte[] datas = Utils.byteMerger(Utils.byteMerger(
                            Utils.floatsToByte(mSensor.getmO()),
                            Utils.floatsToByte(mSensor.getmG())), Utils
                            .floatsToByte(mSensor.getmS()));
                    byte[] responseBytes = ProtocolBuilder.buildProtocol(
                            (byte) ControlInfo.getMain_robot_type(),
                            ProtocolBuilder.CMD_GYRO, datas);
                    sensorMsg.obj = responseBytes;
                    mHandler.sendMessage(sensorMsg);
                    LogMgr.d("返回陀螺仪数值" + Utils.bytesToString(responseBytes));
                }
            } else if (data[5] == GlobalConfig.S_PROGRAM_PROJECT_IN_CMD_1 && data[6] == GlobalConfig
                    .S_PROGRAM_PROJECT_IN_CMD_2_PLAY_LIST) {
                LogMgr.i("S,H34项目编程 播放动作列表");
                int playListMoveCount = (int) data[11];
                if (playListMoveCount <= 0) {
                    LogMgr.e("动作列表中的动作个数<=0 退出");
                    return;
                }
                String[] moveNameArray = new String[playListMoveCount];
                int index = 12;
                int moveCountGetFromData = 0;
                for (int i = 0; i < moveNameArray.length && index < data.length - 1; i++) {
                    int length = (int) data[index];
                    if (length <= 0 || index + length >= data.length - 1) {
                        LogMgr.e("获取动作列表中的第" + i + "个动作名时，长度异常 length = " + length + " index = " + index + " data" +
                                ".length = " + data.length);
                        return;
                    }
                    moveNameArray[i] = GlobalConfig.PROGRAM_FOR_S_PATH + File.separator + new String(data, index
                            + 1, length, "UTF-8");
                    LogMgr.i("动作列表中的第" + i + "个动作名 moveName = " + moveNameArray[i]);

                    index += length + 1;
                    moveCountGetFromData++;
                }
                if (moveCountGetFromData != playListMoveCount) {
                    LogMgr.e("动作列表中的动作个数与期望的不符 退出 moveCountGetFromData = " + moveCountGetFromData + " " +
                            "playListMoveCount = " + playListMoveCount);
                    return;
                }
                //播放动作列表 TODO
                PlayMoveOrSoundUtils.getInstance().setmHandlerForReturn(mHandler);
//                PlayMoveOrSoundUtils.getInstance().handlePlayList(moveNameArray);

                if (ControlApplication.RobotFallCheck && ControlApplication.IsRobotFall
                        && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) { // 机器人摔倒了，不执行bin文件
                    LogMgr.e("机器人摔倒了，不执行bin文件");
                    return;
                }
                LogMgr.v("H_DisposeProtocol 播放动作 moveNameArray[0] = " + moveNameArray[0]);
                //如果需要踏步动作，在踏步动作后反馈客户端，否则在此动作即反馈客户端。
                boolean returnToClient = true;
                if(isNeedMarkTimeAfterMove == 1){
                    returnToClient = false;
                }
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(moveNameArray[0], null, false, returnToClient,
                        0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, new PlayMoveOrSoundUtils
                                .PlayCallBack() {


                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onPause() {

                            }

                            @Override
                            public void onResume() {

                            }

                            @Override
                            public void onStop() {
                            }

                            @Override
                            public void onSingleMoveStopWhileLoop() {

                            }

                            @Override
                            public void onNormalStop() {
                                resumeMove();
                            }

                            @Override
                            public void onForceStop() {

                            }
                        });
            } else if (data[5] == GlobalConfig.S_PROGRAM_PROJECT_IN_CMD_1 && data[6] == GlobalConfig
                    .S_PROGRAM_PROJECT_IN_CMD_2_STOP_LIST) {
                // 停止播放命令
                LogMgr.v("收到播放停止命令");
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                return;
            } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_OUT_CMD_2_TREAD_CONTROL) {//步态控制
                LogMgr.e("ControlApplication.RobotFallCheck = "+ControlApplication.RobotFallCheck + " ControlApplication.IsRobotFall = "+ControlApplication.IsRobotFall);
                if (ControlApplication.RobotFallCheck && ControlApplication.IsRobotFall) { // 摔倒之后不执行步态控制
                    LogMgr.e(TAG, "摔倒之后不执行步态控制");
                    return;
                }
                SensorImuServiceForH5.stopSensorImuServiceForH5();
                int speed[] = new int[]{10, 10, 10, 10};
                for (int i = 0; i < 4; i++) {
                    speed[i] = ((data[15] & 0x00ff) << 8) | (data[16] & 0x00ff);
                }
                GaitAlgorithm gaitAlgorithm = GaitAlgorithm.getInstance();
                gaitAlgorithm.setWalkSpeed(speed);
                LogMgr.i("GaitAlgorithm    " + (data[12] & 0xff));
                switch ((data[12] & 0xff)) {
                    case 0x0:
                        LogMgr.e("Pad端发送命令停止步态服务");
                        gaitAlgorithm.destoryWalk();
                        break;
                    case 0x01:
                        isInStepState = true;
                        gaitAlgorithm.startForwardWalk();
                        break;
                    case 0x02:
                        isInStepState = true;
                        gaitAlgorithm.startBackwardWalk();
                        break;
                    case 0x03:
                        isInStepState = true;
                        gaitAlgorithm.startLeftWalk();
                        break;
                    case 0x04:
                        isInStepState = true;
                        gaitAlgorithm.startRightWalk();
                        break;
                    case 0x05:
                        isInStepState = true;
                        gaitAlgorithm.startLeftForwardWalk();
                        break;
                    case 0x06:
                        isInStepState = true;
                        gaitAlgorithm.startRightForwardWalk();
                        break;
                    case 0x07:
                        isInStepState = true;
                        gaitAlgorithm.startLeftBackwardWalk();
                        break;
                    case 0x08:
                        isInStepState = true;
                        gaitAlgorithm.startRightBackwardWalk();
                        break;
                    case 0x09:
                        LogMgr.i("执行步态停止动作，后跟踏步动作");
                        isInStepState = false;
                        gaitAlgorithm.stopWalk();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                resumeMove();
                            }
                        },900);
                        break;
                    case 0x0A:
                        isInStepState = true;
                        gaitAlgorithm.startTurnLeftWalk();
                        break;
                    case 0x0B:
                        isInStepState = true;
                        gaitAlgorithm.startTurnRightWalk();
                        break;
                    default:
                        LogMgr.e("步态协议不正确");
                        break;
                }
            } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig
                    .KNOW_ROBOT_OUT_CMD_2_MOVE_BALANCE) { // H34动作平衡控制

//                int parameter = data[11];
//                if (parameter<=2){
                isNeedMarkTimeAfterMove = (data[13] & 0xFF);
//                }
                LogMgr.e("isNeedMarkTimeAfterMove:" + isNeedMarkTimeAfterMove);
                if ((data[12] & 0xFF) == 1) {
                    ControlApplication.RobotFallCheck = true;
                    SensorImuService.startSensorImuService();
                    // 打开摔倒检测
                } else {
                    ControlApplication.RobotFallCheck = false;
                    // 关闭摔倒检测
                }
            } else if(data[5] == (byte)0x0C && data[6] == (byte)0x01){//开始注册接收串口广播
                LogMgr.d("收到群控上报信息");
                SP.registerPushEvent(pushMsg);
            } else if(data[5] == (byte)0x0C && data[6] == (byte)0x00){//开始注册接收串口广播
                LogMgr.d("反注册收到群控上报信息");
                SP.unRegisterPushEvent(pushMsg);
            }
            //xiongxin@20171121 add start
            else if (data[5] == (byte)0x20 && data[6] == (byte)0x0E){
                LogMgr.d("H5为CREATOR模块固定舵机");
                if(data[12] == 0x00){
                    if (H5ServoFixed.getInstance().status())
                        H5ServoFixed.getInstance().stopServoFixed();
                }else{
                    if (!H5ServoFixed.getInstance().status())
                        H5ServoFixed.getInstance().startServoFixed();
                }
            }
            else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H &&
                    data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_IN_CMD_2_H5_GAIT_MOVE){
                LogMgr.i("收到H5新步态移动控制协议 data[12] = "+(int)data[12]);
                if(data[12] == (byte)0x00){
                    GaitAlgorithmForH5.getInstance().stopMove();
                }else if(data[12] == (byte)0x01){
                    float angle = Utils.byte2float(data,13);
                    float step = Utils.byte2float(data,17);
                    LogMgr.i("收到H5新步态移动控制协议angle = " + angle + " step = " + step);
                    if(angle < -180f || angle > 180f || step < 0f || step > 190f){
                        LogMgr.e("参数异常 angle = " + angle + " step = " + step);
                        return;
                    }
                    GaitAlgorithmForH5.getInstance().move(angle, step);
                }else if(data[12] == (byte)0x02){
                    GaitAlgorithmForH5.getInstance().stopGait();
                }else{
                    LogMgr.e("移动参数错误");
                }
            }
            else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H &&
                    data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_IN_CMD_2_H5_GAIT_DERECTION){
                LogMgr.i("收到H5新步态方向控制协议 data[12] = "+(int)data[12]);
                if(data[12] == (byte)0x01){
                    GaitAlgorithmForH5.getInstance().setBackward();
                }else if(data[12] == (byte)0x00){
                    GaitAlgorithmForH5.getInstance().setForward();
                }else{
                    LogMgr.e("方向参数错误");
                }
            }
            else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H &&
                    data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_IN_CMD_2_H5_GAIT_SETTING){
                LogMgr.i("收到H5新步态设置控制协议 data[12] = "+(int)data[12] +" data[13] = "+(int)data[13]);
                int value = (int)(data[13]&0xFF);
                if(value > 100){
                    LogMgr.e("设置参数 > 100 错误");
                    return;
                }
                if(data[12] == (byte)0x00){
                    GaitAlgorithmForH5.getInstance().setSpeedLimit(value);
                }else if(data[12] == (byte)0x01){
                    GaitAlgorithmForH5.getInstance().walk2Run(value);
                }else{
                    LogMgr.e("设置参数错误");
                }
            }else if(data[5] ==  GlobalConfig.H_GROUP_CMD_1 && data[6] ==  GlobalConfig.H_GROUP_CMD_CAD){//舵机标定
                LogMgr.d("data = " + Utils.bytesToString(data));
                int posLen = data[11];
                byte[] totalMotor = new byte[posLen];
                System.arraycopy(data,12,totalMotor,0,posLen);
                ProtocolUtils.ServoCalibration(posLen,totalMotor);
            }
            else if(data[5] ==  GlobalConfig.H_GROUP_CMD_1 && data[6] ==  GlobalConfig.H_GROUP_CMD_RELEASE){//舵机固定释放
                LogMgr.d("data = " + Utils.bytesToString(data));
                int posLen = data[11];
                if(posLen == 2){
                    ProtocolUtils.goServePosZeros();
                }else{
                    ProtocolUtils.relAndFix(posLen, (byte) 0x18);
                }
            }
            //add end
            else {
//                LogMgr.v("控制命令 直接发至stm32 data = "+Arrays.toString(data));
                SP.request(data, 15);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e(TAG, "HProtocolDisposer dispose protocol error::" + e);
        }
    }
    public static int doingIndex = -1;
    public static int receiveIndex = 0;
    private final int cmdtype = 6;
    private final int cmdNum = 11;// 鏂版寚浠よ嚜鍔�1
    private final int recvCmdNUm = 12;// 褰撳墠鍛戒护鐨刬ndex
    private final int CmdAllNum = 13;// 鍛戒护鍙戦�佺殑鎬绘鏁�

    private Timer batteryTimer;
    private TimerTask batteryTimerTask;
    private long m_millisecond0 = 2000; // 200ms定时器
    public static int batteryColor = 0;
    private PushMsg pushMsg = new PushMsg(){
        @Override
        public void onPush(byte[] pushData) {
            super.onPush(pushData);

            LogMgr.d("pushData = " + Utils.bytesToString(pushData));
            if(pushData[0] == (byte)0xAA && pushData[1] == (byte)0x55 && pushData[5] == (byte)0xC0){
                LogMgr.d("H5 收到433信息");
                SeriWatch(pushData);
            }
        }
    };
    public final static String MOVEBIN_DIR_GROUP = Environment.getExternalStorageDirectory()+ File.separator +"Abilix" + File.separator + "Download"+ File.separator;
    private void SeriWatch(byte[] recv) {
        if((recv[cmdtype] & 0xff) == 32 || ((recv[cmdtype] & 0xff) != 32) && doingIndex != (recv[cmdNum] & 0xff)){
            if ((recv[cmdtype] & 0xff) != 32)
                doingIndex = (recv[cmdNum] & 0xff);
            if ((recv[cmdtype] & 0xff) != 31 && (recv[cmdtype] & 0xff) != 32 && (recv[cmdtype] & 0xff) != 33){
                receiveIndex = (recv[recvCmdNUm] & 0xff);
                int sleeptime0 = (recv[CmdAllNum] & 0xff);
                try{
                    Thread.sleep((sleeptime0 - receiveIndex) * 200);
                }catch (Exception e){
                    e.printStackTrace();
                }
                switch (recv[cmdtype]){
                    case 0://释放，13,14,15,16,17,18,19,20,21,22
                        byte[] color = new byte[14];//这里显示的额头和双眼的灯
                        color[0] = 2;
                        color[1] = 3;
                        color[2] = 1;
                        color[6] = 2;
                        color[10] = 3;
                        color[3] = (byte)0xFF;
                        color[7] = (byte)0xFF;
                        color[11] = (byte)0xFF;
                        color[4] = (byte)0xFF;
                        color[8] = (byte)0xFF;
                        color[12] = (byte)0xFF;
                        color[5] = (byte)0xFF;
                        color[9] = (byte)0xFF;
                        color[13] = (byte)0xFF;
                        SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0xC0,color));
                        try {
                            Thread.sleep(100);
                            byte[] data = new byte[]{(byte)10,(byte)13,0x02,(byte)14,0x02,(byte)15,0x02,(byte)16,0x02,
                                    (byte)17,0x02,(byte)18,0x02,(byte)19,0x02,(byte)20,0x02,(byte)21,0x02,(byte)22,0x02};
                            for (int i = 0; i < 2; i++) {
                                SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0x65,data));
                                Thread.sleep(50);
                            }
                            Thread.sleep(8000);
                            color[3] = (byte)0x00;
                            color[7] = (byte)0x00;
                            color[11] = (byte)0x00;
                            color[4] = (byte)0x00;
                            color[8] = (byte)0x00;
                            color[12] = (byte)0x00;
                            color[5] = (byte)0x00;
                            color[9] = (byte)0x00;
                            color[13] = (byte)0x00;
                            SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0xC0,color));
                            Thread.sleep(50);
                            SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0xC1,new byte[]{0x02,0x00,0x00}));
                            Thread.sleep(50);
                            SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0xC2,new byte[]{0x02,0x00,0x00,0x00}));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 1://固定
                        byte[] datas = new byte[45];
                        datas[0] = (byte) 22;
                        for (int i = 1; i < 23; i++) {
                            int j = 2 * i - 1;
                            datas[j] = (byte) i;
                            datas[j + 1] = (byte) 1;
                        }
                        SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0x65,datas));
                        break;
                    case 2://归零（这里先使用一个bin文件）
                        PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                        String movePathZero = MOVEBIN_DIR_GROUP + "Rig_H5.bin";
                        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePathZero, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onPause() {

                            }

                            @Override
                            public void onResume() {

                            }

                            @Override
                            public void onStop() {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
                                    }
                                },2500);
                            }
                            @Override
                            public void onSingleMoveStopWhileLoop() {

                            }

                            @Override
                            public void onNormalStop() {

                            }

                            @Override
                            public void onForceStop() {

                            }
                        });
                        break;
                    case 3:
                        int fileNamelength = recv[11 + 6];
                        byte[] name = new byte[fileNamelength];
                        System.arraycopy(recv, 18, name, 0, fileNamelength);
                        LogMgr.d("name = " + Utils.bytesToString(name));
                        String fillename = new String(name);
                        LogMgr.d("fillename  ="+ fillename);
//                        PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                        String movePath = MOVEBIN_DIR_GROUP + fillename + ".bin";
//                        String soundPath = MOVEBIN_DIR_GROUP + fillename + ".mp3";
                        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onPause() {

                            }

                            @Override
                            public void onResume() {

                            }

                            @Override
                            public void onStop() {
//                                mHandler.postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
//                                    }
//                                },2500);
                            }
                            @Override
                            public void onSingleMoveStopWhileLoop() {

                            }

                            @Override
                            public void onNormalStop() {

                            }

                            @Override
                            public void onForceStop() {

                            }
                        });

                        break;
                    case 4://打开电量检测
                        int batteryPct =  SharedPreferenceTools.getInt(instance, SharedPreferenceTools
                                .SHAREDPREFERENCE_KEY_ELECTRICITY, 0);
                        LogMgr.e("batteryPct = " + batteryPct);
                        if(batteryPct < 30){
                            batteryColor = 1;//R
                        }else if(batteryPct < 70){
                            batteryColor = 2;//G
                        }else {
                            batteryColor = 3;//B
                        }
                        RGBEyesBlink(batteryColor);
                        break;
//                    case 61:
                    case 5://关闭电量检测
                        RGBEyesBlink(0);
                        break;
                    case 6://停止
                        PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                        break;
                    case 60://打开LED
                        DoWriteFrame2((byte)0x01);
                        break;
                    case 61:  //关闭LED
                        DoWriteFrame2((byte)0x00);
                        break;
                    case 62:  //LED闪烁
                        DoWriteFrame2((byte)0x02);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void RGBEyesBlink(int batteryColor) {
        byte[] color = new byte[14];//这里显示的额头和双眼的灯
        color[0] = 2;
        color[1] = 3;
        color[2] = 1;
        color[6] = 2;
        color[10] = 3;
        switch (batteryColor){
            case 1://打开-低电量
                color[3] = (byte)0xFF;
                color[7] = (byte)0xFF;
                color[11] = (byte)0xFF;
                break;
            case 2://打开-中电量
                color[4] = (byte)0xFF;
                color[8] = (byte)0xFF;
                color[12] = (byte)0xFF;
                break;
            case 3://打开-高电量
                color[5] = (byte)0xFF;
                color[9] = (byte)0xFF;
                color[13] = (byte)0xFF;
                break;
            case 0://关闭电量检测
                break;
        }
        SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0xC0,color));
    }
    private void goZero() {//归零
        SP.write(ProtocolUtils.buildProtocol((byte)0x03,(byte)0xA3,(byte)0x78,new byte[]{0x02,0x03,0x00}));
    }

    private void DoWriteFrame2(byte data) {//该
        int iLen=9, n=0;
        byte[] sendBuff = new byte[13];
        sendBuff[0] = (byte)0xAA;
        sendBuff[1] = (byte)0x55;
        sendBuff[2] = (byte)((iLen>>8) & 0xFF);
        sendBuff[3] = (byte)(iLen & 0xFF);
        sendBuff[4] = (byte)0x00;
        sendBuff[5] = (byte)0xA3;
        sendBuff[6] = (byte)0xA4;
        sendBuff[7] = 0x00;
        sendBuff[8] = 0x00;
        sendBuff[9] = 0x00;
        sendBuff[10] = 0x00;
        sendBuff[11] = (byte)data;
        sendBuff[12] = 0x00;
        for(n=0; n<12; n++){
            sendBuff[12] += sendBuff[n];
        }
        SP.write(sendBuff);
    }

    /**
     * 构建控制手脚上灯光的下传命令
     *
     * @param engineNo    舵机号
     * @param TurnOnOrOff 打开还是关闭 手臂1是打开 脚0是打开
     * @return
     * @author Yang
     */
    public byte[] getOnOrOffCmd(int engineNo, byte TurnOnOrOff) {

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
        Cmd[9] = (byte) 0x04;
        Cmd[10] = (byte) 0x03;
        Cmd[11] = (byte) 0x19;
        Cmd[12] = TurnOnOrOff;
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

        LogMgr.d(TAG, "Cmd = " + Arrays.toString(Cmd));

        return Cmd;

    }

    /**
     * 构造传至串口0控制头顶眼睛耳朵眼色的命令
     */
    private byte[] getColorCmdToSerial0(byte[] data) {
        // 控制颜色的命令
        LogMgr.d(TAG, "构建控制颜色的命令");
        byte[] colorCmd = new byte[20];
        colorCmd[0] = (byte) 0xAA;
        colorCmd[1] = (byte) 0x53;
        // 头部颜色
        colorCmd[2] = data[12];
        colorCmd[3] = data[13];
        colorCmd[4] = data[14];
        // 左眼灯光
        colorCmd[5] = data[15];
        colorCmd[6] = data[16];
        colorCmd[7] = data[17];
        // 右眼灯光
        colorCmd[8] = data[18];
        colorCmd[9] = data[19];
        colorCmd[10] = data[20];
        // 左耳灯光
        colorCmd[11] = data[21];
        colorCmd[12] = data[22];
        colorCmd[13] = data[23];
        // 右耳灯光
        colorCmd[14] = data[24];
        colorCmd[15] = data[25];
        colorCmd[16] = data[26];
        // 结尾3字节
        colorCmd[17] = (byte) 0x00;
        colorCmd[18] = (byte) 0x00;
        colorCmd[19] = (byte) 0x00;

        return colorCmd;
    }

    private void setColorCmdListToSerial1(List<byte[]> cmdToSendToEngine1List, byte[] data) {
        LogMgr.d(TAG, "构建控制亮灭的命令");
        if ((data[27] == 0x01 || data[27] == 0x02) && (data[28] == 0x01 || data[28] == 0x02)
                && (data[29] == 0x01 || data[29] == 0x02)
                && (data[30] == 0x01 || data[30] == 0x02)) {
            LogMgr.d(TAG, "亮灭指令正确");
            // 手臂点亮的命令是1 脚点亮的命令是0
            byte leftHandTurnOnOrOff = (data[27] == 0x01) ? (byte) 0x01 : (byte) 0x00;
            byte rightHandTurnOnOrOff = (data[28] == 0x01) ? (byte) 0x01 : (byte) 0x00;
            byte leftFeetTurnOnOrOff = (data[29] == 0x01) ? (byte) 0x00 : (byte) 0x01;
            byte rightFeetTurnOnOrOff = (data[30] == 0x01) ? (byte) 0x00 : (byte) 0x01;

            if (leftHandTurnOnOrOff == 0x01) {
                LogMgr.d(TAG, "点亮左臂");
            } else {
                LogMgr.d(TAG, "熄灭左臂");
            }
            if (rightHandTurnOnOrOff == 0x01) {
                LogMgr.d(TAG, "点亮右臂");
            } else {
                LogMgr.d(TAG, "熄灭右臂");
            }
            if (leftFeetTurnOnOrOff == 0x01) {
                LogMgr.d(TAG, "熄灭左脚");
            } else {
                LogMgr.d(TAG, "点亮左脚");
            }
            if (rightFeetTurnOnOrOff == 0x01) {
                LogMgr.d(TAG, "熄灭右脚");
            } else {
                LogMgr.d(TAG, "点亮右脚");
            }

            byte[] leftHandCmd = getOnOrOffCmd(19, leftHandTurnOnOrOff);
            byte[] rightHandCmd = getOnOrOffCmd(20, rightHandTurnOnOrOff);
            byte[] leftFeetCmd = getOnOrOffCmd(23, leftFeetTurnOnOrOff);
            byte[] rightFeetCmd = getOnOrOffCmd(24, rightFeetTurnOnOrOff);

            if (cmdToSendToEngine1List == null) {
                cmdToSendToEngine1List = new ArrayList<byte[]>();
            } else {
                cmdToSendToEngine1List.clear();
            }

            cmdToSendToEngine1List.add(leftHandCmd);
            cmdToSendToEngine1List.add(rightHandCmd);
            cmdToSendToEngine1List.add(leftFeetCmd);
            cmdToSendToEngine1List.add(rightFeetCmd);

            LogMgr.d(TAG, "构建控制亮灭的命令 完成");
        } else {
            LogMgr.d(TAG, "亮灭指令错误");
            return;
        }
    }

    /**
     * H34恢复动作初始状态
     */
    private void  resumeMove() {
        LogMgr.d("是否执行踏步动作 = " + (isNeedMarkTimeAfterMove == 1 && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3));
        if (isNeedMarkTimeAfterMove == 1 && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            String movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_MARK_TIME;
            isNeedMarkTimeAfterMove = 0;
            PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, true, 0, false,
                    PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                        @Override
                        public void onStart() {
                            LogMgr.e("踏步动作刚开始时：ControlApplication.RobotFallCheck = "+ControlApplication.RobotFallCheck);
                        }

                        @Override
                        public void onPause() {

                        }

                        @Override
                        public void onResume() {

                        }

                        @Override
                        public void onStop() {

                        }

                        @Override
                        public void onSingleMoveStopWhileLoop() {

                        }

                        @Override
                        public void onNormalStop() {
                            LogMgr.e("踏步动作刚结束时：ControlApplication.RobotFallCheck = "+ControlApplication.RobotFallCheck);
                            ControlApplication.RobotFallCheck = false;
                        }

                        @Override
                        public void onForceStop() {

                        }
                    });

        }else{
            ControlApplication.RobotFallCheck = false;
            isNeedMarkTimeAfterMove = 0;
        }
    }

    public static class H5ServoFixed implements Runnable{
        private static H5ServoFixed mH5ServoFixed;
        private Thread task;
        private boolean isRunning = false;

        public static H5ServoFixed getInstance(){
            if (mH5ServoFixed ==null){
                mH5ServoFixed = new H5ServoFixed();
            }
            return mH5ServoFixed;
        }

        private H5ServoFixed(){}

        public boolean status(){
            return isRunning;
        }

        public void startServoFixed(){
            LogMgr.i("舵机固定 startServoFixed");
            fixedOrReleaseServo((byte) 0x02);
            if (task ==null &&!isRunning){
                task =new Thread(this);
                isRunning  = true;
                task.start();

            }

        }

        public void stopServoFixed(){
            LogMgr.i("舵机固定 stopServoFixed");
            fixedOrReleaseServo((byte) 0x01);
            isRunning = false;
            task = null;
        }

        private void fixedOrReleaseServo(byte status){
            byte[] data = new byte[45];
            data[0] = 0x16;
            for (int i=1;i<=data[0];i++){
                data[i*2 -1] = (byte) i;
                data[i*2] = status;
            }
            SP.request(ProtocolBuilder.buildProtocol((byte) 0x03,(byte) 0xA3,(byte) 0x65,data));
        }

        @Override
        public void run() {
            while (isRunning){
                byte[] receiver = SP.request(ProtocolBuilder.buildProtocol((byte) 0x03,(byte) 0xA3,(byte) 0x61,null));
                if (receiver !=null &&receiver.length>0 && receiver[5] ==0xF0 &&receiver[6] ==0x61){
                    byte[] buf = new byte[67];
                    buf[0] = 0x16;
                    for (int i=1;i<=buf[0];i++){
                        buf[i] = (byte) i;
                        buf[i*2+21] = receiver[i*2+9];
                        buf[i*2+22] = receiver[i*2+10];
                    }
                    SP.request(ProtocolBuilder.buildProtocol((byte) 0x03,(byte) 0xA3,(byte) 0xA2,buf));
                }else LogMgr.i("舵机固定收到角度有误");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
