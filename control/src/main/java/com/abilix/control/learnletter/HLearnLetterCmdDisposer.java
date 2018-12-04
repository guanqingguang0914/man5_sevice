package com.abilix.control.learnletter;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.patch.PlayMoveOrSoundUtils.PlayCallBack;
import com.abilix.control.utils.LogMgr;

import java.io.File;

public class HLearnLetterCmdDisposer extends AbstractLearnLetterCmdDisposer {

    public HLearnLetterCmdDisposer(Handler mHandler) {
        super(mHandler);
    }

    @Override
    public void disposeLearnLetterCmd(int modeState, String fileFullPath) {
        LogMgr.v("modeState = " + modeState + " fileFullPath = " + fileFullPath);
        if (modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_STOP
                || modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_STOP) {
            //停止当前动作
            LogMgr.v("modeState == 0 停止当前动作");
            PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
        } else if (modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_PLAY || modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_LOOP
                || modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_PLAY_AUTO_BALANCE) {
            //播放当前动作
            LogMgr.v("播放当前动作 modeState = "+ modeState);

            String actionFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + fileFullPath;
            String soundFilePath = actionFilePath.replace(".bin", ".mp3");
            if (!new File(soundFilePath).exists()) {
                LogMgr.w("没有找到对应的MP3文件，使用.wav格式");
                soundFilePath = actionFilePath.replace(".bin", ".wav");
            }
            LogMgr.v("actionFilePath = " + actionFilePath + " soundFilePath = " + soundFilePath);

            File actionFile = new File(actionFilePath);
            File soundFile = new File(soundFilePath);
            boolean isLoop;
            if(modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_LOOP){
                isLoop = true;
            }else{
                isLoop = false;
            }
            boolean isAutoBalance;
            if(modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_PLAY_AUTO_BALANCE){
                isAutoBalance = true;
            }else{
                isAutoBalance = false;
            }
            if (actionFile.exists() && soundFile.exists()) {
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, isLoop, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, isAutoBalance, true, zhaohui_callback);
            } else if (actionFile.exists()) {
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, null, isLoop, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, isAutoBalance, true, zhaohui_callback);
            } else {
                LogMgr.e("没有找到文件 actionFile = " + actionFile);
            }
        } else if (modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_PAUSE
                || modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_PAUSE) {
            //暂停当前动作
            LogMgr.d("modeState == 2 暂停当前动作");
            PlayMoveOrSoundUtils.getInstance().pauseCurrentMove();

        } else if (modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_RESUME
                || modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_RESUME) {
            //续播当前动作
            LogMgr.d("modeState == 3 续播当前动作");
            PlayMoveOrSoundUtils.getInstance().resumeCurrentMove();

        } else if (modeState == AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_PLAY) {
            //播放当前动作
            LogMgr.v("modeState == 0x11 播放当前动作");

            String actionFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + fileFullPath;
            String soundFilePath = actionFilePath.replace(".bin", ".mp3");
            if (!new File(soundFilePath).exists()) {
                LogMgr.w("没有找到对应的MP3文件，使用.wav格式");
                soundFilePath = actionFilePath.replace(".bin", ".wav");
            }
            LogMgr.v("actionFilePath = " + actionFilePath + " soundFilePath = " + soundFilePath);

            File actionFile = new File(actionFilePath);
            File soundFile = new File(soundFilePath);
            if (actionFile.exists() && soundFile.exists()) {
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, soundFilePath, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, playCallBack);
            } else if (actionFile.exists()) {
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFilePath, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, playCallBack);
            } else {
                LogMgr.e("没有找到文件 actionFile = " + actionFile);
            }
        } else {
            LogMgr.e("modeState参数错误。");
        }
    }
    private PlayCallBack playCallBack  = new PlayCallBack() {
        @Override
        public void onStart() {
            Message msg = mHandler.obtainMessage();
            byte[] data = new byte[2];
            data[0] = 0x0E;
            data[1] = AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_PLAY;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onPause() {
            Message msg = mHandler.obtainMessage();
            byte[] data = new byte[2];
            data[0] = 0x0E;
            data[1] = AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_PAUSE;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onResume() {
            Message msg = mHandler.obtainMessage();
            byte[] data = new byte[2];
            data[0] = 0x0E;
            data[1] = AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_RESUME;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onStop() {
            Message msg = mHandler.obtainMessage();
            byte[] data = new byte[2];
            data[0] = 0x0E;
            data[1] = AbstractLearnLetterCmdDisposer.MODE_STATE_EXPLAIN_STOP;
            msg.obj = data;
            mHandler.sendMessage(msg);
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
    };
}
