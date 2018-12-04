package com.abilix.control.pad;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.balancecar.BalanceCarData;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.security.InvalidParameterException;

import static com.abilix.control.sp.SP.request;

public class S5ProtocolDisposer extends AbstractProtocolDisposer {
    // 测试
    private boolean isStart;
    /**
     * 间隔时间20ms
     */
    private static int INTERVAL_TIME = 20;

    /**
     * 每组动作之前的间隔时间
     */
    private static double TIME_1 = 0.616;
    private static double TIME_2 = 0.336;
    private static double TIME_3 = 0.648;
    private static double TIME_4 = 0.336;
    private BalanceCarData balancecarData = null;
//	private SerialPortCommunicator mSerial;

    public S5ProtocolDisposer(Handler handler) {
        super(handler);
        //	mSerial = SerialPortCommunicator.getInstance();
    }

    @Override
    public void DisposeProtocol(Message msg) {
        LogMgr.e("receive message");
        byte[] data = (byte[]) msg.obj;
        LogMgr.d("pad cmd::" + Utils.bytesToString(data));
        try {
            if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55) {
                /*if (data[5] == (byte) 0x08 && data[6] == (byte) 0x04) {
                        //这块代码是谁写的，S没有平衡车，怎么这还把平衡车的代码搞进来了？
                    balancecarData = BalanceCarData.GetManger(mHandler, 1);
                    balancecarData.SetBalanceCar((data[11] & 0xff), (data[12] & 0xff), (data[13] & 0xff));
                } else if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x08
                        && data[6] == (byte) 0x05) {
                    balancecarData = BalanceCarData.GetManger(mHandler, 1);
                    balancecarData.InitBalanceCar((data[11] & 0xff), (data[12] & 0xff));
                } else */
                if (data[5] == (byte) 0x20 && data[6] == (byte) 0x03) {
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
                } else if (data[5] == (byte) 0x20 && data[6] == (byte) 0x04) {
                    byte[] protocolbytes = ProtocolBuilder.getData(data);
                    byte isPlay = protocolbytes[0];
                    LogMgr.d("protocolbytes = " + Utils.bytesToString(protocolbytes));
                    if (isPlay == (byte) 0x00) {
                        byte[] musicNameByes = new byte[protocolbytes.length - 1];
                        System.arraycopy(protocolbytes, 1, musicNameByes, 0, protocolbytes.length - 1);
                        String musicName = new String(musicNameByes);

                        LogMgr.d("播放音频：" + musicName);
                        mPlayer.play(musicName);
                    } else {
                        LogMgr.d("停止播放");
                        mPlayer.stop();
                    }
                } else if (data[5] == (byte) 0x21 && data[6] == (byte) 0x02) {//有问题,暂时先转换一下，协议
                    //之前走的是老协议，pad端获取老协议进行获取数值，
                    byte[] buffer = ProtocolBuilder.sendProtocol((byte) 0x05,new byte[]{(byte) 0xA3, (byte) 0xA3},null,true);
                    byte[] rebuffer = new byte[]{(byte) 0xFE, 0x68, (byte) 0xDA, 00, 00, 07, (byte) 0xFF, (byte) 0xFF, (byte) 0xC8, 04, 00, 00, 00, (byte) 0xAA, 0x16};
                    LogMgr.d("超声  serial prot response::" + Utils.bytesToString(buffer));
                    Message serialResponseMsg = mHandler.obtainMessage();
                    if(buffer[5] == (byte)0xF0
                            && buffer[6] == (byte)0xA0){
                        rebuffer[11] = buffer[12];
                        rebuffer[12] = buffer[11];
//                        LogMgr.d("超声 rebuffer  serial prot response::" + Utils.bytesToString(rebuffer, rebuffer.length));
                        serialResponseMsg.obj = rebuffer;
                    }else {
                        serialResponseMsg.obj = rebuffer;
                    }
                    mHandler.sendMessage(serialResponseMsg);
                } else if (data[5] == (byte) 0x21 && data[6] == (byte) 0x04) {//有问题,协议置灰，是否要修改？
                    byte[] buffer =  ProtocolBuilder.sendProtocol((byte) 0x05,new byte[]{(byte) 0xA3, (byte) 0xA1},new byte[]{data[11],data[13],data[14]},true);
                    LogMgr.d("舵机" + Utils.bytesToString(buffer));
                } else if (data[5] == GlobalConfig.S_PROGRAM_PROJECT_IN_CMD_1 && data[6] == GlobalConfig
                        .S_PROGRAM_PROJECT_IN_CMD_2_PLAY_LIST) {
                    LogMgr.i("S项目编程 播放动作列表");
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
                    PlayMoveOrSoundUtils.getInstance().handlePlayList(moveNameArray);

                } else if ((data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_STOP) ||
                        (data[5] == GlobalConfig.S_PROGRAM_PROJECT_IN_CMD_1 && data[6] == GlobalConfig
                                .S_PROGRAM_PROJECT_IN_CMD_2_STOP_LIST)) {
                    // 停止播放命令
                    LogMgr.v("收到播放停止命令");
                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                    return;
                } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == GlobalConfig.PLAY_IN_CMD_2_PLAY) {
                    isStart = true;
                    send();
                    // 执行动作
                    String filePath =Environment.getExternalStorageDirectory().getPath() + File.separator + "S5_D_Walk005.bin";
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(filePath,
                            null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                } else if (data[5] == GlobalConfig.PLAY_IN_CMD_1 && data[6] == (byte) 0x07) {
                    isStart = false;
                    // 停止执行动作
                    PlayMoveOrSoundUtils.getInstance().forceStop(false);
                } else if (data[5] == (byte) 0x08 && data[6] == (byte) 0x06) {
                    // TODO 批量播放s系列动作文件


                } else if (data[5] == GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1 && data[6] == GlobalConfig
                        .ENGINE_ANGLE_FIRMWARE_OUT_CMD_2) {
                    //S系列获取所有舵机角度
                    LogMgr.v("S系列获取所有舵机角度命令");
//                    mCmdType = CMD_TYPE_NEED_RETURN_AND_SERIAL_0;
                    request(data);
                    Thread.sleep(500);
                    LogMgr.d("当前是认识机器人获取所有舵机角度的命令");
                    byte[] buffer = new byte[56];
                    SP.write(buffer);
//                    LogMgr.v("所有舵机角度的命令 获取到的数据1 = " + Utils.bytesToString(buffer, buffer.length));
                    if (buffer[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1 && buffer[6] == GlobalConfig.ENGINE_ANGLE_FIRMWARE_IN_CMD_2) {
                        Message msg_engine = mHandler.obtainMessage();
                        msg_engine.obj = buffer;
                        mHandler.sendMessage(msg_engine);
                    }
                } else if (data[5] == (byte) 0x06 && data[6] == (byte) 0x02) {
                    LogMgr.d("释放S舵机命令");
                    ProtocolUtils.engineStateChangeAll(ProtocolUtils.ENGINE_STATE_RELEASE);

//                } else if (data[5] == (byte) 0x11 && data[6] == (byte) 0x0F) {
//                    byte[] buffer=SP.request(data);
//                    if(buffer==null){
//                        return;
//                    }
//                    LogMgr.d("serial prot response::" + Utils.bytesToString(buffer, buffer.length));
//                    Message serialResponseMsg = mHandler.obtainMessage();
//                    serialResponseMsg.obj = buffer;
//                    mHandler.sendMessage(serialResponseMsg);
                } else if(data[5] == (byte) 0xA3 && data[6] == (byte) 0xAD){//扫描舵机，因为时间大概200，所以暂时单独列出来；
                    byte[] buffer=SP.request(data,1000);
                    if(buffer==null){
                        return;
                    }
                    LogMgr.d("serial prot response::" + Utils.bytesToString(buffer));
                    Message serialResponseMsg = mHandler.obtainMessage();
                    serialResponseMsg.obj = buffer;
                    mHandler.sendMessage(serialResponseMsg);
                }else {
                    byte[] buffer=SP.request(data,20);
                    if(buffer==null){
                        return;
                    }
                    LogMgr.d("serial prot response::" + Utils.bytesToString(buffer));
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

    /**
     * 发送指令
     */
    private void send() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                computTime(0, 0, 0, 0, 0, 0, 0, 0, 28.1, 12.29, -72.95, -3.8, -4.97, -2.64, -12.01, 48.93, TIME_1);
                while (isStart) {
                    if (isStart) {
                        computTime(28.1, 12.29, -72.95, -3.8, -4.97, -2.64, -12.01, 48.93, -7.9, -2.93, -31.64, 14.94,
                                14.36, -30.18, 12.29, 6.15, TIME_2);
                    }
                    if (isStart) {
                        computTime(-7.9, -2.93, -31.64, 14.94, 14.36, -30.18, 12.29, 6.15, -10.54, 11.12, -9.96, 47.46,
                                9.96, -49.8, 4.97, 5.57, TIME_3);
                    }
                    if (isStart) {
                        computTime(-10.54, 11.12, -9.96, 47.46, 9.96, -49.8, 4.97, 5.57, 18.46, 3.51, -35.16, 50.68,
                                -9.66, -5.57, -12.01, 26.07, TIME_4);
                    }
                    if (isStart) {
                        computTime(18.46, 3.51, -35.16, 50.68, -9.66, -5.57, -12.01, 26.07, 28.1, 12.29, -72.95, -3.8,
                                -4.97, -2.64, -12.01, 48.93, TIME_1);
                    }
                }
            }
        }).start();
    }

    /**
     * 计算时间
     *
     * @param one
     * @param two
     * @param three
     * @param four
     * @param five
     * @param six
     * @param seven
     * @param eight
     * @param one2
     * @param two2
     * @param three2
     * @param four2
     * @param five2
     * @param six2
     * @param seven2
     * @param eight2
     * @param time
     */
    private void computTime(double one, double two, double three, double four, double five, double six, double seven,
                            double eight, double one2, double two2, double three2, double four2, double five2, double
                                    six2,
                            double seven2, double eight2, double time) {
        int i = (int) (time * 1000 / INTERVAL_TIME);
        for (int j = 0; j < i; j++) {
            double one1 = one + j * (one2 - one) / i;
            double two1 = two + j * (two2 - two) / i;
            double three1 = three + j * (three2 - three) / i;
            double four1 = four + j * (four2 - four) / i;
            double five1 = five + j * (five2 - five) / i;
            double six1 = six + j * (six2 - six) / i;
            double seven1 = seven + j * (seven2 - seven) / i;
            double eight1 = eight + j * (eight2 - eight) / i;
            sendOrder(one1, two1, three1, four1, five1, six1, seven1, eight1);
        }
    }

    /**
     * 发送指令
     */
    private void sendOrder(double one, double two, double three, double four, double five, double six, double seven,
                           double eight) {
        try {
            byte[] buffer = new byte[24];
            buffer[0] = (byte) 0x01;
            byte[] bs = intToByte(angleSwitch(one));
            System.arraycopy(bs, 0, buffer, 1, bs.length);
            buffer[3] = (byte) 0x02;
            byte[] bs1 = intToByte(angleSwitch(two));
            System.arraycopy(bs1, 0, buffer, 4, bs1.length);
            buffer[6] = (byte) 0x03;
            byte[] bs2 = intToByte(angleSwitch(three));
            System.arraycopy(bs2, 0, buffer, 7, bs2.length);
            buffer[9] = (byte) 0x04;
            byte[] bs3 = intToByte(angleSwitch(four));
            System.arraycopy(bs3, 0, buffer, 10, bs3.length);
            buffer[12] = (byte) 0x05;
            byte[] bs4 = intToByte(angleSwitch(five));
            System.arraycopy(bs4, 0, buffer, 13, bs4.length);
            buffer[15] = (byte) 0x06;
            byte[] bs5 = intToByte(angleSwitch(six));
            System.arraycopy(bs5, 0, buffer, 16, bs5.length);
            buffer[18] = (byte) 0x07;
            byte[] bs6 = intToByte(angleSwitch(seven));
            System.arraycopy(bs6, 0, buffer, 19, bs6.length);
            buffer[21] = (byte) 0x08;
            byte[] bs7 = intToByte(angleSwitch(eight));
            System.arraycopy(bs7, 0, buffer, 22, bs7.length);
            byte[] bss = buildProtocol(buffer);
            // SerialPort port =
            // SRobotApplication.getApplication().getSSerialPort();
            // port.getOutputStream().writeS(bss);
            SP.write(bss);
            Thread.sleep(20);
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param d
     * @return
     */
    private int angleSwitch(double d) {
        return (int) d * 1024 / 300 + 512;
    }

    /**
     * 包封装
     *
     * @param data
     */
    private byte[] buildProtocol(byte[] data) {
        // 校验位
        byte[] bs = addProtocol(data);
        byte[] bs2 = new byte[bs.length + 2];
        bs2[0] = (byte) 0xfe;
        bs2[1] = (byte) (bs.length + 1);
        System.arraycopy(bs, 0, bs2, 2, bs.length);
        byte check = 0x00; // 校验位
        for (int n = 0; n < bs2.length; n++) {
            check += bs2[n];
        }

        byte[] bs3 = new byte[bs2.length + 3];
        bs3[0] = (byte) 0xff;
        bs3[1] = (byte) 0xff;
        System.arraycopy(bs2, 0, bs3, 2, bs2.length);
        bs3[bs3.length - 1] = (byte) ~check;

        byte[] length = intToByteHigh(bs3.length);

        // 所有数据
        byte[] bs4 = new byte[bs3.length + 8];
        bs4[0] = (byte) 0xfe;
        bs4[1] = (byte) 0x68;
        bs4[2] = (byte) 0x5a;
        bs4[3] = (byte) 0x00;
        bs4[bs4.length - 2] = (byte) 0xaa;
        bs4[bs4.length - 1] = (byte) 0x16;
        System.arraycopy(length, 0, bs4, 4, length.length);
        System.arraycopy(bs3, 0, bs4, 6, bs3.length);
        return bs4;
    }

    /**
     * 封装舵机指令
     *
     * @param data
     */
    private byte[] addProtocol(byte[] data) {
        byte[] bs = new byte[data.length + 3];
        bs[0] = (byte) 0x83;
        bs[1] = (byte) 0x1e;
        bs[2] = (byte) 0x02;
        System.arraycopy(data, 0, bs, 3, data.length);
        return bs;
    }

    /**
     * int 转byte 低在前高在后
     *
     * @param value
     */
    private byte[] intToByte(int value) {
        byte[] bs = new byte[2];
        bs[1] = (byte) ((value >> 8) & 0xff);
        bs[0] = (byte) (value & 0xff);
        return bs;
    }

    /**
     * byte转int 低在前高在后
     *
     * @param bs
     */
    private int byteToInt(byte[] bs) {
        int value = (bs[0] & 0xff) | ((bs[1] & 0xff) << 8);
        return value;
    }

    /**
     * int 转byte 高在前低在后
     *
     * @param value
     */
    private byte[] intToByteHigh(int value) {
        byte[] bs = new byte[2];
        bs[0] = (byte) ((value >> 8) & 0xff);
        bs[1] = (byte) (value & 0xff);
        return bs;
    }

    @Override
    public void stopDisposeProtocol() {
        super.stopDisposeProtocol();
    }
}
