package com.abilix.control.pad;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MProtocolDisposer extends AbstractProtocolDisposer {

    public MProtocolDisposer(Handler mHandler) {
        super(mHandler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        byte[] data = (byte[]) msg.obj;
        LogMgr.d("pad cmd::" + Utils.bytesToString(data));
        // 陀螺仪 指南针
        if (data[1] == 'P' && data[2] == 'O' && data[3] == 'S') {
            if (mSensor != null && mSensor.getmO() != null) {
                byte[] datas = Utils.byteMerger(Utils.byteMerger(Utils.floatsToByte(mSensor.getmO()), Utils.floatsToByte(mSensor.getmG())), Utils.floatsToByte(mSensor.getmS()));
                Message sensorMsg = mHandler.obtainMessage();
                sensorMsg.obj = datas;
                mHandler.sendMessage(sensorMsg);
            }
        } else if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x20 && data[6] == (byte) 0x03) {
            // 陀螺仪新协议
            Message sensorMsg = mHandler.obtainMessage();
            if (mSensor.getmO() != null && mSensor.getmG() != null && mSensor.getmS() != null) {
                byte[] datas = Utils.byteMerger(Utils.byteMerger(Utils.floatsToByte(mSensor.getmO()), Utils.floatsToByte(mSensor.getmG())), Utils.floatsToByte(mSensor.getmS()));
                byte[] responseBytes = ProtocolBuilder.buildProtocol((byte) ControlInitiator.ROBOT_TYPE_M, ProtocolBuilder.CMD_GYRO, datas);
                sensorMsg.obj = responseBytes;
                mHandler.sendMessage(sensorMsg);
                LogMgr.d("陀螺仪新协议 返回陀螺仪数值");
            }
        } else if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x20 && data[6] == (byte) 0x04) {
            byte[] protocolbytes = ProtocolBuilder.getData(data);
            byte isPlay = protocolbytes[0];
            if (isPlay == (byte) 0x00) {
                byte[] musicNameByes = new byte[protocolbytes.length - 1];
                System.arraycopy(protocolbytes, 1, musicNameByes, 0, protocolbytes.length - 1);
                String musicName = new String(musicNameByes);
                LogMgr.d("播放音频：" + musicName);
                mPlayer.play(musicName);
            } else if (isPlay == (byte) 0x01) {
                LogMgr.d("停止播放");
                mPlayer.stop();
            }
        } else if (data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_PAUSE) {
            // skillplayer暂停命令
            PlayMoveOrSoundUtils.getInstance().pauseCurrentMove();
            return;
        } else if (data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_RESUME) {
            // skillplayer续播命令
            PlayMoveOrSoundUtils.getInstance().resumeCurrentMove();
            return;
        } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_STOP) {
            // 停止播放命令
            LogMgr.v("收到播放停止命令");
            PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
            return;
        } else if (ProtocolUtils.isMultiMediaCmd(data)) {
            if (ProtocolUtils.isMultiMediaAudioPlayCmd(data)) {
                LogMgr.i("收到多媒体音频播放命令");
                byte[] temp = new byte[data.length - 12 - 6];
                System.arraycopy(data, 17, temp, 0, temp.length);
                String filePath = "";
                try {
                    filePath = new String(temp, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String totalPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" + File.separator + "media" + filePath;
                File file = new File(totalPath);
                if (file.exists() && file.isFile()) {
                    LogMgr.i("文件存在 file = " + totalPath);
                    mPlayer.setOnCompletionListener(new OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                                    GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY_COMPLETE, null);
                            Message message = mHandler.obtainMessage();
                            message.obj = returnData;
                            mHandler.sendMessage(message);
                        }
                    });
                    mPlayer.playSoundFile(totalPath);
                    byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                            GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY, new byte[]{(byte) 0x00});
                    Message message = mHandler.obtainMessage();
                    message.obj = returnData;
                    mHandler.sendMessage(message);
                } else {
                    LogMgr.w("文件不存在 file = " + totalPath);
                    byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                            GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PLAY, new byte[]{(byte) 0x01});
//					returnToClient(returnData);
                    Message message = mHandler.obtainMessage();
                    message.obj = returnData;
                    mHandler.sendMessage(message);
                }
            } else if (ProtocolUtils.isMultiMediaPauseCmd(data)) {
                LogMgr.i("收到多媒体暂停命令");
                mPlayer.pause();
                byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                        GlobalConfig.MULTI_MEDIA_OUT_CMD_2_PAUSE, null);
                Message message = mHandler.obtainMessage();
                message.obj = returnData;
                mHandler.sendMessage(message);
            } else if (ProtocolUtils.isMultiMediaResumeCmd(data)) {
                LogMgr.i("收到多媒体续播命令");
                mPlayer.resume();
                byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                        GlobalConfig.MULTI_MEDIA_OUT_CMD_2_RESUME, null);
                Message message = mHandler.obtainMessage();
                message.obj = returnData;
                mHandler.sendMessage(message);
            } else if (ProtocolUtils.isMultiMediaStopCmd(data)) {
                LogMgr.i("收到多媒体停止命令");
                mPlayer.stop();
                byte[] returnData = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_C, GlobalConfig.MULTI_MEDIA_OUT_CMD_1,
                        GlobalConfig.MULTI_MEDIA_OUT_CMD_2_STOP, null);
                Message message = mHandler.obtainMessage();
                message.obj = returnData;
                mHandler.sendMessage(message);
            } else {
                LogMgr.e("收到无效的多媒体命令");
            }
        }
        // 老协议
        else if (data[0] == 0x55) {
            try {
                SP.request(data);
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("write data error::" + e);
            }
        } else if (data[0] == 0x56) {
            try {
                byte[] read_byte = SP.request(data);
                Message sensorMsg = mHandler.obtainMessage();
                sensorMsg.what = 0;
                sensorMsg.obj = read_byte;
                mHandler.sendMessage(sensorMsg);
            } catch (Exception e) {
                LogMgr.e("write response data error::" + e);
                e.printStackTrace();
            }
        }
        // 新协议
        else if ((data[0] & 0xff) == 0xaa && data[1] == 0x55) {
            try {
                LogMgr.e("CheckLength():" + (CheckLength(data)));
                if (Check(data)) {
                    byte[] read_byte = SP.request(data);
                    Message sensorMsg = mHandler.obtainMessage();
                    sensorMsg.what = 1;
                    sensorMsg.obj = read_byte;
                    mHandler.sendMessage(sensorMsg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("write data error::" + e);
            }
        } else {
            try {
                String playName = new String(data, "utf-8");
                LogMgr.d("music name::" + playName);
                if (playName.endsWith(".mp3") || playName.endsWith(".wav")) {
                    LogMgr.d("paly short music");
                    mPlayer.play(playName);
                } else {
                    mPlayer.stop();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * 校验
     *
     * @param data
     * @return
     */
    public boolean Check(byte[] data) {
        int j = 0;
        int length = data.length - 1;
        for (int i = 0; i < length; i++) {
            j += data[i] < 0 ? (data[i] & 0xff) : data[i];
        }
        byte z = (byte) j;
        if (z == data[length]) {
            return true;
        } else {
            LogMgr.e("出错的 数据:" + Arrays.toString(data));
            return false;
        }
    }

    /**
     * 校验
     *
     * @param data
     * @return
     */
    public boolean CheckLength(byte[] data) {
        int j = (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
        int length = data.length - 4;
        LogMgr.i("j:" + j + " length:" + length);
        if (j == length) {
            return true;
        } else {
            LogMgr.e("出错的 数据:" + Arrays.toString(data));
            return false;
        }
    }

    @Override
    public void stopDisposeProtocol() {
        super.stopDisposeProtocol();
    }
}
