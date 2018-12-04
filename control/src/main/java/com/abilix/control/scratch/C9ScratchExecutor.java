package com.abilix.control.scratch;

import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.IPlayStateListener;


import java.io.File;
import java.util.Arrays;

/**
 * Created by guanqg on 2017/7/10.
 */

public class C9ScratchExecutor extends AbstractScratchExecutor {
    protected MySensor mSensor;
    //这里定义一个时间戳，防止拍照录音等无限循环卡死。
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;
    //麦克风录音时间记录。
    private int timeInit = 0;
    // 这里还是要记忆的。
    private int dc0speed = 100;
    private int dc1speed = 100;
    private int dc2speed = 100;
    private int dc3speed = 100;
    private int dc4speed = 100;
    private int dc5speed = 100;
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int toPad = 0;
    private final int toStm32 = 1;
    //这里是播放录音文件路径。
    public final static String DATA_PATH = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "Abilix"
            + File.separator + "RobotInfo" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_ = DATA_PATH + "Photo" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_JPG = ".jpg";
    public static final String RECORD_SCRATCH_VJC_ = FileUtils.DATA_PATH
            + File.separator + "Record" + File.separator;
    public static final String RECORD_PATH_3GP = ".3gp";

    // 这里先这么定义
    private final int display_String_mode = 12;
    private final int displayClose = 2;

    public C9ScratchExecutor(Handler mHandler) {
        super(mHandler);
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come Scratch execute " + Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private void checkFunction(final byte[] data) {
        int parms1 = -1, parms2 = -1, parms3 = -1, parms4 = -1, parms5 = -1, parms6 = -1;
        //byte[] array = new byte[4];//用于复制数据的临时buff
        if (data[4] != 0x0A) {
            // C9协议
            LogMgr.e("协议类型错误");
            return;
        }
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[5]){
            case 0x11:
                stopScratch();
                break;
            default:
                switch (data[6]){
                    case 0x01://启动电机
                        parms1 = getparms(data,1);
                        parms2 = getparms(data,2);
                        parms3 = getparms(data,3);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2 + "  parms3: " + parms3);
                        motorSet(parms1, parms2, parms3);
                        break;
                    case 0x02:
                        //启动扬声器
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        playMusic(parms1, parms2);
                        break;
                    case 0x03:// 启动LED 第一个参数为颜色：0~2("红","绿","蓝")
                        parms1 = getparms(data, 1);
                        ledSet(parms1);
                        break;
                    case 0x04:// 启动显示
                        // 第一个参数为类型：0~1("字符","照片")，（1）当第一个参数为"字符"时，第二个参数为字符串数组（最大19个字节）（2）当第一个参数为"照片"时，第二个参数为0~9
                        display(data);
                        break;
                    case 0x05:// 关闭 第一个参数为类型：0~3("电机","扬声器","LED","显示")           ------------还有关闭显示
                        parms1 = getparms(data, 1);
                        close(parms1);
                        break;
                    case 0x06:// 超声探测到障碍物 第一个参数为端口：0~7                          ----------------------ok
                        parms1 = getparms(data, 1);   //这里改为4个字节。 先测试下端口，有问题。
                        LogMgr.i("端口: " + parms1);
                        haveObject(parms1);
                        break;
                    case 0x07:// 超声探测距离 第一个参数为端口：0~7                        --------------------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.i("端口: " + parms1);
                        distance(parms1);
                        break;
                    case 0x08:// 碰到物体 第一个参数为端口：0~7                             ------------------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.e("端口: " + parms1);
                        touch(parms1);
                        break;
                    case 0x09:// 识别颜色
                        // 第一个参数为端口：0~7，第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）-----------ok
                        parms1 = getparms(data, 1);
//                        parms2 = getparms(data, 2);
                        LogMgr.e("颜色: " + parms1);
                        readColor(parms1);
//                        findColorC(parms1);
                        break;
                    case 0x0A:// 探测灰度值 第一个参数为端口：0~7                        --------- -----------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.i("端口: " + parms1);
                        getGraySensor(parms1);
                        break;
                    case 0x0B:// 摄像头拍照 第一个参数为：0~9 这个暂时不做
                        //类似录音，加一个时间戳。
                        pic_pre = System.currentTimeMillis();
                        if (pic_pre - pic_next > 3500) {
                            pic_next = pic_pre;

                            parms1 = getparms(data, 1);
                            takepicture(parms1);
                        }
                        break;
                    case 0x0C:// 时钟复位 不做。\
                    case 0x0D:
                    case 0x0E:
                        ResponseZero();
                        break;
                    case 0x0F:// 指南针探测角度
                        getCompass();
                        break;
                    case 0x10:// 陀螺仪探测 第一个参数为类型：0~3("下俯","后仰","左翻","右翻")  ----------------OK
                        parms1 = getparms(data, 1);
                        getPosition(parms1);
                        break;
                    case 0x11:// 麦克风录音 第一个参数为：0~9，第二个参数为时间：1~60         ----------------OK

                        //用一个时间戳来限制。
                        parms1 = getparms(data, 1);// 第一个参数应该是保存9个音频
                        parms2 = getparms(data, 2);// 第二个参数是时间。
                        LogMgr.e("time: " + parms2);
                        if (parms2 > 60) {
                            parms2 = 60;
                        }
                        //Record_pre - record_next >= 1000  两次指令之间最小时间戳，不然brain会死掉 所以用 &&
                        //Record_pre - record_next >= timeInit * 1000  这里保证上次录音完成。
                        Record_pre = System.currentTimeMillis();
                        if (Record_pre - record_next >= 1000
                                && Record_pre - record_next >= timeInit * 1000) {
                            record_next = Record_pre;
                            timeInit = parms2;
                            if (parms2 >= 1) { // 录音时间小于0都不处理。
                                RecordVoice(parms1, parms2);
                            }
                        }
                        break;
                    case 0x12://获取磁敏;底层没有该协议，暂时不写
                        parms1 = getparms(data, 1);
                        LogMgr.e("端口: " + parms1);
//                        cimin(parms1);
                        getMagneticC9(parms1);
                        break;
                    case 0x13://设置彩灯
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);//mode
                        byte[] pin = new byte[4];
                        System.arraycopy(data, 23, pin, 0, 4);
                        byte[] rgb = Arrays.copyOfRange(data, 28, 31);
                        float frequency =  Utils.byte2float(pin, 0);
                        LogMgr.e("parms1: " + parms1 + ";  parms2: " + parms2 + ";;pin = " + Utils.bytesToString(pin)+";frequency = " + frequency);
                        setColed(parms1,parms2,frequency,rgb);
                        break;
                    case 0x14:
                        int length = (int) (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
                        byte[] name = new byte[length - 12];
                        System.arraycopy(data, 15, name, 0, name.length);
                        String playName = null;
                        try {
                            int k = 0;
                            while (k < name.length && name[k] != 0) {
                                k++;
                            }
                            byte[] nameBytes = Arrays.copyOfRange(name, 0, k);
                            playName = new String(nameBytes, "utf-8");
                            final int isNeedWaiting;
                            if (data.length > 34) {
                                isNeedWaiting = data[34] & 0xFF;
                            } else {
                                isNeedWaiting = 0;
                            }
                            IPlayStateListener iPlayStateListener = new IPlayStateListener() {
                                @Override
                                public void onFinished() {
                                    if (isNeedWaiting == 1) {
                                        byte[] sendArray = new byte[8];
                                        System.arraycopy(id, 0, sendArray, 0, id.length);
                                        System.arraycopy(intToByte(1), 0, sendArray, 4, 4);
                                        for (int i = 0; i < 3; i++) {
                                            sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
                                        }
                                    }
                                }
                            };
                            if (playName.contains("luyin")) {
                                String[] play = playName.split("p");
                                String num = play[1];
                                LogMgr.d("play = " + playName + ";num = " + num);
                                File mfile = new File(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP);
                                if (mfile.exists()) {
                                    //SendScratchToBrain(5, display_String_mode, sendnum);
                                    mPlayer.playRecord(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP, iPlayStateListener);
                                }
                            } else {
                                playName += ".mp3";
                                LogMgr.d("paly short music playName = " + playName);
                                mPlayer.play(playName, iPlayStateListener);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0x15://火焰传感器 、第一个参数为端口：0~7 (0为自动)
                        parms1 = getparms(data, 1);
                        LogMgr.i("火焰端口: " + parms1);
                        fireTest(parms1);
                        break;
                    case 0x16://温度传感器、第一个参数为端口：0~7 (0为自动)
                        parms1 = getparms(data, 1);
                        LogMgr.i("温度传感器: " + parms1);
                        tempTest(parms1);
                        break;
                    case 0x17://设置智能电机、第一个参数：方向 0：正转；1：反转。第二个参数：电机角度（0~90）
                        //0xA4 0x53(设置智能电机),参数个数：1字节;参数1:2字节 电机角度;参数2:2字节 电机速度
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        LogMgr.i("parms1 = " + parms1 + "; parms2 = "+ parms2 + "; parms3 = "+ parms3);
                        //setSMotor(parms1,parms2);
                        setSmartMotor(parms1, parms3, parms2);
                        break;
                    case 0x18://电磁铁、第一个参数为端口：0~7 (0为自动)、第二个参数：1-打开，0-关闭；
                        //0xA4 0x54(设置电磁铁),参数个数：1字节;参数1：1字节 端口号 ;参数2：1字节 0：关闭；1：打开。
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.i("parms1 = " + parms1+";parms2 = "+ parms2);
                        setCiTie(parms1,parms2);
                        break;
                    case 0x19://红外、无参数
                        //TODO 暂无协议，先不写
                        break;
                    case 0x1A://麦克风声音检测、无参数
                        sendResultToPad(Utils.getSystemStreamVolume());
                        break;
                    case 0x1B://获取光强、无参数
                        sendResultToPad(ReadColorL());
                        break;
                }
                break;
        }

    }

    private int ReadColorL() {

            byte[] readbuff = new byte[40];
            for (int m = 0; m < 40; m++){
                readbuff[m] = 0x00;
            }
            try {
                byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x39, null);
                LogMgr.e("cmd = " + Utils.bytesToString(cmd));
                readbuff = SP.request(cmd,100);
                if(readbuff != null){
                    LogMgr.e("readbuff = " + Utils.bytesToString(readbuff));
                    for (int i = 0; i < 30; i++) {
                        if (readbuff[i] == (byte)0xAA && readbuff[i +1] == (byte)0x55 && readbuff[i +5] == (byte)0xF0
                                && readbuff[i + 6] == (byte)0xB7) {
                            return  (int) readbuff[i + 17]/2;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
    }

    private void setCiTie(int port, int state) {
        if (port != 0) {
            port = port - 1;
        } else {
            port = ReadAITypeNew(7);//0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关  5 火焰  6 温度 7 电磁铁
        }
        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x54, new byte[]{0x02, (byte) port, (byte) state}, toStm32);
    }

    private void setSMotor(int drection, int angle) {
        int sudu = 60;
        byte[] data  = new byte[5];
        if (drection == 0) {
            sudu = sudu + 100;
        } else if (drection == 1) {
            sudu = 100 - sudu;
        }
        data[0] = 2;
        System.arraycopy(Utils.intToBytes(angle), 0, data, 1, 2);
        System.arraycopy(Utils.intToBytes(sudu), 0, data, 3, 2);
        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x53, data, toStm32);
    }

    /**
     * 设置智能电机
     * @param id 舵机ID号(1-22)
     * @param angle 舵机角度（0-1023）高位在前低位在后
     * @param speed 舵机速度（200-1023）高位在前低位在后
     */
    public void setSmartMotor(int id, int angle, int speed) {
        byte[] data = new byte[6];
        data[0] = 0x03;
        angle = 512 + angle * 1024 / 300;
        data[1] = (byte) (id & 0xFF);
        data[2] = (byte) ((angle >> 8) & 0xFF);
        data[3] = (byte) (angle &0xFF);
        data[4] = (byte) ((speed >> 8) & 0xFF);
        data[5] = (byte) (speed &0xFF);
        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x53, data, toStm32);
    }

    private void tempTest(int port) {
        int value;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValueNew(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAITypeNew(6);//0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关  5 火焰  6 温度
            value = ReadAIValueNew(searchPort);//[0, 4096]
        }
        value = (int) (value * 200 / 4096.f - 100);//[-100, 100]
        LogMgr.d("Magnetic: " + value);
        sendResultToPad(value);
    }

    private void fireTest(int port) {
        int value;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValueNew(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAITypeNew(5);//0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关 5 火焰  6 温度
            value = ReadAIValueNew(searchPort);//[0, 4096]
        }
        value = (int) (value * 200 / 4096.f - 100);//[-100, 100]
        LogMgr.d("Magnetic: " + value);
        sendResultToPad(value);
    }

    private void getMagneticC9(int port) {
        int value;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValueNew(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAITypeNew(3);//0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关
            value = ReadAIValueNew(searchPort);//[0, 4096]
        }
        value = (int) (value * 200 / 4096.f - 100);//[-100, 100]
        LogMgr.d("Magnetic: " + value);
        sendResultToPad(value);
    }
    /**
     *读取指定传感器所在的端口
     * @param type 0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关
     * @return 传感器所在端口号
     */
    private int ReadAITypeNew(int type) {
        try {
            int[] AI_type = new int[7];
            int min = 0, max = 0;
            if (type == 0) { // 超声
                min = 1668;
                max = 1968;
            } else if (type == 1) { // 碰撞
                min = 46;
                max = 346;
            } else if (type == 2) { // 灰度
                min = 890;
                max = 1190;
            } else if (type == 3) { // 霍尔传感器
                min = 2921;
                max = 3221;
            } else if (type == 4) { // 磁敏开关
                min = 1285;
                max = 1585;
            }else if(type == 5){//火焰
                min = 1678;
                max = 2545;
            }else if(type == 6){//温度
                min = 456;
                max = 789;
            }else if(type == 7){//电磁铁
                min = 346;
                max = 456;
            }

            byte[] readBuff = sendProtocol((byte) 0x01, (byte) 0xA4, (byte) 0x28, null);
            if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 &&
                    readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0xB0) {
                for (int j = 0; j < AI_type.length; j++) {
                    AI_type[j] = Utils.byte2int_2byteHL(readBuff, 11 + j * 2);
                    LogMgr.e("AI_type["+ j +"]:" + AI_type[j]);
                    if (AI_type[j] > min && AI_type[j] < max) { // 查找到Type所在的AI端口
                        return j;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int ReadAIValueNew(int port) {
        int[] AI_value =ReadAIValuenNew();
        if (AI_value != null && port >= 0 && port < AI_value.length) {
            return AI_value[port];
        }
        return -1;
    }
    // 这里写一个获取AI的方法
    public int[] ReadAIValuenNew() {
        try {
            int[] AI_value = new int[7];
            byte[] readBuff = sendProtocol((byte) 0x01, (byte) 0xA4, (byte) 0x29, null);
            if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 &&
                    readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0xB1) {
                for (int j = 0; j < AI_value.length; j++) {
                    AI_value[j] = Utils.byte2int_2byteHL(readBuff, 11 + j * 2);
                }
                return AI_value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void findColorC(int colorIndex) {
        if (colorIndex < 0 || colorIndex > 4) {
            return;
        }
        byte[] result = new byte[5];
        int colorValue= ReadColorValueSC();

        switch (colorValue) {
            case 1: //0x01代表红
                result[0] = 1;
                break;
            case 2: //0x02代表绿
                result[1] = 1;
                break;
            case 3: //0x03代表蓝
                result[2] = 1;
                break;
            case 4: //0x04代表黄
                break;
            case 5: //0x05代表黑
                result[3] = 1;
                break;
            case 6: //0x06代表白
                result[4] = 1;
                break;
        }
        LogMgr.e("colorValue = " + colorValue+";;colorIndex = " + colorIndex+ "result = " + Utils.bytesToString(result));
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(result[colorIndex]), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    private int ReadColorValueSC() {
        int[] value = getColorSensorValue();
        return value[0];
    }

    private int[] getColorSensorValue() {
        int[] value = new int[7];
        try {
            //byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x39, null);
            byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x01, (byte) 0xA4, (byte) 0x39, null);
            byte[] readBuff = SP.request(cmd);
            for (int i = 0; i < 20; i++) {
                if (readBuff[i] == (byte) 0xAA && readBuff[i + 1] == (byte) 0x55 &&
                        readBuff[i + 5] == (byte) 0xF0 && readBuff[i + 6] == (byte) 0xB7) {
                    for (int k = 0; k < value.length; k++) {
                        value[k] = readBuff[k + 11] & 0xFF;
                    }
                    return value;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
//        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x27, new byte[]{(byte) num}, toStm32);
    /*******************************函数执行部分*********************************************/
    private void motorSet(int port, int drection, int sudu) {
        if (Math.abs(drection) > 1 || Math.abs(port) > 6) {
            LogMgr.e("参数解析错误");
            return;
        }
        //速度越界限制。
        if (sudu > 100) {
            sudu = 100;
        } else if (sudu < -100) {
            sudu = -100;
        }
        if (drection == 0) {
            sudu = sudu + 100;
        } else if (drection == 1) {
            sudu = 100 - sudu;
        }
        motorChoice(port, sudu);
    }
    private void motorChoice(int port, int sudu) {
        byte[] data = new byte[6];
        if (port == 0) {
            data[0] = (0x01 << 4) & 0xff;// A端口
            dc0speed = sudu;
        } else if (port == 1) {
            data[0] = (0x01 << 3) & 0xff;// B端口
            dc1speed = sudu;
        } else if (port == 2) {
            data[0] = (0x01 << 2) & 0xff;// C端口
            dc2speed = sudu;
        } else if (port == 3) {
            data[0] = (0x01 << 1) & 0xff;// D端口
            dc3speed = sudu;
        }else if (port == 4) {
            data[0] = (0x01 << 0) & 0xff;// E端口
            dc4speed = sudu;
        }/*else if (port == 5) {
            data[0] = (0x01 << 0) & 0xff;// D端口
            dc5speed = sudu;
        }*/

        data[1] = (byte) dc0speed;
        data[2] = (byte) dc1speed;
        data[3] = (byte) dc2speed;
        data[4] = (byte) dc3speed;
        data[5] = (byte) dc4speed;
//        data[6] = (byte) dc5speed;
        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x36, data, toStm32);

    }
    private void playMusic(int type, int num) {
        // 数据过滤。
        if (type == -1 || num == -1 || type > 5 || num > 11) {
            LogMgr.e("参数解析错误");
            return;
        }
        switch (type) {
            case 0:// 打招呼 "你好","再见","反对","欢迎","请多关照"
                String[] hello = {"hello.mp3", "bye-bye.mp3",
                        "opposition.mp3", "welcome.mp3", "guanzhao.mp3"};
                if (num < hello.length) {
                    mPlayer.play(hello[num]);
                }
                break;
            case 1:// 表情
                // （"生气","傲慢","哭泣","激动","惊吓"，"委屈","高兴","可爱","大笑","悲伤","愤怒","调皮"）
                String[] face = {"Angry.mp3", "arrogant.mp3", "cry.mp3",
                        "excited.mp3", "fright.mp3", "grievance.mp3",
                        "Happy.mp3", "Kawayi.mp3", "laugh.mp3", "sad.mp3",
                        "wrath.mp3", "tricky.mp3"};
                //mPlayer.play(face[num]);
                if (num < face.length) {
                    mPlayer.play(face[num]);
                }
                break;
            case 2:// 动作 0~8（"打寒颤","卖萌","赞成","求抱抱","打哈欠"，"加油","睡觉","休闲","鬼鬼祟祟"）
                String[] action = {"cold.mp3", "cute.mp3", "favor.mp3",
                        "hug_coquetry.mp3", "yawn.mp3", "jiayou.mp3",
                        "sleep.mp3", "Leisure.mp3", "guiguisuisui.mp3"};
                //mPlayer.play(action[num]);
                if (num < action.length) {
                    mPlayer.play(action[num]);
                }
                break;
            case 3:// 动物
                String[] animal = {"niu.mp3", "hu.mp3", "haitun.mp3", "ququ.mp3",
                        "yazi.mp3", "mifeng.mp3"};
                //mPlayer.play(animal[num]);
                if (num < animal.length) {
                    mPlayer.play(animal[num]);
                }

                break;
            case 4:// 乐器
                String[] musicTool = {"1.mp3", "2.mp3", "3.mp3", "4.mp3", "5.mp3", "6.mp3", "7.mp3", "8.mp3"};
                if (num <= musicTool.length) {
                    mPlayer.play(musicTool[num - 1]);
                }
                //mPlayer.play(musicTool[num-1]);
                break;
            case 5:// 录音

                File mfile = new File(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP);
                if (mfile.exists()) {
                    //SendScratchToBrain(5, display_String_mode, sendnum);
                    mPlayer.playRecord(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP);
                }
                break;
        }

    }
    private void ledSet(int num) {
        if (num == -1 || Math.abs(num) > 2) {
            LogMgr.e("led 数据越界");
            return;
        }
        int[] color = {0x01, 0x03, 0x02};// 这里有个乱序。
        byte[] colorVule = {(byte) color[num]};
        sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x27, colorVule, toStm32);
    }
    private void display(byte[] data) {
        int type = getparms(data, 1);
        String content = "";
        if (type == 0) {// 字符串
            int len = Utils.byte2int_2byteHL(data, 2);
            // 这里需要计算出字符串的长度。
            // 从类型到第一个参数的长度为：15，加上校验位为16.
//			if (len - 16 > 19) {
//				LogMgr.e("数据长度溢出");
//				return;
//			}
            byte[] displayContext = new byte[len - 16];
            // 第二个参数从19开始。
            System.arraycopy(data, 19, displayContext, 0,
                    displayContext.length);
//			displayContext[0] = 0;
            // 这里应该直接发给brain来显示。
            try {
                LogMgr.e(" " + Utils.bytesToString(displayContext));
                content += new String(displayContext, "UTF-8");
                LogMgr.e("content: " + content);
                byte[] temp = content.getBytes();
                byte[] temp2 = new byte[temp.length + 1];
                System.arraycopy(temp, 0, temp2, 1, temp.length);
                SendScratchToBrain(5, display_String_mode, temp2);

            } catch (Exception e) {
                //LogMgr.e("err content: " + content);
                e.printStackTrace();
            }
            //SendScratchToBrain(5, display_String_mode, content.getBytes());//

        } else if (type == 1) {// 照片
            int num = getparms(data, 2);
            byte[] sendnum = new byte[5];
            sendnum[0] = 1;
            System.arraycopy(intToByte(num), 0, sendnum, 1, 4);
            File mfile = new File(SCRATCH_VJC_IMAGE_ + num
                    + SCRATCH_VJC_IMAGE_JPG);
            // LogMgr.e(" "+SCRATCH_VJC_IMAGE_+num+SCRATCH_VJC_IMAGE_JPG);
            if (mfile.exists()) {
                SendScratchToBrain(5, display_String_mode, sendnum);
            }

            // 这里通知brain显示照片。
            // SendScratchToBrain(5, display_String_mode, sendnum); //
        }
    }
    private void close(int type) {
        if (type == -1 || type > 3) {// "电机","扬声器","LED","显示"
            LogMgr.e("关闭类型越界");
            return;
        }
        switch (type){
            case 0://关闭电机
                dc0speed = 100;
                dc1speed = 100;
                dc2speed = 100;
                dc3speed = 100;
                dc4speed = 100;
                dc5speed = 100;
                sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x37, null, toStm32);
                break;
            case 1://关闭扬声器
                mPlayer.stop();
                break;
            case 2://关闭LED
                sendProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x27, new byte[]{0}, toStm32);
                break;
            case 3://关闭显示
                SendScratchToBrain(5, displayClose, new byte[2]);//关闭显示要。(字符串，照片。。)
                break;
            default:
                break;
        }
    }
    private void haveObject(int port) {
        if (port == -1 || port > 7) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(0);// 超声是0，按钮是1，灰度是2.
            value = ReadAIValue(searchPort);
        }
        LogMgr.e("value = " + value);
        if (value >= 200 || value <= 0) {
            value = 0;
        } else {
            value = 1;
        }
        LogMgr.i("是否有障碍物： " + value);
        // 这里组装下数据发到pad端。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(value), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void distance(int port) {
        if (port == -1 || port > 7) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(0);// 超声是0，按钮是1，灰度是2.
            value = ReadAIValue(searchPort);
        }
        if (value > 1500) {
            value = 1500;
        }
        LogMgr.i("距离： " + value);
        // 这里直接返回value。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(value), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void touch(int port) {
        if (port == -1 || port > 7) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(1);// 超声是0，按钮是1，灰度是2.
            value = ReadAIValue(searchPort);
        }
        if (value > 1000 && value < 4096) {
            value = 1;
        } else {
            value = 0;
        }
        // 这里直接返回value。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(value), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void readColor(int color) {// 颜色与端口没有关系。
        if (color == -1 || color > 4) {
            LogMgr.e("数组越界");
            return;
        }
        // 第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）
        int[] colorArray = new int[10];
        for (int m = 0; m < 10; m++) {
            colorArray[m] = ReadColorValue();// 这里存储6个值。
//            LogMgr.e("colorArray[m] = " + colorArray[m]);
        }
        int sensorColor = getcolor(colorArray);

        int[] colorValue = {1, 4, 2, 3, 6};// 这里与上面的颜色顺序一致。
        int sendvalue = 0;
        if (colorValue[color] == sensorColor) {
            sendvalue = 1;
        } else {
            sendvalue = 0;
        }
        // 这里把颜色发送出去就好了。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(sendvalue), 0, sendArray, 4, 4);// 这里写死就是4个字节。
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void getGraySensor(int port) {
        if (port == -1 || port > 7) {
            LogMgr.e("端口越界");
            return;
        }
        int grayValue = 0;
        if (port != 0) {
            port = port - 1;
            grayValue = ReadAIValue(port);
        } else {
            int searchPort = ReadAIType(2);// 超声是0，按钮是1，灰度是2.
            grayValue = ReadAIValue(searchPort);
        }
        // 这里可以直接发送了。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(grayValue), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private int getcolor(int[] color) {
        int[] res = {0, 0, 0, 0, 0, 0, 0};// 7个0.

        for (int i = 0; i < color.length; i++) {
            LogMgr.i("color value: " + color[i]);
            if (color[i] == 0) {
                res[0]++;
            } else if (color[i] == 1) {
                res[1]++;
            } else if (color[i] == 2) {
                res[2]++;
            } else if (color[i] == 3) {
                res[3]++;
            } else if (color[i] == 4) {
                res[4]++;
            } else if (color[i] == 5) {
                res[5]++;
            } else if (color[i] == 6) {
                res[6]++;
            }
        }
        int max = 0, index = -1;
        for (int j = 0; j < res.length; j++) {

            if (res[j] > max) {
                max = res[j];
                index = j;
            }

        }
        // 这里利用buf的index跟颜色的数据重合。
        return index;
    }
    private void takepicture(int num) {
        byte[] temp = new byte[2];
        temp[0] = 1;
        temp[1] = (byte) num;
        SendScratch(2, temp);
    }
    private void setColed(int port, int mode, float frequency, byte[] rgb) {
        byte[] dataC = new byte[9];
//        byte[] bytes = Utils.float2byte(pin);
        byte[] bytes = Utils.float2byte(frequency);
        dataC[0] = (byte) 110;
        dataC[1] = (byte) mode;
        System.arraycopy(bytes, 0, dataC, 2, 4);
        dataC[6] = rgb[0];
        dataC[7] = rgb[1];
        dataC[8] = rgb[2];
        LogMgr.d("data = " + Utils.bytesToString(dataC));
        sendProtocol((byte) 0x01, (byte) 0xA4, (byte) 0x52, dataC, toStm32);
    }
    // 获取颜色传感器值
    private int ReadColorValue() {
        byte[] readbuff = new byte[40];
        for (int m = 0; m < 40; m++){
            readbuff[m] = 0x00;
        }
            try {
                byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x39, null);
                LogMgr.e("cmd = " + Utils.bytesToString(cmd));
                readbuff = SP.request(cmd,100);
                if(readbuff != null){
                    LogMgr.e("readbuff = " + Utils.bytesToString(readbuff));
                    for (int i = 0; i < 20; i++) {
                        if (readbuff[i] == (byte)0xAA && readbuff[i +1] == (byte)0x55 && readbuff[i +5] == (byte)0xF0
                                && readbuff[i + 6] == (byte)0xB7) {
                            return  (int) readbuff[i + 11];
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        return -1;
    }
    //0x0C 时钟复位，系统时间，校准指南针，统统回复 0.
    private void ResponseZero() {
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private int ReadAIType(int type) {
//        byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'S', 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 0, 0, 0, 'O'};
        for (int n = 0; n < 3; n++) {
            byte[] readbuff = new byte[40];
            for (int m = 0; m < 40; m++)
                readbuff[m] = 0x00;

            try {
                byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x28, null);
                readbuff = SP.request(cmd);
                if (readbuff == null) {
                    LogMgr.e("SP.request Error! null");
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            // ///////////////循环查找第一个字节//////////////////
            // LogMgr.e("ReadAIType buf is: "+Utils.bytesToString(readbuff,
            // readbuff.length));
            for (int i = 0; i < 20; i++) {
                if (readbuff[i] == (byte) 0xAA && readbuff[i + 1] == (byte)0x55
                        && readbuff[i + 5] == (byte) 0xF0 && readbuff[i + 6] == (byte) 0xB0) {
                    byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, tempbuff, 0, 20);
                    System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    i = 50;
                }
            }
            // /////////////////////////////////////////////

            if (readbuff[0] == (byte) 0xAA && readbuff[1] == (byte)0x55
                    && readbuff[5] == (byte) 0xF0 && readbuff[6] == (byte) 0xB0) {
                int min = 0, max = 0, value = 0;
                if (type == 0) { // 超声 -- 测试OK
                    min = 1668;
                    max = 1968;
                } else if (type == 1) { // 按钮 -- 测试OK
                    min = 51;
                    max = 346;
                } else if (type == 2) { // 灰度

                    min = 890;
                    max = 1190;
                }else if (type == 3) { // 霍尔传感器
                    min = 2921;
                    max = 3221;
                } else if (type == 4) { // 磁敏开关
                    min = 1285;
                    max = 1585;
                }
                for (int i = 11; i < 25; i += 2) {
                    value = (int) ((readbuff[i + 1] & 0xFF) | ((readbuff[i] & 0xFF) << 8));
                    if (value > min && value < max) { // 查找到Type所在的AI
                        return (i - 11) / 2;
                    }
                }
            }
        }
        return -1;
    }
    private int ReadAIValue(int type) {
        byte[] readbuff = new byte[40];
        for (int m = 0; m < 40; m++)
            readbuff[m] = 0x00;

//        byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'A', 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 0, 0, 0, 'O'};
        for (int n = 0; n < 3; n++) {
            try {
                byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x0A, (byte) 0xA4, (byte) 0x29, null);
                LogMgr.e("cmd = " + Utils.bytesToString(cmd));
                readbuff = SP.request(cmd);
//                LogMgr.e("readbuff = " + Utils.bytesToString(readbuff,readbuff.length));
                if (readbuff == null) {
                    LogMgr.e("SP.request Error! null");
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            // ///////////////循环查找第一个字节//////////////////
//			 LogMgr.e("ReadAIvalue buf is: "+Utils.bytesToString(readbuff,
//			 readbuff.length));
            for (int i = 0; i < 20; i++) {
                if (readbuff[i] == (byte)0xAA && readbuff[i+5] == (byte)0xF0 && readbuff[i+6] == (byte)0xB1) {
                    byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, tempbuff, 0, 20);
                    System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    i = 50;
                    // n = 30; 下面直接return了 对的。 这里还是没问题的。
                }
            }
            // /////////////////////////////////////////////
            if (readbuff[5] == (byte)0xF0 && readbuff[6] == (byte)0xB1) {
                if (type >= 0 && type <= 7) {
                    int dx = type * 2;
                    return (readbuff[12 + dx] & 0xFF | (readbuff[11 + dx] & 0xFF) << 8);

                } else if (type >= 8 && type <= 11) {//这里还有问题，之后进行修改
                    int dx = type % 2;
                    return readbuff[8 + type + 1 - 6 + dx] & 0xFF
                            | (readbuff[8 + type + 6 + dx] & 0XFF) << 8;
                } else {
                    return 0;
                }
            }
        }
        return -1;
    }


    private byte[] intToByte(int value) {
        byte[] res = new byte[4];
        res[3] = (byte) (value & 0xFF);
        res[2] = (byte) ((value >> 8) & 0xFF);
        res[1] = (byte) ((value >> 16) & 0xFF);
        res[0] = (byte) ((value >> 24) & 0xFF);
        return res;

    }
    private byte[] floatToByte(float value){
        //将float转换为byte[]
        int fbit = Float.floatToIntBits(value);
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte)(fbit >> (24 - i*8));
        }//翻转数组
        int len = b.length;
        //建立一个与原数组元素类型相同的数组
        byte[] dest =  new byte[len];
        //为了防止修改原数组，将原数组拷贝一份备份
        System.arraycopy(b,0,dest,0,len);
        byte temp;
        //倒置
        for (int i = 0; i < len/2; i++) {
            temp = dest[i];
            dest[i] = dest[len - i -1];
            dest[len - i - 1] = temp;
        }
        return dest;
    }
    //字节转换为浮点；
    private float byte2float(byte[] b,int index){
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |=((long) b[index + 1] << 8);
        l &= 0xffff;
        l |=((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |=((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
    // 向底层发送信息不带返回值。
    protected void writeBuff(byte[] buff) {

        byte[] readbuff = new byte[40];
        try {
            LogMgr.e("write: " + Utils.bytesToString(buff));
            readbuff = SP.request(buff);
            if (readbuff == null) {
                LogMgr.e("SP.request Error! null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
    }
    private void getCompass() {
        int compassValue = 0;
        float[] SN = mSensor.getmO();
        LogMgr.e("yuanshi value " + SN[0]);
        compassValue = (int) SN[0];
        // 这里返回pad端就好了
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(compassValue), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    public int getMagneticSwitchC9(int port) {//获取磁敏，之前是霍尔
        int value;
        if (port != 0) {
            port = port - 1;
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(4);//0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关
            value = ReadAIValue(searchPort);//[0, 4096]
        }
        LogMgr.d("MagneticSwitch: " + value);
        if (value > 3000) {
            return 1;
        }
        return 0;
    }
    private void cimin(int port) {
        //
//        byte[] array = new byte[8];
//        SendScratchToBrain(5, 7, array);//mode用7.
        if (port == -1 || port > 7) {
            LogMgr.e("端口越界");
            return;
        }
        int grayValue = 0;
        if (port != 0) {
            port = port - 1;
            grayValue = ReadAIValue(port);
        } else {
            int searchPort = ReadAIType(3);// 0 超声 1 碰撞 2 灰度 3 霍尔 4 磁敏开关
            grayValue = ReadAIValue(searchPort);
        }
        if(grayValue != -1){
            grayValue = (int) (grayValue * 200 / 4096.f - 100);//[-100, 100]
        }
        // 这里可以直接发送了。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(grayValue), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x0A, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    /*****
     * 第几个参数
     * @param data
     * @param n
     * @return
     */
    private int getparms(byte[] data, int n) {
        byte[] array = new byte[4];
        int res = -1;// 参数没有-1，返回-1为错误。
        if (n == 1) {
            // 第一个参数从15,16,17,18
            if (data.length > 18) {
                System.arraycopy(data, 15, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        } else if (n == 2) {
            if (data.length > 22) {
                System.arraycopy(data, 19, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        } else if (n == 3) {
            if (data.length > 26) {
                System.arraycopy(data, 23, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        }else if(n == 4){
            if(data.length > 30){
                System.arraycopy(data, 27, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        }else if(n == 5){
            if(data.length > 34){
                System.arraycopy(data, 31, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        }
        return res;
    }
    private void getPosition(int num) {

        // 0~3("下俯","后仰","左翻","右翻")
        if (num == -1 || num > 3) {
            LogMgr.e("参数错误");
            return;
        }
        int value = 0;
        float[] SN = mSensor.getmO();
        switch (num) {

            case 0:
                if (SN[1] >= 5 && SN[1] <= 90) {
                    value = 1;
                }
                break;
            case 1:
                if (SN[1] >= -90 && SN[1] <= -5) {
                    value = 1;
                }
                break;
            case 2:
                if (SN[2] >= 5 && SN[2] <= 90) {
                    value = 1;
                }
                break;
            case 3:
                if (SN[2] >= -90 && SN[2] <= -5) {
                    value = 1;
                }
                break;
            default:
                break;
        }
        // 这里发送到pad端。
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(value), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void RecordVoice(int num, int time) {
        // 这里面应该这样改。
        byte[] array = new byte[8];
        System.arraycopy(intToByte(num), 0, array, 0, 4);
        System.arraycopy(intToByte(time), 0, array, 4, 4);
        SendScratchToBrain(5, 7, array);//mode用7.
    }
    //暂停功能
    private void stopScratch() {
        record_next = 0;
        sendProtocol((byte) 0x01, (byte) 0x11, (byte) 0x08, new byte[]{1,0,0,0,0,0,0,0}, toStm32);
        close(1);//关闭扬声器
        close(3);//关闭显示
//        takepicture(0);//关闭拍照
        SendScratchToBrain(5, 7, new byte[1]);//mode用7.关闭录音界面
        sendProtocol((byte) 0x01, (byte) 0x11, (byte) 0x08, new byte[]{0,0,0,0,0,0,0,0}, toStm32);
    }
    // 返回PAD端及STM32的协议封装。
    /*
	 * to pad 为 0，发送到stm32参数为1.
	 */
    public void sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data,
                             int to) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        LogMgr.e("write cmd::" + Utils.bytesToString(sendbuff));
        if (to == toPad) {
            SendScratch(1, sendbuff);
        } else if (to == toStm32) {

            try {
                SP.request(sendbuff);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void sendResultToPad(int returnValue) {
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(returnValue), 0, sendArray, 4, 4);// 这里写死就是4个字节。
        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    public byte[]  sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            if(type == (byte) 0x0A){
                buf[0] = 0x01;
            }else {
                buf[0] = type;
            }
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        LogMgr.e("write cmd::" + Utils.bytesToString(sendbuff));
        byte[] rebuffer =  SP.request(sendbuff);
        if (rebuffer!= null){
            return rebuffer;
        }
        return new byte[30];
    }

    // 协议封装： AA 55 len1 len2 type cmd1 cmd2 00 00 00 00 (data) check
    public static byte[] addProtocol(byte[] buff) {
        short len = (short) (buff.length);
        byte[] sendbuff = new byte[len + 4];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        sendbuff[3] = (byte) (len & 0x00FF); // 长度: 从type到check
        sendbuff[2] = (byte) ((len >> 8) & 0x00FF);
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data

        byte check = 0x00; // 校验位
        for (int n = 0; n <= len + 2; n++) {
            check += sendbuff[n];
        }
        sendbuff[len + 3] = (byte) (check & 0x00FF);
        return sendbuff;
    }

    @Override
    public void clearState() {
        pic_next = 0;
        record_next = 0;
        timeInit = 0;
        mPlayer.stop();
    }
}
