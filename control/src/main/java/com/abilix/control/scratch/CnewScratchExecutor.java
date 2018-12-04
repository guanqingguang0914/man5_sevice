package com.abilix.control.scratch;

import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.model.Model;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.IPlayStateListener;

import static com.abilix.control.sp.SP.request;

/*
 * 主要调试点在参数解析，数据发送，给brain的通信。
 */
public class CnewScratchExecutor extends AbstractScratchExecutor {
    private Timer mTimer;
    private TimerTask mTimerTask;
    protected MySensor mSensor;
    //这里定义一个时间戳，防止拍照录音等无限循环卡死。
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;
    //麦克风录音时间记录。
    private int timeInit = 0;
    private int wheel = 0;

    public CnewScratchExecutor(Handler mHandler) {
        super(mHandler);

        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come Scratch execute " + Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        //这里做一个协议类型区分。
        if (data[5] == 0x0A || data[5] == 0x11) {
            checkFunctionNew(rec);
        } else if (data[5] == 0x04) {
            checkFunction(rec);
        }
    }

    // 这里还是要记忆的。
    private int dc0speed = 100;
    private int dc1speed = 100;
    private int dc2speed = 100;
    private int dc3speed = 100;
    public static int U_open = 0;
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int toPad = 0;
    private final int toStm32 = 1;
    //这里是播放录音文件路径。
    public final static String DATA_PATH = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "Abilix"
            + File.separator + "RobotInfo" + File.separator;
    public final static String AUDIOPATH = Environment
            .getExternalStorageDirectory().getPath()
            + File.separator
            + "Abilix" + File.separator + "AbilixMusic" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_U201 = DATA_PATH + "PhotoU201" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_ = DATA_PATH + "Photo" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_JPG = ".jpg";
    public final static String SCRATCH_VJC_IMAGE_GIF = ".gif";
    public static final String RECORD_SCRATCH_VJC_ = FileUtils.DATA_PATH
            + File.separator + "Record" + File.separator;
    public static final String RECORD_PATH_3GP = ".3gp";

    // 这里先这么定义
    private final int display_String_mode = 12;
    private final int displayClose = 2;

    private void checkFunctionNew(final byte[] data) {

        int parms1 = -1, parms2 = -1, parms3 = -1, parms4 = -1, parms5 = -1;
        //byte[] array = new byte[4];//用于复制数据的临时buff
//        if (data[5] != 0x0A) {
//            // C新版协议
//            LogMgr.e("协议类型错误");
//            return;
//        }
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[5]){
            case 0x11:
                stopScratch();
                break;
            default:
                switch (data[6]) {
                    case 0x01:// 启动电机 第一个参数为电机端口：0~3("A","B","C","D")，第二个参数为转向0~1  --------OK
                        // ("正转","反转")，第三个参数为速度：0~100("快","中","慢");
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2 + "  parms3: " + parms3);
                        motorSet(parms1, parms2, parms3);
                        break;
                    case 0x02:// 启动扬声器												----------------还有一个录音。
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        //parms2 = getParms(data, 2);
                        //启动扬声器，需要单独解析。
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        playMusic(parms1, parms2);
                        break;
                    case 0x03:// 启动LED 第一个参数为颜色：0~2("红","绿","蓝")               ---------------OK
                        parms1 = getparms(data, 1);
                        ledSet(parms1);
                        break;
                    case 0x04:// 启动显示												--------------等待调试
                        // 第一个参数为类型：0~1("字符","照片")，（1）当第一个参数为"字符"时，第二个参数为字符串数组（最大19个字节）（2）当第一个参数为"照片"时，第二个参数为0~9
                        display(data);
                        break;
                    case 0x05:// 关闭 第一个参数为类型：0~3("电机","扬声器","LED","显示")           ------------还有关闭显示
                        parms1 = getparms(data, 1);
                        close(parms1);
                        break;
                    case 0x06:// 超声探测到障碍物 第一个参数为端口：0~7                          ----------------------ok
                        parms1 = getparms(data, 1);   //这里改为4个字节。 先测试下端口，有问题。
                        LogMgr.i("duankou: " + parms1);
                        haveObject(parms1);
                        break;
                    case 0x07:// 超声探测距离 第一个参数为端口：0~7                        --------------------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.i("duankou: " + parms1);
                        distance(parms1);
                        break;
                    case 0x08:// 碰到物体 第一个参数为端口：0~7                             ------------------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.e("duankou: " + parms1);
                        touch(parms1);
                        break;
                    case 0x09:// 识别颜色
                        // 第一个参数为端口：0~7，第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）-----------ok
                        parms1 = getparms(data, 1);
			            parms2 = getparms(data, 2);
                        readColor((ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_CU) ? parms2 : parms1);
                        break;
                    case 0x0A:// 探测灰度值 第一个参数为端口：0~7                        --------- -----------------ok
                        parms1 = getparms(data, 1);
                        LogMgr.i("duankou: " + parms1);
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
                    case 0x0C:// 时钟复位 不做。
                        ResponseZero();
                        break;
                    case 0x0D:// 系统时间 不做。
                        ResponseZero();
                        break;
                    case 0x0E:// 指南针校准 不做。
                        ResponseZero();
                        break;

                    case 0x0F:// 指南针探测角度
                        getCompass();
                        break;
                    case 0x10:// 陀螺仪探测 第一个参数为类型：0~3("下俯","后仰","左翻","右翻")  ----------------OK
                        parms1 = getparms(data, 1);
                        getPosition(parms1);
                        break;
                    case 0x11:// 麦克风录音 第一个参数为：0~9，第二个参数为时间：1~3600          ----------------OK

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
                                && Record_pre - record_next >= timeInit * 1000 - 100) {
                            record_next = Record_pre;
                            timeInit = parms2;
                            if (parms2 >= 1) { // 录音时间小于0都不处理。
                                RecordVoice(parms1, parms2);
                            }
                        }
                        break;
                    case 0x17://闭环运动，大小电机，端口，速度// 启动电机 第一个参数为电机端口：0~1 0:小电机；1：大电机，第二个参数为转向0~5
                        // A,B,C,D,A+D,B+C，第三个参数为速度：-100~100
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2 + "  parms3: " + parms3);
                        motorSetNew(parms1,parms2,parms3);
                        break;
                    case 0x18://闭环电机：大小电机，端口，速度，（角度，圈数，时间），对应的值；
                        // 启动电机 第一个参数为电机端口：0~1 0:小电机；1：大电机，第二个参数为端口0~5:
                        // A,B,C,D,A+D,B+C，第三个参数为速度：-100~100
                        //第四个参数：0~2 ，0：角度，1：圈数，2：时间；第5个参数：第四个参数对应的值；
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        parms4 = getparms(data, 4);
                        parms5 = getparms(data, 5);
                        LogMgr.e("parms1: " + parms1 + ";  parms2: " + parms2 + ";  parms3: " + parms3 +";parms4 = "+parms4+";parms5="+parms5);
                        motorSetAQT(parms1,parms2,parms3,parms4,parms5);
                        break;
                    case 0x19://平衡车 1-4:前进；超声；设置；速度
                        byte[] car = new byte[7];
                        System.arraycopy(data, 15, car, 0, car.length);
                        /*if(car[1]  == 1){
                            U_open = 1;//1是开启
                        }else if(car[1]  == 2){
                            U_open = 2;//2是关闭
                        }
                        car[1] = (byte) U_open;*/
                        LogMgr.e("car = " + Utils.bytesToString(car));
                        banlance(car);
                        break;
                    case 0x20:
                        parms1 = getparms(data, 1);//获取动画的参数；
                        gifView(parms1);
                        break;
                    case 0x12://新的扬声器；
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
                                LogMgr.d("play = " + play.toString() + ";num = " + num);
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
                    case 0x13://蝎子控制
                        byte[] order = new byte[4];
                        System.arraycopy(data, 15, order, 0,4);
                        LogMgr.e("order = " + Utils.bytesToString(order));
                        Xmove(order);
                        break;
                    case 0x14://坦克救援车
                        byte[] order_t = new byte[4];
                        System.arraycopy(data, 15, order_t, 0,4);
                        LogMgr.e("order_t = " + Utils.bytesToString(order_t));
                        Tmove(order_t);
                        break;
                }
                break;
        }

    }

    private void Tmove(byte[] order_t) {
        switch (order_t[0]){
            case 0x01://移动命令
//                Model.initOrDestroyInstance(mHandler,1);//坦克模型
                LogMgr.d("收到模型移动命令");
                Model.getInstance().move(order_t[1],order_t[2]);
                break;
            case 0x02://功能命令
                LogMgr.d("收到模型功能开关命令");
                boolean onOrOff = false;
                if(order_t[1] == Model.FUNCTION_OFF+1){
                    onOrOff = true;
                }else if(order_t[1] == Model.FUNCTION_ON+1){
                    onOrOff = false;
                }
//                LogMgr.e("onOrOff = " + onOrOff);
                Model.getInstance().function(Model.FUNCTION_AVOID_OBSTACLE,onOrOff);
                break;
            case 0x03://初始化模型
                LogMgr.d("收到模型类型通知命令");
                Model.initOrDestroyInstance(mHandler,1);//坦克模型
                break;
            case 0x04://退出此模型
                LogMgr.d("收到模型类型通知停止命令");
                Model.initOrDestroyInstance(mHandler,0);//退出模型
                break;
        }
    }

    private void Xmove(byte[] order) {
        switch (order[0]){
            case 0x01://移动命令
                LogMgr.d("收到模型移动命令");
                Model.getInstance().move(order[1],order[2]);
                break;
            case 0x02://功能命令
//                sendProtocol((byte) 0x01, (byte) 0x24, (byte) 0x01, new byte[]{0x01 ,0x02}, toStm32);//初始化类型
//                sendProtocol((byte) 0x01, (byte) 0x24, (byte) 0x06, new byte[]{0x01 ,0x01}, toStm32);//初始化类型
                switch (order[1]) {
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                        Model.getInstance().action(order[1], new Model.ModelCallback() {
                            @Override
                            public void onActionStart() {

                            }
                            @Override
                            public void onActionStop() {

                            }
                            @Override
                            public void onActionRefused() {

                            }
                        });
                        break;
                    case 0x05: //避障开
                        Model.getInstance().function(Model.FUNCTION_AUTO_ATTACK, true);
                        break;
                    case 0x06: //避障关
                        Model.getInstance().function(Model.FUNCTION_AUTO_ATTACK, false);
                        break;
                }
                break;
            case 0x03://初始化模型
                LogMgr.d("收到模型类型通知命令");
                Model.initOrDestroyInstance(mHandler,2);//蝎子模型
                break;
            case 0x04://退出此模型
                LogMgr.d("收到模型类型通知停止命令");
                Model.initOrDestroyInstance(mHandler,0);//退出模型
                break;
        }
    }

    //老协议。
    private void checkFunction(final byte[] data) {
        // 函数名是第六个字节。设置电机。
        if (data[6] == 0x01) {

            byte[] sudubuff = new byte[4];
            System.arraycopy(data, 16, sudubuff, 0, sudubuff.length);
            int sudu = Utils.byteAray2Int(sudubuff);
            if (sudu > 100) {
                sudu = 100;
            } else if (sudu < -100) {
                sudu = -100;
            }
            sudu = sudu + 100;
            motorChoice(data[15], sudu);

            // 这里是runmovetime.
        } else if (data[6] == 0x02) {

            byte[] motor = new byte[]{'C', 'S', 'E', 'T', 'M', 0x64, 0x64,
                    0x64, 0x64, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 'O'};
            byte sp = data[19];
            // LogMgr.e("speed is 11111111111111     " + sp);
            // sp = sp + 100;//这里加一百。
            byte speed = (byte) (sp + 100);
            // byte speed = (byte) (data[19] & 0xff);
            // 这里是电机0~3.
            if (data[15] == 0x00) {
                dc0speed = speed;
            } else if (data[15] == 0x01) {
                dc1speed = speed;
            } else if (data[15] == 0x02) {
                dc2speed = speed;
            } else if (data[15] == 0x03) {
                dc3speed = speed;
            }
            motor[5] = (byte) dc0speed;
            motor[6] = (byte) dc1speed;
            motor[7] = (byte) dc2speed;
            motor[8] = (byte) dc3speed;
            writeBuff(motor);

            try {
                Thread.sleep((int) data[23] * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] motorstop = new byte[]{'C', 'S', 'E', 'T', 'M', 0x64,
                    0x64, 0x64, 0x64, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0,
                    'O'};
            if (data[15] == 0x00) {
                dc0speed = (byte) 100;
            } else if (data[15] == 0x01) {
                dc1speed = (byte) 100;
            } else if (data[15] == 0x02) {
                dc2speed = (byte) 100;
            } else if (data[15] == 0x03) {
                dc3speed = (byte) 100;
            }
            motorstop[5] = (byte) dc0speed;
            motorstop[6] = (byte) dc1speed;
            motorstop[7] = (byte) dc2speed;
            motorstop[8] = (byte) dc3speed;

            writeBuff(motorstop);

            // 停止运动。
        } else if (data[6] == 0x03) {
            sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x14, null, toStm32);// 停止所有电机
            // 模拟动物 setSound1
        } else if (data[6] == 0x04) {
            // 牛
            if (data[15] == 0x00) {
                mPlayer.play("niu.mp3");
                // 虎
            } else if (data[15] == 0x01) {
                mPlayer.play("hu.mp3");
                // 海豚
            } else if (data[15] == 0x02) {
                mPlayer.play("haitun.mp3");
                // 蛐蛐
            } else if (data[15] == 0x03) {
                mPlayer.play("ququ.mp3");
                // 鸭子
            } else if (data[15] == 0x04) {
                mPlayer.play("yazi.mp3");
                // 飞虫
            } else if (data[15] == 0x05) {
                mPlayer.play("mifeng.mp3");
            }
        } else if (data[6] == 0x05) {
            // 萨克斯
            if (data[15] == 0x00) {
                mPlayer.play("sakesi.mp3");
                // 钢琴
            } else if (data[15] == 0x01) {
                mPlayer.play("gangqin.mp3");
                // 鼓
            } else if (data[15] == 0x02) {
                mPlayer.play("gudian.mp3");
                // 大提琴
            } else if (data[15] == 0x03) {
                mPlayer.play("datiqin.mp3");
                // 小号
            } else if (data[15] == 0x04) {
                mPlayer.play("xiaohao.mp3");
                // 吉他
            } else if (data[15] == 0x05) {
                mPlayer.play("jita.mp3");
            }
        } else if (data[6] == 0x06) {
            // 介绍一
            if (data[15] == 0x00) {
                mPlayer.play("cchangedansw1.mp3");
                // 介绍二
            } else if (data[15] == 0x01) {
                mPlayer.play("cchangedansw2.mp3");
                // 介绍三
            } else if (data[15] == 0x02) {
                mPlayer.play("cchangedansw3.mp3");
                // 介绍四
            } else if (data[15] == 0x03) {
                mPlayer.play("cchangedansw4.mp3");
                // 介绍五
            } else if (data[15] == 0x04) {
                mPlayer.play("cchangedansw5.mp3");
                // 介绍六
            } else if (data[15] == 0x05) {
                mPlayer.play("cchangedansw6.mp3");
                // 介绍七
            } else if (data[15] == 0x06) {
                mPlayer.play("cchangedansw7.mp3");
                // 介绍八
            } else if (data[15] == 0x07) {
                mPlayer.play("cchangedansw8.mp3");
            }
            // 停止播放。
        } else if (data[6] == 0x07) {
            mPlayer.stop();
            // led
        } else if (data[6] == 0x08) {
            byte[] led = new byte[]{'C', 'S', 'E', 'T', 'L', 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 'O'};
            // 灭 红 绿 蓝
            if (data[15] == 0x00) {

                led[5] = (byte) 0x00;
            } else if (data[15] == 0x01) {
                // rgb
                led[5] = (byte) 0x01;
            } else if (data[15] == 0x02) {

                led[5] = (byte) 0x03;
            } else if (data[15] == 0x03) {

                led[5] = (byte) 0x02;
            }
            writeBuff(led);
            // 下面是传感器。 前方有障碍物。
        } else if (data[6] == 0x09) {
            int index = ReadAIType(0); // 获取超声对应的AI口
            int value = 0;
            if (index != -1)
                value = ReadAIValue(index); // 获取AI口的值

            if (value >= 200 || value <= 0) {
                value = 0;
            }
            dealwith(value, data, 1);

            // 探测距离。
        } else if (data[6] == 0x0A) {

            // strReturn = getParam(str, 10);
            int index = ReadAIType(0); // 获取超声对应的AI口
            LogMgr.e("duankou : " + index);
            int value = 0;
            if (index != -1)
                value = ReadAIValue(index); // 获取AI口的值
            LogMgr.e("distance value is: " + value);
            if (value <= 0) {
                value = 0;
            }
            if (value > 1500) {
                value = 1500;
            }
            dealwith(value, data, 1);

            // 机器人碰到物体。
        } else if (data[6] == 0x0B) {
            // strReturn = getParam(str, 10);
            int index = ReadAIType(1); // 获取按钮对应的AI口
            LogMgr.e("duankou : " + index);
            int value = 0;
            if (index != -1)
                value = ReadAIValue(index); // 获取AI口的值

            if (value != -1) {
                if (value > 1000 && value < 4096) { // 碰撞
                    value = 1;
                } else { // 未碰撞
                    value = 0;
                }
            } else {
                // 未检测到按钮传感器
                value = 0;
            }
            dealwith(value, data, 1);

            // 感受到颜色。 颜色传感器只有一个0x0c 0~4 ("红色","绿色","蓝色","黑色","白色")
        } else if (data[6] == 0x0C) {

            readColorOld(data, data[15]);
            // 地面灰度。
        } else if (data[6] == 0x0D) {

            // 灰度模式是2.
            int index = ReadAIType(2); // 超声传递0，按钮传递1.地面灰度传递？暂时默认跟超声一样。
            int value = 0;
            if (index != -1)
                value = ReadAIValue(index); // 获取AI口的值
            if (value < 0) {
                value = 0;
            }
            dealwith(value, data, 1);

            // 这里受磁场影响较大，具体的无法调节。 新协议直接返回角度。
        } else if (data[6] == 0x0E) {
            // 东南西北 0~3.
            int value = 0;
            float[] SN = mSensor.getmO();
            LogMgr.e("yuanshi value " + SN[0]);
            value = (int) SN[0];
            dealwith(value, data, 1);

            // 下俯，上扬，左翻，右翻。
        } else if (data[6] == 0x0F) {

            int value = 0;
            float[] SN = mSensor.getmO();
            // 上下是value[1].下正 上负。
            if (data[15] == 0) {
                if (SN[1] >= 5 && SN[1] <= 90) {
                    value = 1;
                }
                // 上扬。
            } else if (data[15] == 1) {
                if (SN[1] >= -90 && SN[1] <= -5) {
                    value = 1;
                }
                // 左翻，右翻。 左正右负。
            } else if (data[15] == 2) {
                if (SN[2] >= 5 && SN[2] <= 90) {
                    value = 1;
                }
                // 右翻。
            } else if (data[15] == 3) {
                if (SN[2] >= -90 && SN[2] <= -5) {
                    value = 1;
                }
            }
            LogMgr.e("上下府  SN[1]=： " + SN[1] + " 左右翻转 SN[2]=: " + SN[2]);
            dealwith(value, data, 1);
            // 麦克风音量检测。
        } else if (data[6] == 0x10) {

            // 现在是录音，这里有一个时间参数，给brain发信息。这里我直接发送那四个字节。
            byte[] temp = new byte[4];
            System.arraycopy(data, 15, temp, 0, temp.length);
            int time = Utils.byteAray2Int(temp);
            Record_pre = System.currentTimeMillis();
            if (Record_pre - record_next >= 1000
                    && Record_pre - record_next >= time * 1000) {
                record_next = Record_pre;

                if (time >= 1) {
                    SendScratch(3, temp);
                }

            }

            // 时钟复位。
        } else if (data[6] == 0x11) {

            // 这里无意义，只返回0就好了。
            dealwith(0, data, 1);
            // 系统时间 也是一样
        } else if (data[6] == 0x12) {

            dealwith(0, data, 1);
        } else if (data[6] == 0x13) {
            // 显示。这里显示什么呢。

        } else if (data[6] == 0x14) {// 拍照。
            pic_pre = System.currentTimeMillis();
            if (pic_pre - pic_next > 3500) {
                pic_next = pic_pre;
                byte[] temp = new byte[1];
                temp[0] = 1;
                SendScratch(2, temp);
            }

        } else if (data[6] == 0x15) { // 校准指南针。
        } else if (data[6] == 0x17){//闭环运动，大小电机，端口，速度

        } else if (data[6] == 0x18){//闭环电机：大小电机，端口，速度，（角度，圈数，时间），对应的值；

        }

    }
    //显示gif动画

    //这里暂时添加一个Handler
//    private Handler motorHandler;
//    private Runnable motorrunnable;
    /*******************************函数执行部分*********************************************/
    //0x18 调用大小电机，端口，速度，角度圈数时间，对应值
    private void motorSetAQT(int motor, int port, int sudu, int aqt, int aqt_value) {
        if (Math.abs(motor) > 1 || Math.abs(port) > 5) {
            LogMgr.e("参数解析错误");
            return;
        }
        //速度越界限制。
        if (sudu > 100) {
            sudu = 100;
        } else if (sudu < -100) {
            sudu = -100;
        }
        sudu += 100;
        byte[] data = new byte[21];
        //byte[] dataEnd = new byte[21];
//        data = motorPort(data,port,motor,sudu);
        data =  motorAqt(data,port,aqt,aqt_value,motor,sudu);
        //dataEnd = motorAqt(data,port,aqt,aqt_value);
//        switch(aqt){
//            case 0://角度
////                for (int i = 0;i <4 ;i++){
////                    data [2 + i*5] = 2;
////                    data [4 + i*5] = (byte) ((aqt_value >> 8) & 0xFF);
////                    data [5 + i*5] = (byte) (aqt_value  & 0xFF);
////                }
//               data =  motorAqt(data,port,aqt,aqt_value,motor,sudu);
//                break;
//            case 1://圈数
////                for (int i = 0;i <4 ;i++){
////                    data [2 + i*5] = 1;
////                    data [4 + i*5] = (byte) ((aqt_value >> 8) & 0xFF);
////                    data [5 + i*5] = (byte) (aqt_value  & 0xFF);
////                }
//                data =  motorAqt(data,port,aqt,aqt_value,motor,sudu);
//                break;
//            case 2://时间
//                //1.s时间这块需要Scratch进行控制，延迟aqt，aqt_vaule时间然后发送停止电机的命令；类似M5
//                //2.Brain端处理：可用handler
//                LogMgr.d("这里表示的是时间：aqt_vaule = " + aqt_value);
////                motorrunnable = new Runnable() {
////                    @Override
////                    public void run() {
////                        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x14, null, toStm32);
////                    }
////                };
////                motorHandler.removeCallbacks(motorrunnable);
////                motorHandler.postDelayed(motorrunnable,aqt_value*1000);
//                break;
//        }
        LogMgr.d("dataEnd = " + Utils.bytesToString(data));

        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x03, data, toStm32);
    }
    //停止功能
    private void stopScratch() {
        record_next = 0;
        sendProtocol((byte) 0x01, (byte) 0x11, (byte) 0x08, new byte[]{1,0,0,0,0,0,0,0}, toStm32);
        close(1);//关闭扬声器
        close(2);//关闭LED
        close(3);//关闭显示
//        takepicture(0);//关闭拍照
        SendScratchToBrain(5, 7, new byte[1]);//mode用7.关闭录音界面
        sendProtocol((byte) 0x01, (byte) 0x11, (byte) 0x08, new byte[]{0,0,0,0,0,0,0,0}, toStm32);
    }
    private byte[] motorAqt(byte[] data, int port, int aqt, int aqt_value,int motor,int sudu) {
        data = new byte[21];
        if(port == 0){//A电机
            data[0] = (0x01 << 3) & 0xff;// A端口
            data[1] = (byte) motor;
            data [2] = (byte) (2-aqt);
            data[3] = (byte) sudu;
            data [4] = (byte) ((aqt_value >> 8) & 0xFF);
            data [5] = (byte) (aqt_value  & 0xFF);
        }else if(port ==1){
            data[0] = (0x01 << 2) & 0xff;// B端口
            data[6] = (byte) motor;
            data [7] = (byte) (2-aqt);
            data[8] = (byte) sudu;
            data [9] = (byte) ((aqt_value >> 8) & 0xFF);
            data [10] = (byte) (aqt_value  & 0xFF);
        }else if(port ==2){
            data[0] = (0x01 << 1) & 0xff;// C端口
            data[11] = (byte) motor;
            data [12] = (byte) (2-aqt);
            data[13] = (byte) sudu;
            data [14] = (byte) ((aqt_value >> 8) & 0xFF);
            data [15] = (byte) (aqt_value  & 0xFF);
        }else if(port ==3){
            data[0] = (0x01 << 0) & 0xff;// D端口
            data[16] = (byte) motor;
            data [17] = (byte) (2-aqt);
            data[18] = (byte) sudu;
            data [19] = (byte) ((aqt_value >> 8) & 0xFF);
            data [20] = (byte) (aqt_value  & 0xFF);
        }else if(port ==4){// A+D端口
            data[0] = 9;// A+D端口
            data[1] = (byte) motor;
            data [2] = (byte) (2-aqt);
            data[3] = (byte) sudu;
            data [4] = (byte) ((aqt_value >> 8) & 0xFF);
            data [5] = (byte) (aqt_value  & 0xFF);
            data[16] = (byte) motor;
            data [17] = (byte) (2-aqt);
            data[18] = (byte) sudu;
            data [19] = (byte) ((aqt_value >> 8) & 0xFF);
            data [20] = (byte) (aqt_value  & 0xFF);
        }else if(port ==5){
            data[0] = 6;// B+C端口
            data[6] = (byte) motor;
            data [7] = (byte) (2-aqt);
            data[8] = (byte) sudu;
            data [9] = (byte) ((aqt_value >> 8) & 0xFF);
            data [10] = (byte) (aqt_value  & 0xFF);
            data[11] = (byte) motor;
            data [12] = (byte) (2-aqt);
            data[13] = (byte) sudu;
            data [14] = (byte) ((aqt_value >> 8) & 0xFF);
            data [15] = (byte) (aqt_value  & 0xFF);
        }
        return data;
    }
    //实现平衡车
    private void banlance(byte[] car){
        /*//停止定时器
        stopTimer();
        float yaw = 0;
        if((car[0] == 1 || car[0] == 2) && car[2] != 0 && car[4] != 0) {//前进
            startTimer(yaw,car);
        }else {//除了前进的命令，其他可以直接发送；
            sendProtocol((byte)0x01,(byte)0xA3,(byte)0x29,car,toStm32);
        }*/
       //直接发送，有stm32处理
        sendProtocol((byte)0x01,(byte)0xA3,(byte)0x29,car,toStm32);
    }
    private void gifView(int num) {
        //SCRATCH_VJC_IMAGE_GIF
        byte[] sendnum = new byte[5];
        sendnum[0] = 2;
        sendnum[4] = (byte) (num & 0xFF);
        System.arraycopy(intToByte(num), 0, sendnum, 1, 4);
        File mfile = new File(SCRATCH_VJC_IMAGE_U201 + (num) + SCRATCH_VJC_IMAGE_GIF);
        if (mfile.exists()) {
            SendScratchToBrain(5, display_String_mode, sendnum);
        }
    }
    private int Times = 0;
    private void startTimer(final float yaw, final byte[] car) {
        Times = 0;
        final float value = Utils.byteToInt(new byte[]{car[5],car[6]});
        if (mTimer == null) {
            mTimer = new Timer();
            sendProtocol((byte)0x01,(byte)0xA3,(byte)0x29,car,toStm32);
//            sendProtocol((byte)0x01,(byte)0xA3,(byte)0x29,new byte[]{car[0],0,0x01,0x32,0,0,0},toStm32);
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if(car[0] == 3 || car[0] == 4){
                        Times++;
                        if(Times > (/*30*value/car[3])*/ value/4 )){
                            LogMgr.e("Times = " +Times + ";value / 4 = " + value / 4 );
                            byte[] data = new byte[7];
                            sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data, toStm32);
                            stopTimer();
                            Times = 0;
                        }
                    }
                    else if(car[0] == 1 || car[0] == 2){
                        sendProtocol((byte)0x01,(byte)0xA3,(byte)0x29,car,toStm32);
                        //wheelset(car[3],car[3]);
                        Times++;
                        if(Times > (/*30*value/car[3])*/ value -1)){
                            LogMgr.e("Times = " +Times + ";value / 10 = " + value / 10 );
                            byte[] data = new byte[7];
                            sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data, toStm32);
                            stopTimer();
                            Times = 0;
                        }
                    }
                }
            };
            mTimer.schedule(mTimerTask,0,35);
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }
    private void wheelset(int left, int right) {

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
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data,toStm32);

    }
    //0x17调用大小电机
    private void motorSetNew(int motor, int port, int sudu) {
        if (Math.abs(motor) > 1 || Math.abs(port) > 5) {
            LogMgr.e("参数解析错误");
            return;
        }
        //速度越界限制。
        if (sudu > 100) {
            sudu = 100;
        } else if (sudu < -100) {
            sudu = -100;
        }
        sudu += 100;
        byte[] data = new byte[21];
        data = motorPort(data,port,motor,sudu);
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x03, data, toStm32);
    }

    private byte[] motorPort(byte[] data,int port,int motor,int sudu) {
        data = new byte[21];
        if(port == 0){
            data[0] = (0x01 << 3) & 0xff;// A端口
            data[1] = (byte) motor;
            data[2] = 0;
            data[3] = (byte) sudu;
        } else if (port == 1){
            data[0] = (0x01 << 2) & 0xff;// B端口
            data[6] = (byte) motor;
            data[7] = 0;
            data[8] = (byte) sudu;
        }else if(port == 2){
            data[0] = (0x01 << 1) & 0xff;// C端口
            data[11] = (byte) motor;
            data[12] = 0;
            data[13] = (byte) sudu;
        }else if(port == 3){
            data[0] = (0x01 << 0) & 0xff;// D端口
            data[16] = (byte) motor;
            data[17] = 0;
            data[18] = (byte) sudu;
        }else if(port == 4){
            data[0] = 9;// A+D端口
            data[1] = (byte) motor;
            data[2] = 0;
            data[3] = (byte) sudu;
            data[16] = (byte) motor;
            data[17] = 0;
            data[18] = (byte) sudu;
        }else if(port == 5){
            data[0] = 6;// B+C端口
            data[6] = (byte) motor;
            data[7] = 0;
            data[8] = (byte) sudu;
            data[11] = (byte) motor;
            data[12] = 0;
            data[13] = (byte) sudu;
        }
        return data;
    }

    // 0x01 调用电机。
    private void motorSet(int port, int drection, int sudu) {
        // 暂设速度为100 60 20. 电机协议换成新的。
        if (Math.abs(drection) > 1 || Math.abs(port) > 3) {
            LogMgr.e("参数解析错误");
            return;
        }
        //速度越界限制。
        if (sudu > 100) {
            sudu = 100;
        } else if (sudu < -100) {
            sudu = -100;
        }
        //这里暂时加入判断U201 CD 端口 左轮方向相反
        if(ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_CU && ((port == 2) | (port == 3))){
            if (drection == 0) {
//                sudu = sudu + 100;
                sudu = 100 - sudu;
            } else if (drection == 1) {
//                sudu = 100 - sudu;
                sudu = sudu + 100;
            }
        }else{
            if (drection == 0) {
                sudu = sudu + 100;
            } else if (drection == 1) {
                sudu = 100 - sudu;
            }
        }
        // 这里用新协议进行单一端口控制，状态记录与否无所谓。
        motorChoice(port, sudu);
    }

    private void motorChoice(int port, int sudu) {

        byte[] data = new byte[5];
        if (port == 0) {
            data[0] = (0x01 << 3) & 0xff;// A端口
            dc0speed = sudu;
        } else if (port == 1) {
            data[0] = (0x01 << 2) & 0xff;// B端口
            dc1speed = sudu;
        } else if (port == 2) {
            data[0] = (0x01 << 1) & 0xff;// C端口
            dc2speed = sudu;
        } else if (port == 3) {
            data[0] = (0x01 << 0) & 0xff;// D端口
            dc3speed = sudu;
        }

        data[1] = (byte) dc0speed;
        data[2] = (byte) dc1speed;
        data[3] = (byte) dc2speed;
        data[4] = (byte) dc3speed;
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x13, data, toStm32);

    }
    //获取版本号；如果大于等于1.3.0.12，用最新的协议
    private long getStmVersion(){//得到储存的固件版本号；
        long StmVersion = 0;
        String stm = "";
        stm  = FileUtils.readFile(FileUtils.STMVERSION_PATH).trim();
//        LogMgr.e("stm = " + stm);
        if(stm != null && !"".equals(stm) && !"-1".equals(stm)){
            StmVersion = Long.parseLong(stm);
//            LogMgr.e("stm2 = " + stm);
        }
        return StmVersion;
    }
    // 0x02
    private String[] hello_zh = {"hello.mp3", "bye-bye.mp3",
            "opposition.mp3", "welcome.mp3", "guanzhao.mp3"};
    private String[] hello_en = {"hello_en.mp3", "bye-bye_en.mp3",
            "opposition_en.mp3", "welcome_en.mp3", "guanzhao_en.mp3"};

    private String[] face_zh = {"Angry.mp3", "arrogant.mp3", "cry.mp3",
            "excited.mp3", "fright.mp3", "grievance.mp3",
            "Happy.mp3", "Kawayi.mp3", "laugh.mp3", "sad.mp3",
            "wrath.mp3", "tricky.mp3"};
    private String[] face_en = {"Angry.mp3", "arrogant_en.mp3", "cry_en.mp3",
            "excited_en.mp3", "fright_en.mp3", "grievance_en.mp3",
            "Happy.mp3", "Kawayi_en.mp3", "laugh_en.mp3", "sad_en.mp3",
            "wrath_en.mp3", "tricky_en.mp3"};

    private String[] action_zh = {"cold.mp3", "cute.mp3", "favor.mp3",
            "hug_coquetry.mp3", "yawn.mp3", "jiayou.mp3",
            "sleep.mp3", "Leisure.mp3", "guiguisuisui.mp3"};
    private String[] action_en = {"cold_en.mp3", "cute_en.mp3", "favor_en.mp3",
            "hug_me_en.mp3", "yawn_en.mp3", "jiayou_en.mp3",
            "sleep_en.mp3", "Leisure_en.mp3", "guiguisuisui_en.mp3"};

    private void playMusic(int type, int num) {
        // 数据过滤。
        if (type == -1 || num == -1 || type > 5 || num > 11) {
            LogMgr.e("参数解析错误");
            return;
        }
        switch (type) {
            case 0:// 打招呼 "你好","再见","反对","欢迎","请多关照"
                String[] hello = hello_zh;
                if (Utils.isEn()) {
                    hello = hello_en;
                }
                if (num < hello.length) {
                    mPlayer.play(hello[num]);
                }
                break;
            case 1:// 表情
                // （"生气","傲慢","哭泣","激动","惊吓"，"委屈","高兴","可爱","大笑","悲伤","愤怒","调皮"）
                String[] face = face_zh;
                if (Utils.isEn()) {
                    face = face_en;
                }
                //mPlayer.play(face[num]);
                if (num < face.length) {
                    mPlayer.play(face[num]);
                }
                break;
            case 2:// 动作 0~8（"打寒颤","卖萌","赞成","求抱抱","打哈欠"，"加油","睡觉","休闲","鬼鬼祟祟"）
                String[] action = action_zh;
                if (Utils.isEn()) {
                    action = action_en;
                }
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

    // 0x03
    private void ledSet(int num) {

        if (num == -1 || Math.abs(num) > 2) {
            LogMgr.e("led 数据越界");
            return;
        }

        byte[] led = new byte[]{'C', 'S', 'E', 'T', 'L', 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 'O'};
        int[] color = {0x01, 0x03, 0x02};// 这里有个乱序。
        led[5] = (byte) color[num];
        writeBuff(led);
    }

    // 0x04
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

    // 0x05
    private void close(int type) { // "电机","扬声器","LED","显示"
        if (type == -1 || type > 3) {
            LogMgr.e("关闭类型越界");
            return;
        }
        switch (type) {
            case 0:// 关闭电机

                dc0speed = 100;
                dc1speed = 100;
                dc2speed = 100;
                dc3speed = 100;
                sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x14, null, toStm32);
                break;
            case 1:// 关闭扬声器
                mPlayer.stop();
                break;
            case 2:// 关闭LED
                byte[] led = new byte[]{'C', 'S', 'E', 'T', 'L', 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 'O'};
                writeBuff(led);
                break;
            case 3:// 关闭显示 这个先不做。
                SendScratchToBrain(5, displayClose, new byte[2]);//关闭显示要。(字符串，照片。。)
                break;
            default:
                break;
        }
    }

    // 0x06
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x07
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x08
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x09
    private void readColor(int color) {// 颜色与端口没有关系。

        if (color == -1 || color > 4) {
            LogMgr.e("数组越界");
            return;
        }
        // 第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）
        int[] colorArray = new int[10];
        for (int m = 0; m < 10; m++) {
            colorArray[m] = ReadColorValue();// 这里存储6个值。
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    private void readColorOld(byte[] data, int color) {// 颜色与端口没有关系。

        if (color == -1 || color > 4) {
            LogMgr.e("数组越界");
            return;
        }
        // 第二个参数为颜色值：0~4（"红","绿色","蓝色","白色","黄色"）
        int[] colorArray = new int[10];
        for (int m = 0; m < 10; m++) {
            colorArray[m] = ReadColorValue();// 这里存储6个值。
        }
        int sensorColor = getcolor(colorArray);
        int[] colorValue = {1, 2, 3, 5, 6};// 这里与上面的颜色顺序一致。
        int sendvalue = 0;
        if (colorValue[color] == sensorColor) {
            sendvalue = 1;
        } else {
            sendvalue = 0;
        }
        dealwith(sendvalue, data, 1);
    }

    // 0x0A
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x0B 拍照
    private void takepicture(int num) {

        byte[] temp = new byte[2];
        temp[0] = 1;
        temp[1] = (byte) num;
        SendScratch(2, temp);
    }

    //0x0C 时钟复位，系统时间，校准指南针，统统回复 0.
    private void ResponseZero() {

        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x0F
    private void getCompass() {

        int compassValue = 0;
        float[] SN = mSensor.getmO();
        LogMgr.e("yuanshi value " + SN[0]);
        compassValue = (int) SN[0];
        // 这里返回pad端就好了
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(compassValue), 0, sendArray, 4, 4);
        for (int i = 0; i < 5; i++) {
            sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    // 0x10
    private void getPosition(int pos) {
        int value = 0;
        float[] SN = mSensor.getmO();
        switch (pos) {
            case 0://下俯
                if (SN[1] >= -180 && SN[1] <= -95) {//(SN[1] >= 5 && SN[1] <= 90)
                    value = 1;
                }
                break;
            case 1://上仰
                if (SN[1] >= -90 && SN[1] <= -5) {
                    value = 1;
                }
                break;
            case 2://左翻
                if (SN[2] >= 5 && SN[2] <= 90) {
                    value = 1;
                }
                break;
            case 3://右翻
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

    // 0x11
    private void RecordVoice(int num, int time) {
        // 这里面应该这样改。
        byte[] array = new byte[8];
        System.arraycopy(intToByte(num), 0, array, 0, 4);
        System.arraycopy(intToByte(time), 0, array, 4, 4);
        SendScratchToBrain(5, 7, array);//mode用7.

    }

    // 向底层发送信息不带返回值。
    protected void writeBuff(byte[] buff) {
        try {
            LogMgr.e("write: " + Utils.bytesToString(buff));
            request(buff);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
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

    // 返回PAD端及STM32的协议封装。
    /*
	 * to pad 为 0，发送到stm32参数为1.
	 */
    public byte[] sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data,
                             int to) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            buf[0] = type;
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
                byte[] responseData=SP.request(sendbuff,20);
                if (responseData!=null){
                   LogMgr.e("responseData:"+Utils.bytesToString(responseData));
                    return responseData;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    // 获取颜色传感器值
    private int ReadColorValue() {
        byte[] readbuff = new byte[40];
        for (int m = 0; m < 40; m++)
            readbuff[m] = 0x00;

        if(getStmVersion() >= 16973842){//采用的是最新的协议 1.03.0.12之后的

                try{
                    readbuff =  sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x2D, null, toStm32);
                    LogMgr.e("readbuff = " + Utils.bytesToString(readbuff));
                } catch (Exception e){
                   e.printStackTrace();
                }
            if (readbuff == null) {
                readbuff = new byte[40];
            }

            for (int i = 0; i < 20; i++) {
                if(readbuff[i] == (byte) 0xF0 && readbuff[i+1] ==(byte) 0x2C){
                    LogMgr.e("readbuff[i + 6] = " + readbuff[i + 6]);
                    return  (int) readbuff[i + 6];
                }
            }
        }else{//采用的是老协议
            byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'C', 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 'O'};
            for (int n = 0; n < 5; n++) {
                try {
                    readbuff = SP.request(AIBuff);
                    if (readbuff == null) {
                        readbuff = new byte[40];
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }

                for (int i = 0; i < 20; i++) {
                    if (readbuff[i] == 'C') {
                        byte[] tempbuff = new byte[20];
                        System.arraycopy(readbuff, i, tempbuff, 0, 20);
                        System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                        i = 50;
                    }
                }
                // /////////////////////////////////////////////
                LogMgr.e("color: " + Utils.bytesToString(readbuff));
                if (readbuff[0] == 'C' && readbuff[1] == 'G' && readbuff[2] == 'E'
                        && readbuff[3] == 'T' && readbuff[4] == 'C') {
                    // 检测Color返回的HSLC
                    int h = (readbuff[5] & 0xff);
                    int s = (readbuff[6] & 0xff);
                    int l = (readbuff[7] & 0xff);
                    int c = (readbuff[8] & 0xff);
                    if (h == 8 || l > 255) {
                        // NSLog(@"错误");
                        return -1;
                    } else if (h != 8) {
                        if (h > 12 && h <= 45) {
                            if (c < 90) {
                                if (s == 240) {
                                    // self.colorView.randomView.backgroundColor =
                                    // [UIColor yellowColor];
                                    return 4;
                                } else {
                                    // self.colorView.randomView.backgroundColor =
                                    // [UIColor whiteColor];
                                    return 6;
                                }

                            } else {
                                // self.colorView.randomView.backgroundColor
                                // =[UIColor yellowColor];
                                return 4;
                            }
                        } else {
                            if ((h == 0 && s == 0 && l == 240) || (l > 150)) {
                                // self.colorView.randomView.backgroundColor =
                                // [UIColor whiteColor];
                                return 6;
                            } else if (((h >= 0 && h <= 10) || (h >= 200 && h < 255))
                                    && l < 150) {
                                // self.colorView.randomView.backgroundColor =
                                // [UIColor redColor];
                                return 1;
                            } else if (h >= 120 && h < 160 && l < 150) {
                                if (l < 90) {
                                    if (h < 160 && h >= 145) {
                                        // self.colorView.randomView.backgroundColor
                                        // = [UIColor blueColor];
                                        return 3;
                                    } else if (h > 60 && h < 144) {
                                        // self.colorView.randomView.backgroundColor
                                        // = [UIColor greenColor];
                                        return 2;
                                    }
                                } else if (l >= 90) {
                                    if (h < 131 && h >= 120) {
                                        // self.colorView.randomView.backgroundColor
                                        // = [UIColor greenColor];
                                        return 2;
                                    } else if (h > 130) {
                                        // self.colorView.randomView.backgroundColor
                                        // = [UIColor blueColor];
                                        return 3;
                                    }
                                }
                            } else if ((h >= 60 && h <= 119) && l < 110) {
                                // self.colorView.randomView.backgroundColor =
                                // [UIColor greenColor];
                                return 2;
                            } else if (h >= 160 && h <= 180 && l < 80) {
                                // self.colorView.randomView.backgroundColor =
                                // [UIColor blueColor];
                                return 3;
                            }
                        }
                    }

                }
            }
        }


        return -1;
    }

    /**
     *读取指定传感器所在的端口
     * @param type 0 超声 1 碰撞 2 灰度
     * @return 传感器所在端口号
     */
    private int ReadAIType(int type) {
        try {
            int[] AI_type = new int[7];
            int min = 0, max = 0;
            if (type == 0) { // 超声
                min = 1640;
                max = 2260;
            } else if (type == 1) { // 碰撞
                min = 100;
                max = 410;
            } else if (type == 2) { // 灰度
                min = 820;
                max = 1230;
            }
            byte[] readBuff = sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x05, null, toStm32);
            if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 &&
                    readBuff[4] == (byte) 0x01 && readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0x21) {
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
        return 0;
    }

    private int ReadAIValue(int port) {
        int[] AI_value = ReadAIValue();
        if (AI_value != null && port >= 0 && port < AI_value.length) {
            return AI_value[port];
        }
        return 0;
    }

    // 这里写一个获取AI的方法
    private int[] ReadAIValue() {
        int[] AI_value = new int[7];
        try {
            byte[] readBuff = sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x06, null, toStm32);
            if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 &&
                    readBuff[4] == (byte) 0x01 && readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0x22) {
                for (int j = 0; j < AI_value.length; j++) {
                    AI_value[j] = Utils.byte2int_2byteHL(readBuff, 11 + j * 2);
                }
                return AI_value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AI_value;
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
    // 颜色排序。
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

    @Override
    public void clearState() {
        pic_next = 0;
        record_next = 0;
        timeInit = 0;
        mPlayer.stop();
//        motorHandler.removeCallbacks(motorrunnable);
    }

}
