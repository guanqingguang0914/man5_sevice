package com.abilix.control.scratch;

import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.pad.HProtocolDisposer;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.GaitAlgorithm;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class HScratchExecutor extends AbstractScratchExecutor {
    // 数据发送到哪。
    private final int toPad = 0;
    private final int toStm32 = 1;
    private final int toWrite = 2;
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int display_String_mode = 12;
    private final int displayClose = 2;
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;
    private int timeInit = 0;
    private long startTime;
    private int scratchTimes = 0;
    private int walkType=0;
    private int walkSpeed=0;
    public static int[] motor_zero = new int[]{  512, 512, 512, 512, 512, 512, 542, 481, 512, 512, 512, 512,
            719, 305, 818, 205, 512, 512, 512, 512, 512, 512};
    private int value = 0;
    //照片路劲。
    public final static String DATA_PATH = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "Abilix"
            + File.separator + "RobotInfo" + File.separator;
    //存放bin文件的路径
    public final static String BIN_PATH = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "Abilix"
            + File.separator + "MoveBin" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_ = DATA_PATH + "Photo" + File.separator;
    public final static String SCRATCH_VJC_IMAGE_JPG = ".jpg";
    public static final String RECORD_SCRATCH_VJC_ = FileUtils.DATA_PATH
            + File.separator + "Record" + File.separator;
    public static final String RECORD_PATH_3GP = ".3gp";
    protected MySensor mSensor;
    private String NUM_EN ="Number too large to display";
    private String NUM_ZH = "数字过大无法显示";

    public HScratchExecutor(Handler mHandler) {
        super(mHandler);
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come H3:" + Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private void checkFunction(final byte[] data) {
        int parms1 = -1, parms2 = -1, parms3 = -1;
        System.arraycopy(data, 11, id, 0, id.length);
        switch ((data[5]&0xff)){
            case 0x11:
                if (!(System.currentTimeMillis() - startTime > 500)) {
                    return;
                }
                startTime = System.currentTimeMillis();
                stopScratch();
                break;
            default:
                switch (data[6]){
                    case 0x01://(舵机控制)
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        LogMgr.e("parms1:  " + parms1 + "  parms2: " + parms2 + "   parms3" + parms3);
                        // id,角度，速度。
                        motorControlNew(parms1, parms3, parms2);
                        break;
                    case 0x02://(舵机上电)
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 1);
                        break;
                    case 0x03://(舵机释放)
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 2);
                        break;
                    case 0x04://舵机归零
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 3);
                        break;
                    case 0x05://启动显示
                        display(data);
                        break;
                    case 0x06://启动扬声器
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("0x06: parms1 = " + parms1 + ";parms2=" + parms2);
                        playMusic(parms1, parms2);
                        break;
                    case 0x07://设置LED H34
                        byte[] rgb = new byte[3];
                        System.arraycopy(data,16,rgb,0,rgb.length);
                        setLED(rgb);
                        break;
                    case 0x08://关闭  //H34
                        parms1 = getparms(data, 1);
                        close(parms1);
                        break;
                    case 0x09://前方是否有障碍物
                        distance(1);
                        break;
                    case 0x0A://探测障碍物距离
                        distance(0);
                        break;
                    case 0x0B://指南针校准
                        //命令不做指南针校准。
                        ResponseZero();
                        break;
                    case 0x0C://指南针探测角度
                        getCompass();
                        break;
                    case 0x0D://麦克风录音
                        parms1 = getparms(data, 1);// 第一个参数应该是保存9个音频
                        parms2 = getparms(data, 2);// 第二个参数是时间。
                        LogMgr.e("time: " + parms2);
                        if (parms2 > 60) {
                            parms2 = 60;
                        }
                        Record_pre = System.currentTimeMillis();
                        if (Record_pre - record_next >= 1000
                                && Record_pre - record_next >= parms2 * 1000 - 100) {
                            record_next = Record_pre;
                            timeInit = parms2;
                            if (parms2 >= 1) { // 录音时间小于0都不处理。
                                RecordVoice(parms1, parms2);
                            }
                        }
                        break;
                    case 0x0E://拍照
                        pic_pre = System.currentTimeMillis();
                        if (pic_pre - pic_next > 3500) {
                            pic_next = pic_pre;

                            parms1 = getparms(data, 1);
                            takepicture(parms1);
                        }
                        break;
                    case 0x0F://启动扬声器新
                        int length = (int)(  ((data[2]&0xFF)<<8) | (data[3]&0xFF)   );

                        byte[] name = new byte[length -12];
                        System.arraycopy(data, 15, name, 0, name.length);
                        String playName  = null;
                        try {
//                            LogMgr.e("length = " + length + ";;name = " + Utils.bytesToString(name,name.length));
                            playName = new String(name,"utf-8");
                            if (playName.contains("luyin")) {
                                String[] play = playName.split("p");
                                LogMgr.d("play = " + play.toString());
                                String num = play[1];
                                File mfile = new File(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP);
                                if (mfile.exists()) {
                                    //SendScratchToBrain(5, display_String_mode, sendnum);
                                    mPlayer.playRecord(RECORD_SCRATCH_VJC_ + num + RECORD_PATH_3GP);
                                }
                                return;
                            }
                            playName += ".mp3";
                        } catch (Exception e) {
                            LogMgr.d("playName 出错");
                            e.printStackTrace();
                        }
                        LogMgr.d("paly short music playName = "+playName);
                        mPlayer.play(playName);
                        break;
                    case 0x10://运动
                        parms1 = getparms(data, 1);//运动对应的参数
                        LogMgr.d("parms1 = " + parms1);
                        if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                            move(parms1);
                        }else {
                            int speed[] = new int[]{10, 10, 10, 10};
                            GaitAlgorithm gaitAlgorithm = GaitAlgorithm.getInstance();
                            gaitAlgorithm.setWalkSpeed(speed);
                            LogMgr.i("GaitAlgorithm    ");
                            switch (parms1) {
                                case 0x00:
                                    HProtocolDisposer.isInStepState = true;
                                    gaitAlgorithm.startForwardWalk();
                                    break;
                                case 0x01:
                                    HProtocolDisposer.isInStepState = true;
                                    gaitAlgorithm.startBackwardWalk();
                                    break;
                                case 0x02:
                                    HProtocolDisposer.isInStepState = true;
                                    gaitAlgorithm.startLeftWalk();
                                    break;
                                case 0x03:
                                    HProtocolDisposer.isInStepState = true;
                                    gaitAlgorithm.startRightWalk();
                                    break;
                                case 0x04:
                                    HProtocolDisposer.isInStepState = true;
                                    gaitAlgorithm.startTurnLeftWalk();
                                    break;
                                default:
                                    gaitAlgorithm.stopWalk();
                                    break;
                            }
                        }
                        break;
                    case 0x11://停止运动
                        if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                            PlayMoveOrSoundUtils.getInstance().forceStop(false);
                            startSStop();
                        }else {
                            int speed[] = new int[]{10, 10, 10, 10};
                            GaitAlgorithm gaitAlgorithm = GaitAlgorithm.getInstance();
                            gaitAlgorithm.setWalkSpeed(speed);
                            LogMgr.i("GaitAlgorithm    ");
                            try {
                                HProtocolDisposer.isInStepState = false;
                                gaitAlgorithm.stopWalk();
                                Thread.sleep(100);
                                gaitAlgorithm.destoryWalk();
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_BOOT_RECOVER_H;
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
                        }
                        break;
                    case 0x12://陀螺仪探测 0~3 ("下俯","后仰","左翻","右翻")
                        parms1 = getparms(data, 1);
                        getPosition(parms1);
                        break;
                    case 0x13://头部触摸 H56
                        touchH5();
                        break;
                    case 0x14://颜色检测 H56   0x00红色 0x01绿色 0x02蓝色 0x03黑色
                        parms1 = getparms(data, 1);
//                        byte[] array = new byte[8];
//                        System.arraycopy(intToByte(parms1), 0, array, 0, 4);
//                        System.arraycopy(intToByte(parms1), 0, array, 4, 4);
                        byte[] array = new byte[1];
                        array[0] = (byte) parms1;
                        SendScratchToBrain(5, 15, array);//mode用15.
                        break;
                    case 0x15://设置LED等 H56  00额头，01双眼
                        parms1 = getparms(data, 1);
                        byte[] heye = new byte[3];
                        System.arraycopy(data,20,heye,0,3);
                        setH5LED(parms1,heye);
                        break;

                    case 0x16://设置手脚灯光 H56// 第一个参数：0x00左手 0x01右手 0x02双手 0x03左脚 0x04右脚 0x05双脚
                                // 第二个参数：0x00常亮 0x01闪烁
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);

                        setH5HFLED(parms1,parms2);
                        break;
                    case 0x17://关闭 H56
//                        0~9(额头,双眼,左手灯，右手灯，双手灯，左脚灯，右脚灯，双脚灯，扬声器，显示)
                        parms1 = getparms(data, 1);
                        closeH5(parms1);
                        break;
                    case 0x18://步态控制
                        int scratchTimes0 = ((data[13] & 0x00ff) << 8) | (data[14] & 0x00ff);
                        int walkType0=(((data[17] & 0x00ff) << 8) | (data[18] & 0x00ff));
                        int walkSpeed0= ((data[21] & 0x00ff) << 8) | (data[22] & 0x00ff);
                        if(scratchTimes!=scratchTimes0 || walkType!=walkType0  || walkSpeed!=walkSpeed0 ){
                            scratchTimes=scratchTimes0;
                            walkType=walkType0;
                            walkSpeed=walkSpeed0;
                            int speed[] = new int[]{10, 10, 10, 10};
                            for (int i = 0; i < 4; i++) {
                                speed[i] = ((data[21] & 0x00ff) << 8) | (data[22] & 0x00ff);
                            }
                            GaitAlgorithm gaitAlgorithm = GaitAlgorithm.getInstance();
                            gaitAlgorithm.setWalkSpeed(speed);
                            LogMgr.i("GaitAlgorithm    " + (data[18] & 0xff));
                            switch (walkType) {
                                case 0x0:
                                    gaitAlgorithm.destoryWalk();
                                    break;
                                case 0x01:
                                    gaitAlgorithm.startForwardWalk();
                                    break;
                                case 0x02:
                                    gaitAlgorithm.startBackwardWalk();
                                    break;
                                case 0x03:
                                    gaitAlgorithm.startLeftWalk();
                                    break;
                                case 0x04:
                                    gaitAlgorithm.startRightWalk();
                                    break;
                                case 0x05:
                                    gaitAlgorithm.startLeftForwardWalk();
                                    break;
                                case 0x06:
                                    gaitAlgorithm.startRightForwardWalk();
                                    break;
                                case 0x07:
                                    gaitAlgorithm.startLeftBackwardWalk();
                                    break;
                                case 0x08:
                                    gaitAlgorithm.startRightBackwardWalk();
                                    break;
                                case 0x09:
                                    gaitAlgorithm.stopWalk();
                                    break;
                                case 0x0A:
                                    gaitAlgorithm.startTurnLeftWalk();
                                    break;
                                case 0x0B:
                                    gaitAlgorithm.startTurnRightWalk();
                                    break;
                            }
                        }
                        break;
                }
                break;
        }
    }
    /***********************************以下为方法区***********************************/
    private int handOrfooth = -1;
    private int handOrfootf = -1;
    private void move(int bin) {
        //在运动之前先停止运动；
        PlayMoveOrSoundUtils.getInstance().forceStop(false);
        startSStop();
        String moveName = null;
        String musicName = null;
        switch (ControlInfo.getMain_robot_type()){
            case ControlInitiator.ROBOT_TYPE_H3:
                switch (bin){
                    case 0://向前
                        moveName = "H_walk.bin";
//                musicName = "H_walk_1.mp3";
                        break;
                    case 1://后退
                        moveName = "H_backwalk.bin";
//                musicName = "H_backwalk_1.mp3";
                        break;
                    case 2://向左
                        moveName = "H_zuoyi.bin";
//                musicName = "H_turnleft_1.mp3";
                        break;
                    case 3://向右
                        moveName = "H_youyi.bin";
//                musicName = "H_turnright_1.mp3";
                        break;
                    case 4://转体
                        moveName = "H_ztleft.bin";
//                musicName = "H_ztleft_1.mp3";
                        break;
                }
                break;
            default://这个对应的是H5的bin文件
                switch (bin){
                    case 0://向前
                        moveName = "H_walk.bin";
//                musicName = "H_walk_1.mp3";
                        break;
                    case 1://后退
                        moveName = "H_backwalk.bin";
//                musicName = "H_backwalk_1.mp3";
                        break;
                    case 2://向左
                        moveName = "H_zuoyi.bin";
//                musicName = "H_turnleft_1.mp3";
                        break;
                    case 3://向右
                        moveName = "H_youyi.bin";
//                musicName = "H_turnright_1.mp3";
                        break;
                    case 4://转体
                        moveName = "H_ztleft.bin";
//                musicName = "H_ztleft_1.mp3";
                        break;
                }
                break;
        }

        startMove(moveName, musicName, false);
    }

    private void touchH5() {
        int result = 0;
        byte[] receive = sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x6C, null, toStm32);
//        byte[] receive = SP.request(ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x6C, null),200);
        LogMgr.e("receive = " + Utils.bytesToString(receive));
        for (int i = 0; i < 30; i++) {
            if ((receive[i] & 0xFF) == 0xA3 && receive[i + 1] == (byte) 0x69) {
                result = receive[i + 7];
                break;
            }
        }
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(result), 0, sendArray, 4, 4);// 这里写死就是4个字节。
        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void startMove(String moveName, String musicName, boolean isLoop) {
        String filePath = BIN_PATH + moveName;
        String musicPath = BIN_PATH + musicName;
        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(filePath, musicPath, isLoop, false, 0, false,
                PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onPause() {
                        //返回pad端，告知暂停，可继续点击新的模块//暂无
                    }

                    @Override
                    public void onResume() {

                    }

                    @Override
                    public void onStop() {
                        //返回pad端，告知结束，可继续点击新的模块
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
    }

    private void startSStop() {
        try {
            byte[] buffer = new byte[66];

            for (int i = 0; i < 22; i++) {
                buffer[i * 3] = (byte) (i + 1);
                byte[] bs = intToByte(512);
                System.arraycopy(bs, 0, buffer, i * 3 + 1, bs.length);
            }
            byte[] bss = buildProtocol(buffer);
            SP.write(bss);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 构造传输命令
     *
     * @param type 1个字节，代表不同的上层(机器人)型号
     * @param cmd1 1个字节，代表主命令
     * @param cmd2 1个字节，代表子命令
     * @param data 可变长度 null表示不带数据
     * @return
     */
    public static byte[] buildProtocol(byte type, byte cmd1, byte cmd2,
                                       byte[] data) {
//        LogMgr.d("buildProtocol----1");
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[7];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[7 + data.length];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        LogMgr.d("buildProtocol----2");
        return sendbuff;
    }

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
    private void getPosition(int num) {
        // 0~3("下俯","后仰","左翻","右翻")
        if (num == -1 || num > 3) {
            LogMgr.e("参数错误");
            return;
        }
        int value = 0;
        float[] SN = mSensor.getmO();
        LogMgr.i("SN[0] = " + SN[0] +"; SN[1] = " + SN[1] + "; SN[2] = "+ SN[2]);
        switch (num) {
            case 0:
                if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){//H3和H5对应的陀螺仪数值不同
                    if ((SN[1] >= -180 && SN[1] <= -95) && (SN[2] > -10 && SN[2] < 10)) {
                        value = 1;
                    }
                }else {
                    if ((SN[1] >= 30 && SN[1] <= 80) && (SN[2] > -10 && SN[2] < 10)) {
                        value = 1;
                    }
                }

                break;
            case 1:
                if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                    if ((SN[1] >= -75 && SN[1] <= 0)&& (SN[2] > -10 && SN[2] < 10)) {
                        value = 1;
                    }
                }else {
                    if ((SN[1] >= 95 && SN[1] <= 150)&& (SN[2] > -10 && SN[2] < 10)) {
                        value = 1;
                    }
                }

                break;
            case 2:
                if (SN[2] >= -90 && SN[2] <= -5) {
                    value = 1;
                }
                break;
            case 3:
                if (SN[2] >= 5 && SN[2] <= 90) {
                    value = 1;
                }
                break;
            default:
                break;
        }
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(value), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x23, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
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
    private void setH5HFLED(int hand_foot, int status) {//常亮，闪烁

        switch (status){
            case 0://常亮

                switch (hand_foot){

                    case 0://左手
                    case 1://右手
                    case 2://双手
                        if(hand_foot == handOrfooth || hand_foot == 2){
                            stopTimerTemp();
                        }
                        hand_led(hand_foot,status);
                        break;
                    case 3://左脚
                    case 4://右脚
                    case 5://双脚
                        if(hand_foot == handOrfootf || hand_foot == 5){
                            stopTimerTempF();
                        }
                        foot_led(hand_foot,512);
                        break;
                }
                break;
            case 1://闪烁

                switch (hand_foot){
                    case 0:
                    case 1:
                    case 2:
                        handOrfooth = hand_foot;
                        startTimerTemp(hand_foot);
                        break;
                    case 3:
                    case 4:
                    case 5:
                        handOrfootf = hand_foot;
                        startTimerTempF(hand_foot);
                        break;
                }
                break;
        }
    }
    private Timer TempTimerF;
    private TimerTask TempTimerTaskF;
    private  int timesf;
    private void startTimerTempF(final int hand_foot) {
        times = 0;
        stopTimerTempF();
        TempTimerF = new Timer();
        TempTimerTaskF = new TimerTask() {
            @Override
            public void run() {
                switch (hand_foot){
                    case 3:
                    case 4:
                    case 5:
                        if(timesf % 2 == 0){
                            foot_led(hand_foot,512); //脚部512亮度
                        }else {
                            foot_led(hand_foot,0);//脚部熄灭
                        }
                        break;
                }
                timesf++;
            }
        };
        TempTimerF.schedule(TempTimerTaskF,0,1000);
    }
    private Timer TempTimer;
    private TimerTask TempTimerTask;
    private  int times;
    private void startTimerTemp(final int hand_foot) {
        times = 0;
        stopTimerTemp();
        TempTimer = new Timer();
        TempTimerTask = new TimerTask() {
            @Override
            public void run() {
                switch (hand_foot){
                    case 0:
                    case 1:
                    case 2:
                        if(times % 2 == 0){
                            hand_led(hand_foot,0);//常亮
                        }else {
                            hand_led(hand_foot,-1);//熄灭
                        }
                        break;
                }
                times++;
            }
        };
        TempTimer.schedule(TempTimerTask,0,1000);
    }
    private void stopTimerTempF() {
//        hand_led(2,-1);//熄灭
//        foot_led(5,0);
        if (TempTimerF != null) {
            TempTimerF.cancel();
            TempTimerF = null;
        }
        if (TempTimerTaskF != null) {
            TempTimerTaskF.cancel();
            TempTimerTaskF = null;
        }
    }
    private void stopTimerTemp() {
//        hand_led(2,-1);//熄灭
//        foot_led(5,0);
        if (TempTimer != null) {
            TempTimer.cancel();
            TempTimer = null;
        }
        if (TempTimerTask != null) {
            TempTimerTask.cancel();
            TempTimerTask = null;
        }
    }
    //hand_foot:3,4,5,;status:0：熄灭，512 亮度
    private void foot_led(int hand_foot, int status) {
        switch (status){
            case 0://熄灭
                switch (hand_foot){
                    case 3://左脚
                        footLight(23,status);
                        break;
                    case 4://右脚
                        footLight(24,status);
                        break;
                    case 5://双脚
                        try {
                            footLight(23,status);
                            Thread.sleep(20);
                            footLight(24,status);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            default://512默认亮度
                switch (hand_foot){
                    case 3://左脚
                        footLight(23,status);
                        break;
                    case 4://右脚
                        footLight(24,status);
                        break;
                    case 5://双脚
                        try {
                            footLight(23,status);
                            Thread.sleep(20);
                            footLight(24,status);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
        }
    }
    //hand_foot 3:左手灯，4：右手灯，5：双手灯；status：0：熄灭，512，亮度
    private void footLight(int mCurrentID,int angleV){
        byte bChecksum = 0;
        byte[] gSendBuff = new byte[9];
        byte[] angles = new byte[2];
        angles = Utils.intToBytesLH(angleV);
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 5;
        gSendBuff[4] = (byte) 3;
        gSendBuff[5] = (byte) 0x1E;
        System.arraycopy(angles,0,gSendBuff,6,2);
        for (int i = 2; i < gSendBuff.length - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[8] = bChecksum;
        LogMgr.e("gSendBuff = " + Utils.bytesToString(gSendBuff));
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    //hand_foot 0:左手灯，1：右手灯，2：双手灯；status：0：常亮，-1，熄灭
    private void hand_led(int hand_foot, int status) {
//        byte[] led = new byte[3];
//        led[0] = (byte) 0x02;
//        led[1] = (byte) ((hand_foot+1) % 3);
//        led[2] = (byte) (status +1);
//        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC1, led, toWrite);
        switch (status){
            case 0:
                switch (hand_foot){
                    case 0:
                        handLight(1,19);
                        break;
                    case 1:
                        handLight(1,20);
                        break;
                    case 2:
                        handLight(1,19);
                        try {
                            Thread.sleep(20);
                            handLight(1,20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            default://关闭
                switch (hand_foot){
                    case 0:
                        handLight(0,19);
                        break;
                    case 1:
                        handLight(0,20);
                        break;
                    case 2:
                        handLight(0,19);
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        handLight(0,20);
                        break;
                }
                break;
        }
    }
//手部灯光
    private void handLight(int onOrOff, int mCurrentID){
        byte[] gSendBuff = new byte[8];
        byte bChecksum = 0;
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 4 ;
        gSendBuff[4] = (byte) 3;
        gSendBuff[5] = (byte) 0x19;
        gSendBuff[6] = (byte)onOrOff;
        for (int i = 2; i <  7; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[7] = bChecksum;
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    private void DoWriteFrame(byte[] pBuf, int dwLen){
        int iLength = dwLen + 3;//构建数据位
        //高位在前，低位在后
        byte[] sendBuff = new byte[iLength];
        sendBuff[0] = (byte) 0x02;
        sendBuff[1] = (byte) ((dwLen >> 8) & 0xFF);
        sendBuff[2] = (byte) (dwLen & 0xFF);
        System.arraycopy(pBuf, 0, sendBuff, 3, dwLen);
        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0x11,(byte) 0x15, sendBuff, toWrite);
    }
    private void closeH5(int num) {
        byte[] led_H = new byte[15];
        if (num > 9 || num < 0 ){
            LogMgr.e("发送错误");
            return;
        }

        byte[] led = null;
        switch (num){
            case 0://额头
                led = new byte[6];
                byte[] heye = new byte[3];
                led[0] = (byte) 0x02;
                led[1] = 1;
                led[2] = 1;
                System.arraycopy(heye,0,led,3,3);
                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC0, led, toWrite);
                break;
            case 1://双眼
                led = new byte[10];
                byte[] heye2 = new byte[3];
                led[0] = (byte) 0x02;
                led[1] = 2;
                led[2] = 2;
                System.arraycopy(heye2,0,led,3,3);
                led[6] = 3;
                System.arraycopy(heye2,0,led,7,3);
                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC0, led, toWrite);
                break;
            case 2://左手灯
//                stopTimerTemp();
//                led = new byte[3];
//                led[0] = (byte) 0x02;
//                led[1] = 1;
//                led[2] = (byte) 0x00;
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC1, led, toStm32);
            case 3://右手灯
//                stopTimerTemp();
//                led = new byte[3];
//                led[0] = (byte) 0x02;
//                led[1] = 2;
//                led[2] = (byte) 0x00;
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC1, led, toStm32);
            case 4://双手灯
//                stopTimerTemp();
//                led = new byte[3];
//                led[0] = (byte) 0x02;
//                led[1] = 0;
//                led[2] = (byte) 0x00;
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC1, led, toStm32);
                if((num - 2) == handOrfooth || num == 4){
                    stopTimerTemp();
                }
//                hand_led(num + 1,0);
                hand_led(num - 2,-1);
                break;
            case 5://左脚灯
//                stopTimerTemp();
//                led = new byte[4];
//                byte[] lfoot = new byte[2];
//                led[0] = (byte) 0x02;
//                led[1] = 1;
//                System.arraycopy(lfoot,0,led,2,2);
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC2, led, toStm32);
//                break;
            case 6://右脚灯
//                stopTimerTemp();
//                led = new byte[4];
//                byte[] rfoot = new byte[2];
//                led[0] = (byte) 0x02;
//                led[1] = 2;
//                System.arraycopy(rfoot,0,led,2,2);
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC2, led, toStm32);
//                break;
            case 7://双脚灯
                if(((num - 2) == handOrfootf) || num == 7){
                    stopTimerTempF();
                }
                foot_led(num -2,0);
//                stopTimerTemp();
//                led = new byte[4];
//                byte[] bfoot = new byte[2];
//                led[0] = (byte) 0x02;
//                led[1] = 0;
//                System.arraycopy(bfoot,0,led,2,2);
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC2, led, toStm32);
                break;
            case 8://扬声器
                close(1);//关闭扬声器
                break;
            case 9://显示
                close(0);
                break;
        }
        if(num != 8 && num != 9){
            sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x6F, led, toWrite);
        }
    }
    private void setH5LED(int head, byte[] heye) {
        byte[] led = null;
        switch (head){
            case 0://额头灯光
                led = new byte[6];
                led[0] = (byte) 0x02;
                led[1] = 1;
                led[2] = 1;
                System.arraycopy(heye,0,led,3,3);
                break;
            default://双眼灯光
                led = new byte[10];
                led[0] = (byte) 0x02;
                led[1] = 2;
                led[2] = 2;
                System.arraycopy(heye,0,led,3,3);
                led[6] = 3;
                System.arraycopy(heye,0,led,7,3);
                break;
        }
        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC0, led, toStm32);
    }
    //0x09 and 0x10 障碍物距离。0为距离，1为障碍物。
    private void distance(int type) {
        LogMgr.e("type:: " + type);
        //获取超声。
        int result = 0;
        byte[] send = new byte[16];
        byte[] receive = new byte[40];
//        send[0] = (byte)0xFE;
//        send[1] = (byte)0x68;
//        send[2] = (byte)0x5A;
//        send[3] = (byte)0x00;
//        send[4] = (byte)0x00;
//        send[5] = (byte)0x08;
//        send[6] = (byte)0xFF;
//        send[7] = (byte)0xFF;
//        send[8] = (byte)0xC8;
//        send[9] = (byte)0x04;
//        send[10] = (byte)0x02;
//        send[11] = (byte)0x24;
//        send[12] = (byte)0x02;
//        send[13] = (byte)0x0B;
//        send[14] = (byte)0xAA;
//        send[15] = (byte)0x16;
        try {
//            for (int i = 0; i < 3; i++) {
                receive = sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xA3, null, toStm32);
//                Thread.sleep(80);
//            }
            if (receive == null) {
                LogMgr.e("SP.request Error! null");
                return;
            }
            LogMgr.e("readbuff: " + Utils.bytesToString(receive));
            while ((receive[0] & 0xff) == 0x00 ||(receive[1] & 0xff) == 0x00||(receive[5] & 0xff) == 0x00
                    || (receive[6] & 0xff) == 0x00){
                receive = sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xA3, null, toStm32);
                LogMgr.e("readbuff2: " + Utils.bytesToString(receive));
            }
            for (int i = 0; i < 30; i++) {
                if ((receive[i] & 0xFF) == 0xF0 && receive[i + 1] == (byte) 0xA0) {
                    result = (int) (((receive[i+6] & 0xFF) << 8) | receive[i + 7] & 0xFF);
                    break;
                }
            }
            if (result > 200 || result < 0) {
                result = 199;
            }
            if (type == 1) {
                if (result > 0 && result < 20) {
                    result = 1;
                } else {
                    result = 0;
                }
            }
            byte[] sendArray = new byte[8];
            System.arraycopy(id, 0, sendArray, 0, id.length);
            System.arraycopy(intToByte(result), 0, sendArray, 4, 4);// 这里写死就是4个字节。
            sendProtocol((byte) 0x23, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
        }catch(Exception e){
            e.printStackTrace();
        }
//
//
//        byte[] readbuff = null;
//        int dis = 0;
//
//            readbuff =sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xA3, null, toStm32);
//
//        LogMgr.e("readbuff: " + Utils.bytesToString(readbuff, readbuff.length));
//        for (int j = 0; j < 20; j++) {
//
//            if ((readbuff[j] & 0xff) == 0xf0
//                    && (readbuff[j + 1] & 0xff) == 0xA0) {
//                dis = Utils.byte2int_2byteHL(readbuff, j + 6);
//                if (dis > 200 || dis < 0) {
//                    dis = 199;
//                } else if (dis == 0) {
//                }
//            }
//        }
//        LogMgr.e("dis:: " + dis);
//        if (type == 1) {
//            if (dis > 0 && dis < 20) {
//                dis = 1;
//            } else {
//                dis = 0;
//            }
//        }
//        byte[] sendArray = new byte[8];
//        System.arraycopy(id, 0, sendArray, 0, id.length);
//        System.arraycopy(intToByte(dis), 0, sendArray, 4, 4);// 这里写死就是4个字节。
//        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    private void setLED(byte[] rgb) {
        for (int i = 0; i < 2; i++) {
            try {
                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x74, rgb, toWrite);
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void display(byte[] data) {
//		第一个参数为类型：0~2("字符","照片","舵机角度")，
//		（1）当第一个参数为"舵机角度"时，第二个参数为0~22,0为全部
//		（2）当第一个参数为"字符"时，第二个参数为字符串数组（最大19个字节）
//		（3）当第一个参数为"照片"时，第二个参数为0~9
        int type = getparms(data, 1);
        int num = getparms(data, 2);
        LogMgr.e("num:: " + num + "; type = " +type);
        String content = "";
        switch (type) {
            case 2:
                //获取舵机角度。
                String ss = GetAngle(num);
                byte[] senddata = new byte[ss.getBytes().length + 1];
                System.arraycopy(ss.getBytes(), 0, senddata, 1, senddata.length - 1);
                SendScratchToBrain(5, display_String_mode, senddata);
                break;
            case 0://获取字符
                int len = Utils.byte2int_2byteHL(data, 2);
                byte[] displayContext = new byte[len - 16 + 1];
                // 第二个参数从19开始。
                System.arraycopy(data, 19, displayContext, 1, displayContext.length - 1);
                displayContext[0] = 0;
                // 这里应该直接发给brain来显示。
                try {
                    content += new String(displayContext, "UTF-8");
                    LogMgr.e("content: " + content);
                    if(NUM_EN.equals(content) || NUM_ZH.equals(content)){
                        content = FileUtils.isCH() == true ? NUM_ZH : NUM_EN;
                        LogMgr.e("content2: " + content);
                    }
                    byte[] temp = content.getBytes();
                    byte[] temp2 = new byte[temp.length + 1];
                    System.arraycopy(temp, 0, temp2, 1, temp.length);
                    SendScratchToBrain(5, display_String_mode, temp2);//
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                SendScratchToBrain(5, display_String_mode, content.getBytes());//
                break;
            case 1://获取图片
                byte[] sendnum = new byte[5];
                sendnum[0] = 1;
                System.arraycopy(intToByte(num), 0, sendnum, 1, 4);
                File mfile = new File(SCRATCH_VJC_IMAGE_ + num + SCRATCH_VJC_IMAGE_JPG);
                if (mfile.exists()) {
                    SendScratchToBrain(5, display_String_mode, sendnum);
                }
                break;
        }
    }

    private String GetAngle(int id) {//id 为0表示获取所有舵机；id 不为0表示某个；
        if (id < 0 || id > 22) {
            LogMgr.e("id 越界");
            return "id 越界";
        }
        final int[] CAngle = new int[22];//22个舵机角度位置
        String ss = "";
//        byte[] readbuff =sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x61, null, toStm32);//获取所有角度。
//
//        LogMgr.e("readbuff1: " + Utils.bytesToString(readbuff));
//        while ((readbuff[0] & 0xff) == 0x00 ||(readbuff[1] & 0xff) == 0x00||(readbuff[5] & 0xff) == 0x00
//                || (readbuff[6] & 0xff) == 0x00) {
//            readbuff = sendProtocol((byte) 0x23, (byte) 0xA3, (byte) 0x61, null, toStm32);//获取所有角度。
//            LogMgr.e("readbuff2: " + Utils.bytesToString(readbuff));
//        }
//        ss = getAngle(id, readbuff);
        if(id != 0){//单个舵机角度
            int position = 0;//舵机位置
            position = (int) ((float) (ProtocolUtils.getSingleServoPos(id,0) - 512) / (float) 307 * 90);
            ss = position + "";
            return ss;
        }
        for (int i = 0; i < CAngle.length; i++) {
            CAngle[i] = (int) ((float) (ProtocolUtils.getSingleServoPos(i+1,0) - 512) / (float) 307 * 90);
            LogMgr.e("CAngle[i] = " + CAngle[i]);
            ss += (i+1) +":"+ CAngle[i] + " ";
            if((i+1) % 3 == 0){
                ss += "\n";
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ss;
    }
    //type 0:获取舵机角度  1：获取零位偏移量
    private int getSingleServoPos(int mCurrentID,int type){
        int getSingleTimes = 3;
        int getAngle = 0;
        byte bChecksum = 0;
        byte[] gSendBuff = new byte[8];
        byte[] sendBuff = new byte[11];
        sendBuff[0] = (byte) 0x02;
        sendBuff[1] = (byte) 0;
        sendBuff[2] = (byte) 8;
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 4 ;
        gSendBuff[4] = (byte) 2;
        gSendBuff[5] = (byte) 0x24;
        gSendBuff[6] = (byte) 0x02;
        if(type ==1){
            gSendBuff[5] = (byte) 0x0A;
            gSendBuff[6] = (byte) 0x01;
        }
        for (int i = 2; i <  7; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[7] = bChecksum;
        System.arraycopy(gSendBuff,0,sendBuff,3,8);
        while (getSingleTimes> 0 ){
            getSingleTimes --;
            byte[] resquest = SP.request(buildProtocol((byte)0x03,(byte) 0x11,(byte) 0x15,sendBuff),15);
            if(resquest == null){
                LogMgr.d("request = null");
                getAngle = 0;
            }else if( resquest.length >= 21 && (resquest[0] & 0xFF) == 0xAA && (resquest[1] & 0xFF) == 0x55
                    && (resquest[5] & 0xFF) == 0xF0 && (resquest[6] & 0xFF) == 0x0B
                    ){
                if(resquest[16] == (byte) mCurrentID){
                    LogMgr.d("request = " + Utils.bytesToString(resquest));
                    if(type == 0){
//                    getAngle = (resquest[19]  +  resquest[20]<<8);
                        getAngle = (int) (((resquest[20] & 0xFF) << 8) | resquest[19] & 0xFF);
                    }else if(type == 1){
                        getAngle = resquest[19];
                    }
                    break;
                }
            }
        }
        LogMgr.d("getAngle = " + getAngle);
        return getAngle;
    }
    private String getAngle(int id, byte[] readbuff) {
        String ss = "";
        for (int j = 0; j < 20; j++) {

            if ((readbuff[j] & 0xff) == 0xf0
                    && (readbuff[j + 1] & 0xff) == 0x61) {
                if (id != 0) {
                    float value = Utils.byte2int_2byteHL(readbuff, j + 4 + id * 2);
                    int value_T = 0;
                    if(value != 0){
                        value_T = (int) ((float) (value - 512) / (float) 307 * 90);
                    }
//                    int value_T = (int) ((float) (value - 512) / (float) 307 * 90);
                    LogMgr.e("value = " + value + ";value_T = " + value_T);
//							int value_T = (int)(((value - 512)/307)*90);
                    if(value_T >90){
                        value_T = 90;
                    }else if(value_T < -90){
                        value_T = -90;
                    }
                    ss = "" + id + ": " + value_T;
                    return ss;
                } else {
                    int i = 0;
                    for (int m = 1; m <= 22; m++) {
                        int value = 0;
                        float value1 = Utils.byte2int_2byteHL(readbuff, j + 4 + m * 2);
                        if (value1 != 0) {
                            i++;
                            value = (int) ((float) (value1 - 512) / (float) 307 * 90);
                            //添加判斷
                            if(value >90){
                                value = 90;
                            }else if(value < -90){
                                value = -90;
                            }
                            ss += m + ":" + value + "  ";
                            if (i % 3 == 0) {
                                ss += "\n";
                            }
                        }
                    }
                    return ss;
                }

            }else if((readbuff[j] & 0xff) == 0xf0
                    && (readbuff[j + 1] & 0xff) == 0xa2){
                return  GetAngle(id);
            }

        }
        return  "0";
    }


    //上电释放归零写一个函数。
    private void freeAndZero(int id, int type) {//固定  释放  归零。
        if(id == 0){//全部舵机
            switch (type){
                case 1:
                    ProtocolUtils.relAndFix(0, (byte) 0x18);
                    break;
                case 2:
                    ProtocolUtils.relAndFix(1, (byte) 0x18);
                    break;
                case 3:
                    ProtocolUtils.goServePosZeros();
                    break;
            }
        }else{//单个舵机
            switch (type){
                case 1:
                    freeFix(id,1);
                    break;
                case 2:
                    freeFix(id,0);
                    break;
                case 3:
                    goZero(id);
                    break;
            }
        }
//        if (id == 0) {
//            if(type == 3){
//                int type0 =  ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3 ? 2 :3;
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x78, new byte[]{2, (byte) type0,0}, toStm32);//按照H的协议来。
//            }else {
//                byte[] dataALL = new byte[45];
//                dataALL[0] = 22;
//                for (int i = 1; i <= 22; i++) {
//                    dataALL[2 * i - 1] = (byte) i;
//                    dataALL[2 * i] = (byte) type;
//                }
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x65, dataALL, toStm32);//按照H的协议来。
//            }
//        } else {
//            if(type == 3){
//                motorControlNew(id,0,200);
//            }else{
//                byte[] data = new byte[3];
//                data[0] = (byte) 1;//就是一个。
//                data[1] = (byte) id;
//                data[2] = (byte) type;
//                sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x65, data, toStm32);//按照H的协议来。
//            }
//        }
    }
    private void goZero(int mCurrentID){
        byte[] gSendBuff = new byte[9];
        int angle = motor_zero[mCurrentID - 1];
        byte bChecksum = 0;
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 5 ;
        gSendBuff[4] = (byte) 3;
        gSendBuff[5] = (byte) 0x1E;
        gSendBuff[6] = (byte)(angle & 0xff);
        gSendBuff[7] = (byte)((angle & 0xff00) >> 8);
        for (int i = 2; i <  8; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[8] = bChecksum;
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    private void freeFix(int mCurrentID,int type){//type,固定释放
        byte[] gSendBuff = new byte[8];
        byte bChecksum = 0;
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 4 ;
        gSendBuff[4] = (byte) 3;
        gSendBuff[5] = (byte) 0x18;
        gSendBuff[6] = (byte)type;
        for (int i = 2; i <  7; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[7] = bChecksum;
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    private void motorControlNew(int id, int angle, int sudu) {
        byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x00, 0x07, 0x03, (byte) 0x1E, 0x00, 0x00, 0x00, 0x00, 0x00};
        if (sudu == 0) {
            sudu = 1;
        }
        data[2] = (byte) id;
        angle = (int) (205 + (float) 614 / (float) 180 * (angle + 90));
        System.arraycopy(intToBytes(angle), 0, data, 6, 2);
        System.arraycopy(intToBytes(sudu), 0, data, 8, 2);
        byte check = 0;
        for (int i = 2; i < 10; i++) {
            check += data[i];
        }
        data[10] = (byte) ~(check & 0xFF);
        LogMgr.e("电机数据 " + Utils.bytesToString(data));
        DoWriteFrame(data,data.length);
//        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xA5, data, toStm32);
    }

    private void close(int type) { // 0~2("显示","扬声器","LED")
        if (type == -1 || type > 3) {
            LogMgr.e("关闭类型越界");
            return;
        }
        switch (type) {
            case 0:// 关闭显示。
                SendScratchToBrain(5, displayClose, new byte[2]);//关闭显示要。(字符串，照片。。)
                break;
            case 1:// 关闭扬声器
                mPlayer.stop();
                break;
            case 2:// 关闭LED    。
//                byte[] closeLed = new byte[3];
                for (int i = 0; i < 2; i++) {
                    byte[] color = new byte[14];//这里关闭LED灯
                    color[0] = 2;
                    color[1] = 3;
                    color[2] = 1;
                    color[6] = 2;
                    color[10] = 3;
                    color[3] = (byte)0;
                    color[4] = (byte)0;
                    color[5] = (byte)0;
                    sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0xC0, color, toWrite);
                }
                break;
            default:
                break;
        }
    }
    private void ResponseZero() {

        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
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
        sendProtocol((byte) 0x23, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void RecordVoice(int num, int time) {
        // 这里面应该这样改。
        byte[] array = new byte[8];
        System.arraycopy(intToByte(num), 0, array, 0, 4);
        System.arraycopy(intToByte(time), 0, array, 4, 4);
        SendScratchToBrain(5, 7, array);//mode用7.

    }
    private void playMusic(int type, int num) {
        // 数据过滤。
        if (type == -1 || num == -1 || type > 5 || num > 11) {
            LogMgr.e("参数解析错误");
            return;
        }
        switch (type) {
            case 0:// 打招呼 "你好","再见","反对","欢迎","请多关照"
                String[] hello = {"hello.mp3", "bye-bye.mp3", "opposition.mp3", "welcome.mp3", "guanzhao.mp3"};
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
                if (num < face.length) {
                    mPlayer.play(face[num]);
                }
                break;
            case 2:// 动作 0~8（"打寒颤","卖萌","赞成","求抱抱","打哈欠"，"加油","睡觉","休闲","鬼鬼祟祟"）
                String[] action = {"cold.mp3", "cute.mp3", "favor.mp3",
                        "hug_coquetry.mp3", "yawn.mp3", "jiayou.mp3",
                        "sleep.mp3", "Leisure.mp3", "guiguisuisui.mp3"};
                if (num < action.length) {
                    mPlayer.play(action[num]);
                }
                break;
            case 3:// 动物
                String[] animal = {"niu.mp3", "hu.mp3", "haitun.mp3", "ququ.mp3",
                        "yazi.mp3", "mifeng.mp3"};
                if (num < animal.length) {
                    mPlayer.play(animal[num]);
                }
                break;
            case 4:// 乐器
                String[] musicTool = {"1.mp3", "2.mp3", "3.mp3", "4.mp3", "5.mp3", "6.mp3", "7.mp3", "8.mp3"};
                if (num <= musicTool.length) {
                    mPlayer.play(musicTool[num - 1]);
                }
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
    private void takepicture(int num) {
        LogMgr.e("takepicture: " + num);
        byte[] temp = new byte[2];
        temp[0] = 1;
        temp[1] = (byte) num;
        SendScratch(2, temp);
    }
    /*****
     * 第几个参数
     *
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
        }
        return res;
    }

    private void stopScratch() {
        try{
            LogMgr.d("time0");
            record_next = 0;

            if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
                PlayMoveOrSoundUtils.getInstance().forceStop(false);
            }else {
                GaitAlgorithm.getInstance().stopWalk();
                Thread.sleep(100);
                GaitAlgorithm.getInstance().destoryWalk();
            }
//        sendProtocol((byte) 0x23, (byte) 0x11, (byte) 0x08, new byte[]{1,0,0,0,0,0,0,0}, toStm32);
            close(0);//关闭显示
            close(1);//关闭扬声器
            close(2);//关闭LED
            closeH5(0);
            closeH5(1);
            closeH5(4);
            closeH5(7);
//        takepicture(0);//关闭拍照
            SendScratchToBrain(5, 7, new byte[1]);//mode用7.关闭录音界面
//        sendProtocol((byte) 0x23, (byte) 0x11, (byte) 0x08, new byte[]{0,0,0,0,0,0,0,0}, toStm32);
            LogMgr.d("time1");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
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
        LogMgr.e("topad 0r tostm32 ::" + Utils.bytesToString(sendbuff));
        if (to == toPad) {
            SendScratch(1, sendbuff);
        } else if (to == toStm32) {

            try {
                byte[] result = SP.request(sendbuff,100);
                if(result != null){
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(to == toWrite){
            SP.write(sendbuff);
        }
        return new byte[60];
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
    public byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }
    private byte[] intToByte(int value) {
        byte[] res = new byte[4];
        res[3] = (byte) (value & 0xFF);
        res[2] = (byte) ((value >> 8) & 0xFF);
        res[1] = (byte) ((value >> 16) & 0xFF);
        res[0] = (byte) ((value >> 24) & 0xFF);
        return res;
    }
    @Override
    public void clearState() {
        LogMgr.d("clearState");
        pic_next = 0;
        record_next = 0;
        timeInit = 0;
        mPlayer.stop();
//        closeH5(0);
//        closeH5(1);
//        closeH5(4);
//        closeH5(7);
    }
}