package com.abilix.control.skillplayer;

import java.io.File;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.TextUtils;

import com.abilix.control.GlobalConfig;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class HSkillPlayerCmdDisposer extends AbstractSkillPlayerCmdDisposer {


    public HSkillPlayerCmdDisposer(Handler mHandler) {
        super(mHandler);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void disposeSkillPlayerCmd(byte[] Cmd, int modeState) {
        LogMgr.d("H_disposeSkillPlayerCmd()");
        try {
            LogMgr.d("SkillPlayerCmd = " + Utils.bytesToString(Cmd));
            if (modeState == 0) {
                LogMgr.i("disposeSkillPlayerCmd() modeState = " + modeState);
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                return;
            }
            if (Cmd.length <= 5) {
                LogMgr.e("命令长度不足");
                return;
            }

            byte[] tempbuff = new byte[Cmd.length - 5];
            System.arraycopy(Cmd, 5, tempbuff, 0, tempbuff.length); // 文件名
            String fileNameTemp = new String(tempbuff, "UTF-8");
            LogMgr.d("fileNameTemp = " + fileNameTemp);
            //文件路径
            String actionFilePath = null;
            String soundFilePath = null;
            String folderPath = null;
            String actionFileName = null;
            String soundFileName = null;
            if (TextUtils.isEmpty(fileNameTemp)) {
                LogMgr.e("文件名为空");
                return;
            }
            //上级App
            byte upperAppType = Cmd[0];
            //播放类型
            byte playType = Cmd[1];
            //循环控制
            boolean loopCtr = false;
            if (Cmd[2] == (byte) 0x00) {
                LogMgr.d("当前命令不循环");
                loopCtr = false;
            } else if (Cmd[2] == (byte) 0x01) {
                LogMgr.d("当前命令循环");
                loopCtr = true;
            } else {
                LogMgr.e("循环控制位错误");
                loopCtr = false;
            }
            //第一帧后延迟时间
            int delayTime = (int) ((Cmd[3] & 0xFF) << 8 | Cmd[4] & 0xFF);
            boolean isSkillPlayCmd = false;

            if (upperAppType == GlobalConfig.APP_TYPE_SKILL_PLAYER) {
                LogMgr.d("上级App是SkillPlayer");
                isSkillPlayCmd = true;
                PlayMoveOrSoundUtils.getInstance().setmHandlerForReturn(mHandler);
            } else {
                LogMgr.d("上级App不是SkillPlayer");
                isSkillPlayCmd = false;
            }

            String[] folderAndFiles = fileNameTemp.split("\\\\");
            if (folderAndFiles.length == 2) {
                folderPath = folderAndFiles[0];
                String fileNames = folderAndFiles[1];
                if (fileNames.contains("&")) {
                    actionFileName = fileNames.split("&")[0];
                    soundFileName = fileNames.split("&")[1];
                    actionFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File.separator + actionFileName;
                    soundFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File.separator + soundFileName;
                } else if (fileNames.toLowerCase().endsWith(".bin")) {
                    actionFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File.separator + fileNames;
                    soundFilePath = null;
                } else if (fileNames.toLowerCase().endsWith(".wav") || fileNames.toLowerCase().endsWith(".avi") || fileNames.toLowerCase().endsWith(".mp3")) {
                    actionFilePath = null;
                    soundFilePath = GlobalConfig.SKILLPLAYER_PATH + File.separator + folderPath + File.separator + fileNames;
                }
            } else {
                LogMgr.e("文件名格式不正确");
                return;
            }

            LogMgr.d("actionfilePath = " + actionFilePath + " soundfilePath = " + soundFilePath);

            LogMgr.d("延迟时间 delayTime = " + delayTime);

            //根据播放类型的不同作不同的处理
            if (playType == GlobalConfig.PLAY_TYPE_ACTION) {
                //只播放动作
                LogMgr.v("H_DisposeProtocol 只播放动作 actionfilePath = " + actionFilePath);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, null, loopCtr, isSkillPlayCmd, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
            } else if (playType == GlobalConfig.PLAY_TYPE_SOUND) {
                //只播放声音
                LogMgr.v("H_DisposeProtocol 只播放声音 soundfilePath = " + soundFilePath);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(null, soundFilePath, loopCtr, isSkillPlayCmd, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
            } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_SOUND) {
                //动作声音同时播放
//				String soundFilePath = filePath.replace(".bin", ".wav");
                LogMgr.v("H_DisposeProtocol动作声音同时播放 actionfilePath = " + actionFilePath + " soundFilePath = " + soundFilePath);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, loopCtr, isSkillPlayCmd, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
            } else if (playType == GlobalConfig.PLAY_TYPE_VIDEO) {
                //只播视频文件
                LogMgr.v("H_DisposeProtocol只播视频文件 soundfilePath = " + soundFilePath);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(null, soundFilePath, loopCtr, isSkillPlayCmd, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
            } else if (playType == GlobalConfig.PLAY_TYPE_ACTION_AND_VIDEO) {
                //动作视频同时播放
//				String videoFilePath = filePath.replace(".bin", ".avi");
                LogMgr.v("H_DisposeProtocol动作声音同时播放 actionfilePath = " + actionFilePath + " soundfilePath = " + soundFilePath);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, loopCtr, isSkillPlayCmd, delayTime, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
            } else {
                //播放类型错误
                LogMgr.e("H_DisposeProtocol 播放类型错误");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("disposeSkillPlayerCmd error::" + e);
        }
    }
}
