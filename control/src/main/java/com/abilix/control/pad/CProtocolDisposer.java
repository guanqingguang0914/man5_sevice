package com.abilix.control.pad;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.abilix.control.ControlInfo;
import com.abilix.control.balancecar.BalanceCarData;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.balancecar.Mypoint;
import com.abilix.control.model.Model;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.PushMsg;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import static com.abilix.control.protocol.ProtocolSender.sendProtocol;

public class CProtocolDisposer extends AbstractProtocolDisposer {
    private BalanceCarData balancecarData = null;
    private int V = 30;
    private int Vleft = 0;
    private float offset = 0;
    private int Vright = 0;
    private Thread sendThread;
    // 启动停止线程，以及关闭电机。这里包含一个总的关闭方式。
    private float yaw = 0;
    private boolean ai_flag = false;

    public CProtocolDisposer(Handler handler) {
        super(handler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        LogMgr.e("receive message");
        byte[] data = (byte[]) msg.obj;
        LogMgr.d("pad cmd::" + Utils.bytesToString(data));

        try {
            if ((data[0] == 'C' && data[2] == 'E' && data[3] == 'T' && data[19] == 'O') || (data[0] == (byte) 0xAA && data[1] == (byte) 0x55)) {
//				LogMgr.d("pad query serial port response  "+data[5]+"   "+data[6]+"   "+data[11]);
                //测试UDP命令是否是都收到了
                if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x08 && data[6] == (byte) 0x04) {
                    balancecarData = BalanceCarData.GetManger(mHandler, 1);
                    balancecarData.SetBalanceCar((data[11] & 0xff), (data[12] & 0xff), (data[13] & 0xff));
                } else if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x08 && data[6] == (byte) 0x05) {
                    balancecarData = BalanceCarData.GetManger(mHandler, 2);
                    balancecarData.InitBalanceCar((data[11] & 0xff), (data[12] & 0xff));
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
                } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_IN_CMD_2_PLAY_SOUND) {
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
                } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == GlobalConfig.KNOW_ROBOT_IN_CMD_2_GYRO) {
                    byte[] data_gyro = Utils.byteMerger(
                            Utils.byteMerger(Utils.floatsToByte(mSensor.getmO()),
                                    Utils.floatsToByte(mSensor.getmG())), Utils.floatsToByte(mSensor.getmS()));
                    byte[] returnDataGyro = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                            GlobalConfig.KNOW_ROBOT_OUT_CMD_1, GlobalConfig.KNOW_ROBOT_OUT_CMD_2_GYRO, data_gyro);
                    Message messageGyro = mHandler.obtainMessage();
                    messageGyro.obj = returnDataGyro;
                    mHandler.sendMessage(messageGyro);
                } else if (ProtocolUtils.isModelCmd(data)) {
                    LogMgr.d("收到模型命令");
                    if (ProtocolUtils.isModelTypeNotifyCmd(data)) {
                        LogMgr.d("收到模型类型通知命令");
                        Model.initOrDestroyInstance(mHandler, data[12]);
                    } else if (ProtocolUtils.isModelMoveCmd(data)) {
                        LogMgr.d("收到模型移动命令");
                        Model.getInstance().move(data[12], (int) data[13]);
                    } else if (ProtocolUtils.isModelFunctionCmd(data)) {
                        LogMgr.d("收到模型功能开关命令");
                        boolean onOrOff;
                        if (data[13] == Model.FUNCTION_OFF) {
                            onOrOff = false;
                        } else if (data[13] == Model.FUNCTION_ON) {
                            onOrOff = true;
                        } else {
                            LogMgr.e("参数错误");
                            return;
                        }
                        Model.getInstance().function(data[12], onOrOff);
                    } else if (ProtocolUtils.isModelActionCmd(data)) {
                        LogMgr.d("收到模型功能动作命令");
                        Model.getInstance().action(data[12], null);
                    }
                } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == (byte) 0x0A) {//平衡车开始
                    LogMgr.e("start()");
                    start();
                } else if (data[5] == GlobalConfig.KNOW_ROBOT_IN_CMD_1 && data[6] == (byte) 0x0B) {//平衡车停止
                    LogMgr.e("stop()");
                    stop();
                } else {
                    //平衡车状态主动上报控制
                     if (data[5] == (byte) 0xA3 && data[6] == (byte) 0x2E) {
                         switch (data[12]) {
                             case 0x00:
                                 LogMgr.d("平衡车状态主动上报反注册");
                                 SP.unRegisterPushEvent(pushMsg);
                                 break;
                             case 0x01:
                                 LogMgr.d("平衡车状态主动上报注册");
                                 SP.registerPushEvent(pushMsg);
                                 break;
                         }
                    }
                    byte[] buffer = SP.request(data, 100);
                    if (buffer == null) {
                        return;
                    }
                    Message sensorMsg = mHandler.obtainMessage();
                    if (data[0] == 'C' && data[2] == 'E' && data[3] == 'T') { // 老协议
                        sensorMsg.what = 0;
                        if (mSensor.getmO() != null && mSensor.getmG() != null && mSensor.getmS() != null) {
                            byte[] datas = Utils.byteMerger(
                                    Utils.byteMerger(Utils.floatsToByte(mSensor.getmO()),
                                            Utils.floatsToByte(mSensor.getmG())), Utils.floatsToByte(mSensor.getmS()));
                            byte[] buf = new byte[40];
                            System.arraycopy(buffer, 0, buf, 0, buffer.length);
                            byte[] send_buff = Utils.byteMerger(buf, datas);
                            sensorMsg.obj = send_buff;
                        } else {
                            sensorMsg.obj = buffer;
                        }
                        // LogMgr.d("response pad cmd to brain::"+Utils.bytesToString(response, response.length));
                    } else { // 新协议 lz 2017-6-7 10:41:47添加
                        sensorMsg.what = 1;
                        sensorMsg.obj = buffer;
                    }
                    mHandler.sendMessage(sensorMsg);
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
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("dispose cmd error::" + e);
        }
    }

    private void stop() {
        ai_flag = false;
        sendThread = null;
        LogMgr.e("ai_flag = " + ai_flag);
        //balance();
        stopbal();
    }

    private PushMsg pushMsg = new PushMsg() {
        @Override
        public void onPush(byte[] pushData) {
            super.onPush(pushData);
            LogMgr.i("收到平衡上报信息");
            Message message = mHandler.obtainMessage();
            message.obj = pushData;
            mHandler.sendMessage(message);
        }
    };

    private void start() {
        try {
            //第一步取出数据。
            wheelset(V, V);//先发一个0xoA.
            ai_flag = true;
            ai_num();
            Thread.sleep(100);//线程等待100ms.
            offset = yaw;
            byte[] data = toByteArray("line.bin");
            byte[] length = new byte[4];
            System.arraycopy(data, 2, length, 0, 4);
            int len = Utils.byteAray2IntLH(length);
            //第三步获取数据。
            ArrayList<Mypoint> list1 = getpointlist(data, len);
            //第四步过滤数据。
            ArrayList<Mypoint> list2 = getpointFilter(list1);
            float[] angle = drection(list2);

            //这里处理下方位问题。
            float[] angle2 = dealangle(angle, offset);
            //第五步发送数据。
            send(angle2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] dealangle(float[] angle, float offset) {
        //偏移量。
        offset = getyaw(offset, angle[0]);
        for (int i = 0; i < angle.length; i++) {
            angle[i] = (angle[i] + offset + 360) % 360;
        }
        return angle;
    }

    private void send(final float[] data) {
        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {

                int i = 0;
                while (ai_flag && i < data.length) {
                    int j = 0;
                    while (ai_flag && j < 8) {
                        ai_num();
                        //第一步拿到钥值。
                        //float d_yaw=yaw-data[i];
                        float d_yaw = getyaw(yaw, data[i]);
                        //假设相差400度 速度相差25.则比例系数为1/16；
                        float kp = (float) 1 / 8;
                        //那么偏差为：
                        float round = kp * d_yaw;
                        //目前只有P调节。
                        Vleft = (int) (V + round);
                        Vright = (int) (V - round);
                        wheelset(Vleft, Vright);
                        long time = System.currentTimeMillis();
                        while (ai_flag && System.currentTimeMillis() - time < 30) {
                        }
                        j++;
                    }
                    i++;
                }
                stopbal();
                LogMgr.e("划线bin执行完毕！");
            }
        });
        sendThread.start();
    }

    private void stopbal() {
        byte[] data = new byte[7];
        data[0] = 0x05;
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data);
    }

    private void balance() {
        // 默认自平衡。
        byte[] data = new byte[4];
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data);
    }

    private float getyaw(float yao, float angle) {
        float d_yaw = yao - angle;
        if (d_yaw > 180) {
            d_yaw = d_yaw - 360;
        } else if (d_yaw < -180) {
            d_yaw = d_yaw + 360;
        }
        return d_yaw;
    }

    private float[] drection(ArrayList<Mypoint> pointlist) {
        float[] data = new float[pointlist.size() - 1];//方向比点少一个。
        for (int i = 0; i < pointlist.size() - 1; i++) {
            //求两点的向量。
            float angle = 0;
            int x = pointlist.get(i + 1).getX() - pointlist.get(i).getX();
            int y = pointlist.get(i + 1).getY() - pointlist.get(i).getY();
            //向量分方向。
            if (y >= 0) {//一二象限合并。
                angle = (float) Math.acos((float) x / Math.sqrt(x * x + y * y));
            } else if (x <= 0) { //第三象限。
                angle = (float) (Math.PI + (float) Math.acos((float) (-x) / Math.sqrt(x * x + y * y)));
            } else if (x > 0) { //第4象限。
                angle = (float) (2 * Math.PI - (float) Math.acos((float) x / Math.sqrt(x * x + y * y)));
            }
            data[i] = (float) (angle * 57.3);
        }
        return data;
    }

    private ArrayList<Mypoint> getpointFilter(ArrayList<Mypoint> pointlist1) {
        // 目前只做简单的过滤，小于25的过滤掉，倍数补点。
        int devide = 1600;// 25*25 阈值
        ArrayList<Mypoint> pointlist2 = new ArrayList<Mypoint>();
        for (int i = 0; i < pointlist1.size(); i++) {

            // 第一条数据无条件赋值。
            if (i == 0) {
                pointlist2.add(pointlist1.get(0));
            }
            int X1 = pointlist1.get(i).getX();
            int Y1 = pointlist1.get(i).getY();
            // 取最后一个值。
            int X2 = pointlist2.get(pointlist2.size() - 1).getX();
            int Y2 = pointlist2.get(pointlist2.size() - 1).getY();
            int dis = (X1 - X2) * (X1 - X2) + (Y1 - Y2) * (Y1 - Y2);
            if (dis > devide) {
                int num = (int) Math.sqrt(dis / devide);
                for (int j = 1; j <= num; j++) {
                    int x = (int) (X2 + (float) j / num * (X1 - X2));
                    int y = (int) (Y2 + (float) j / num * (Y1 - Y2));
                    Mypoint mypoint = new Mypoint(x, y);
                    pointlist2.add(mypoint);
                }
            }
        }
        return pointlist2;
    }

    private ArrayList<Mypoint> getpointlist(byte[] data, int length) {
        ArrayList<Mypoint> pointlistInit = new ArrayList<Mypoint>();
        byte[] bindata = new byte[length];
        System.arraycopy(data, 6, bindata, 0, length);
        // 一个点4个字节。
        for (int i = 0; i < length - 3; i += 4) {
            int x = Utils.byte2int_2byteLH(bindata, i);
            int y = Utils.byte2int_2byteLH(bindata, i + 2);
            Mypoint mypoint = new Mypoint(x, y);
            pointlistInit.add(mypoint);
        }
        return pointlistInit;
    }

    private byte[] toByteArray(String filename) throws Exception {
        filename = GlobalConfig.KNOW_ROBOT_PATH + File.separator + filename;
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while ((len = in.read(buffer, 0, buf_size)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }

    private void ai_num() {

        try {
            byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x2C, null);
            byte[] ai_receive = SP.request(cmd);
            if (ai_receive == null) {
                LogMgr.e("SP.request Error! null");
            }
            /*aa 55 00 0c 01 f0 2b 00 00 00 00 f4 a2 26 43 26*/
            if (ai_receive != null && ai_receive.length >= 14 && ai_receive[5] == (byte) 0xf0 && ai_receive[6] == (byte) 0x2B) {
                yaw = Utils.byte2float(ai_receive, 11) + 180;
                LogMgr.e("yaw = " + yaw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wheelset(int left, int right) {
        LogMgr.d("wheelset: [" + left + ", " + right + "]");
        byte[] data = new byte[7];
        data[0] = 0x0A;// 左右轮速度控制。
        data[1] = 0x00;// 无超声控制。
        left = left + 100;
        if (left > 200) {
            left = 200;
        }
        right = right + 100;
        if (right > 200) {
            right = 200;
        }
        data[2] = (byte) left;// 0~200
        data[3] = (byte) right;// 0~200.
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data);
    }

    @Override
    public void stopDisposeProtocol() {
        if (BalanceCarData.balancecarData != null) {
            BalanceCarData.getOut();
        }
        if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_CU) {
            stop();
            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < 1300) {
            }
        }
        super.stopDisposeProtocol();
    }
}
