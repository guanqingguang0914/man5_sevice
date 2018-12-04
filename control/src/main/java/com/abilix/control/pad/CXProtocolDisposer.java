package com.abilix.control.pad;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.balancecar.BalanceCarData;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;



public class CXProtocolDisposer extends AbstractProtocolDisposer {
    private BalanceCarData balancecarData = null;

    public CXProtocolDisposer(Handler handler) {
        super(handler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        LogMgr.e("receive message");
        byte[] data = (byte[]) msg.obj;
        LogMgr.d("pad cmd CX ::" + Utils.bytesToString(data));

        try {
            if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55) {
                if (data[5] == (byte) 0x08 && data[6] == (byte) 0x04) {
                    balancecarData = BalanceCarData.GetManger(
                            mHandler, 1);
                    balancecarData.SetBalanceCar((data[11] & 0xff),
                            (data[12] & 0xff), (data[13] & 0xff));
                } else if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55
                        && data[5] == (byte) 0x08 && data[6] == (byte) 0x05) {
                    balancecarData = BalanceCarData.GetManger(
                            mHandler, 1);
                    balancecarData.InitBalanceCar((data[11] & 0xff),
                            (data[12] & 0xff));
                } else if (ProtocolUtils.isMultiMediaCmd(data)) {
                    if (ProtocolUtils.isMultiMediaAudioPlayCmd(data)) {
                        LogMgr.i("收到多媒体音频播放命令");
                        byte[] temp = new byte[data.length - 12 - 6];
                        System.arraycopy(data, 17, temp, 0, temp.length);
                        String filePath = new String(temp, "UTF-8");
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
//							returnToClient(returnData);
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
                } else if (data[5] == (byte) 0x20 && data[6] == (byte) 0x03) {
                    Message sensorMsg = mHandler.obtainMessage();
                    if (mSensor.getmO() != null && mSensor.getmG() != null
                            && mSensor.getmS() != null) {
                        LogMgr.e(" " + mSensor.getmO()[0] + "  " + mSensor.getmO()[1] + " " + mSensor.getmO()[2]);
                        LogMgr.e(" " + mSensor.getmG()[0] + "  " + mSensor.getmG()[1] + " " + mSensor.getmG()[2]);
                        LogMgr.e(" " + mSensor.getmS()[0] + "  " + mSensor.getmS()[1] + " " + mSensor.getmS()[2]);
                        byte[] datas = Utils.byteMerger(Utils.byteMerger(
                                Utils.floatsToByte(mSensor.getmO()),
                                Utils.floatsToByte(mSensor.getmG())),
                                Utils.floatsToByte(mSensor.getmS()));
                        byte[] responseBytes = ProtocolBuilder.buildProtocol(
                                (byte) ControlInitiator.ROBOT_TYPE_C1_2,
                                ProtocolBuilder.CMD_GYRO, datas);
                        LogMgr.e(" 回陀螺仪数值" + Utils.bytesToString(responseBytes));
                        sensorMsg.obj = responseBytes;
                        mHandler.sendMessage(sensorMsg);
                        LogMgr.d("返回陀螺仪数值");
                    }
                } else if (data[5] == (byte) 0x20 && data[6] == (byte) 0x04) {
                    byte[] protocolbytes = ProtocolBuilder.getData(data);
                    byte isPlay = protocolbytes[0];
                    if (isPlay == 0) {
                        byte[] musicNameByes = new byte[protocolbytes.length - 1];
                        System.arraycopy(protocolbytes, 1, musicNameByes, 0,
                                protocolbytes.length - 1);
                        String musicName = new String(musicNameByes);
                        LogMgr.d("播放音频：" + musicName);
                        mPlayer.play(musicName);
                    } else {
                        LogMgr.d("停止播放");
                        mPlayer.stop();
                    }
                } else {
                    byte[] buffer = SP.request(data);
                    if(buffer==null){
                        return;
                    }
                    LogMgr.d("serial prot response::"
                            + Utils.bytesToString(buffer));
                    Message serialResponseMsg = mHandler.obtainMessage();
                    serialResponseMsg.obj = buffer;
                    mHandler.sendMessage(serialResponseMsg);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("dispose cmd error::" + e);
        }
    }

    @Override
    public void stopDisposeProtocol() {
        super.stopDisposeProtocol();
    }
}
