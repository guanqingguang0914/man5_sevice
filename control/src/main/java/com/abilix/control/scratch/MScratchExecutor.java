package com.abilix.control.scratch;

import android.hardware.Camera;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.R;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MScratchExecutor extends AbstractScratchExecutor {

    protected MySensor mSensor;

    //这里集成拍照的功能。
    private Camera camera;
    private ByteArrayOutputStream out;
    //音量检测。
    //private AudioVoice mAudioVoice = null;
    private final static int AI_TYPE_ULTRASONIC_FRONT = 0x01; //前端超声距离
    private final static int AI_TYPE_ULTRASONIC_BACK = 0x02; //后端超声距离
    private final static int AI_TYPE_COLLISION = 0x03; //碰撞
    private final static int AI_TYPE_INFRARED = 0x04; //后端红外距离
    private final static int AI_TYPE_DOWN_LOOK = 0x05; //下视
    private final static int AI_TYPE_TOUCH_ARM_LEFT = 0x06; //左臂
    private final static int AI_TYPE_TOUCH_ARM_RIGHT = 0x07; //右臂
    private final static int AI_TYPE_TOUCH_HEAD = 0x08; //头部
    private final static int AI_TYPE_TOUCH_EAR_LEFT = 0x09; //左耳
    private final static int AI_TYPE_TOUCH_EAR_RIGHT = 0x0A; //右耳
    private final static int AI_TYPE_TOUCH_CHEST = 0x0B; //前胸
    private final static int AI_TYPE_TOUCH_BACK = 0x0C; //后背
    private AudioRecordDemo mAudioRecordDemo = null;
    private double value = 0;
    private boolean VoiceFlag = true;
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int display_String_mode = 12;
    private final int toPad = 0;
    private final int toStm32 = 1;
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;
    private int displayClose = 2;
    private String NUM_EN ="Number too large to display";
    private String NUM_ZH = "数字过大无法显示";
    public MScratchExecutor(Handler mHandler) {
        super(mHandler);
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come function" + Utils.bytesToString(data));
//        checkFunction(data);    //老方法，暂时不用，采用最新的协议
        checkFunctionNew(data);
    }

    private void checkFunctionNew(final byte[] data) {
        int parms1 = -1, parms2 = -1, parms3 = -1;
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[5]){
            case 0x11:
                stopScratch();
                break;
            default:
                switch (data[6]){
                    case 0x01://运动 参数为方向：0~1(“向前”,”向后”)，第二个参数为速度值(float类型);
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        motorSet(parms1,parms2);
                        break;
                    case 0x02://左转右转  转速。 两个参数。
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        MrunRotate(parms1,parms2);
                        break;
                    case 0x03://旋转+延时;第一个参数为顺逆时针：0~1("顺时针","逆时针")，第二个参数为转速 (float类型)，第三个参数为时长(float类型);
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
//                        parms3 = getparms(data, 3);
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2 + ";parms3:" + parms3);
                        Mrunwise(parms1,parms2);
                        break;
                    case 0x04://停止运动
                        Mrunstop();
                        break;
                    case 0x05://开启吸尘
//                        parms1 = getparms(data, 1);
                        parms1 = data[18];
                        LogMgr.e("parms1: " + parms1);
                        setVacuumPower(parms1);
                        break;
                    case 0x06://关闭吸尘
                        setVacuumPower(0);
                        break;
                    case 0x07://runneck1  上下
                        byte[] necUpDown = new byte[4];
                        System.arraycopy(data, 15, necUpDown, 0, 4);
                        parms1 = Utils.bytesToInt2(necUpDown, 0);
//                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        SetNeckUPMotor(parms1);
                        break;
                    case 0x08://runneck2 左右
                        byte[] necLR = new byte[4];
                        System.arraycopy(data, 15, necLR, 0, 4);
                        parms1 = Utils.bytesToInt2(necLR, 0);
                        LogMgr.e("parms1: " + parms1);
                        SetNeckLRMotor(parms1);
                        break;
                    case 0x09://播放音乐
                        mPlayer.play();
                        break;
                    case 0x0A://停止播放
                        mPlayer.stop();
                        break;
                    case 0x0B://设置眼睛灯 参数为颜色类型：0~4("灭","红","绿","蓝","全亮")；
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        setEyeColorMode(parms1);
                        break;
                    case 0x0C://设置脖子灯  第一个参数(0-3)代表{红绿蓝白} 第二个参数（0-3）代表{正弦，方波，常亮，常灭}
                        parms1 = getparms(data, 1);
//                        parms2 = getparms(data, 2);
                        parms2 = data[16];
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        Msetled2(parms1,parms2);
                        break;
                    case 0x0D://底部 第一个参数(0-3)代表{红绿蓝白} 第二个参数（0-3）代表{正弦，方波，常亮，常灭}
                        parms1 = getparms(data, 1);
//                        parms2 = getparms(data, 2);
                        parms2 = data[16];
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        Msetled3(parms1,parms2);
                        break;
                    case 0x0E://轮子  第一个参数(0-3)代表{红绿蓝白} 第二个参数（0-3）代表{正弦，方波，常亮，常灭}
                        parms1 = getparms(data, 1);
//                        parms2 = getparms(data, 2);
                        parms2 = data[16];
                        LogMgr.e("parms1: " + parms1 + "   parms2: " + parms2);
                        Msetled4(parms1,parms2);
                        break;
                    case 0x0F://判断前方是否障碍物 第一个参数 0代表身体前部声波传感器，1代表身体后部声波传感器
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        findBarM(parms1);
                        break;
                    case 0x10://判断障碍物距离 第一个参数 0代表身体前部声波传感器，1代表身体后部声波传感器
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        int result = getUltrasonic(parms1);
                        //返回至pad端
                        sendResultToPad(result);
                        break;
                    case 0x11://机器是否碰到物体 第一个参数 0代表左前碰撞，1代表右前碰撞，2代表正前碰撞
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        getCollision(parms1);
                        break;
                    case 0x12://感受到颜色；暂时没有该功能
                        break;
                    case 0x13://第一个参数 （0-4）分别代表（1-5灰度传感器）1号灰度传感器在最左边，5号在最右边
                        parms1 = (data[18] - 1) < 0 ? 0 :  (data[18] - 1);
                        LogMgr.e("parms1: " + parms1);
                        getGroundGray(parms1);
                        break;
                    case 0x14://指南针角度
                        int value = (int) mSensor.getMcompass();
                        sendResultToPad(value);
                        break;
                    case 0x15://陀螺仪 参数为陀螺仪值：0~3 ("下俯","后仰","左翻","右翻")
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        getposM(parms1);
                        break;
                    case 0x16://第一个参数 0代表左悬空，1代表右悬空，2代表左右悬空
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        getDownLook(parms1);
                        break;
                    case 0x17://参数为录音时长，单位为s
                        SendScratchToBrain(5, displayClose, new byte[2]);
                        byte[] temp = new byte[4];
                        System.arraycopy(data, 15, temp, 0, temp.length);
                        int time = Utils.byteAray2Int(temp);
                        LogMgr.e("time = " + time);
                        Record_pre = System.currentTimeMillis();
                        if (Record_pre - record_next > 1000 && Record_pre - record_next > 2 * time * 900) {
                            record_next = Record_pre;
                            if (time > 0) {
                                SendScratch(3, temp);
                            }
                        }
                        break;
                    case 0x18://时钟复位
                    case 0x19://系统时间  这里都是返回0
                    case 0x1D://指南针校准
                        sendResultToPad(0);
                        break;
                    case 0x1A://字符串数组（最大19个字节）
                        display(data);
                        break;
                    case 0x1B://感受到触摸
                        getTouchHead();
                        break;
                    case 0x1C://拍照
                        pic_pre = System.currentTimeMillis();
                        if (pic_pre - pic_next > 3500) {
                            pic_next = pic_pre;
                            byte[] tempic = new byte[1];
                            tempic[0] = 1;
                            SendScratch(2, tempic);
                        }
                        break;
                    case 0x1E://(扬声器模拟动物)参数为声音类型：0~5("牛","虎","海豚","蟋蟀","鸭","飞虫")
                    case 0x1F://扬声器模拟乐器)参数为声音类型：0~5("萨克斯","钢琴","鼓","大提琴","中号","吉他")
                    case 0x20://扬声器自我介绍)参数为声音类型：0~7("介绍1","介绍2","介绍3","介绍4","介绍5","介绍6","介绍7","介绍8")
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        playMusic(data[6],parms1);
                        break;
                    case 0x21://(控制轮子运动)第一个参数为左轮速度值(float类型)，第二个参数为右轮速度值(float类型)
                        //暂时应无此需求
                        byte[] left = new byte[4];
                        System.arraycopy(data, 15, left, 0, 4);
                        int leftspeed = Utils.bytesToInt2(left, 0);
                        if (leftspeed > 100) {
                            leftspeed = 100;
                        } else if (leftspeed < -100) {
                            leftspeed = -100;
                        }
                        byte[] right = new byte[4];
                        System.arraycopy(data, 19, right, 0, 4);
                        int rightspeed = Utils.bytesToInt2(right, 0);
                        if (rightspeed > 100) {
                            rightspeed = 100;
                        } else if (rightspeed < -100) {
                            rightspeed = -100;
                        }
                        setMotorSpeed(leftspeed,rightspeed);
                        break;
                    case 0x22://感受到触摸)(适用M3S,M4S 第一个参数 0头部，1前方，2后方,3左耳，4右耳，5左臂，6右臂
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        getTouchM3S(parms1);
                        break;
                    case 0x23://判断机器人是否悬挂)(适用M3S,M4S 0代表后左，1代表后中，2代表后右，3代表前左，4代表前右, 5代表全悬挂
                        parms1 = getparms(data, 1);
                        LogMgr.e("parms1: " + parms1);
                        getDownLookM3S(parms1);
                        break;
                }
                break;
        }
    }
    /**
     *下视传感器是否悬空
     * @param type 下视传感器类型：0x00代表后左，0x01代表后中,0x02代表后右,0x03代表前左,0x04代表前右；0x05代表全部悬空；0x06代表全部不悬空
     * @return 0 否 1是
     */
    private void getDownLookM3S(int type) {
        //第4字节,使用低五位表示下视 bit0代表后左，bit1代表后中,bit2代表后右,bit3代表前左,bit4代表前右；0为没悬空，1为悬空；
        //即00000000没有悬空，00000001后左悬空，00000010后中悬空，00000100后右悬空，00001000前左悬空，00010000前右悬空。
        int value = getAIValue(AI_TYPE_DOWN_LOOK);
        int returnValue = 0;
        switch (type) {
            case 0x00://代表后左
                returnValue = value & 0x01;
                break;
            case 0x01://代表后中
                returnValue = (value >> 1) & 0x01;
                break;
            case 0x02://代表后右
                returnValue = (value >> 2) & 0x01;
                break;
            case 0x03://代表前左
                returnValue = (value >> 3) & 0x01;
                break;
            case 0x04://代表前右
                returnValue = (value >> 4) & 0x01;
                break;
            case 0x05://代表全部悬空
                returnValue = (value == (0x1F & 0xFF)) ? 1 : 0;
                break;
            case 0x06://代表全部不悬空
                returnValue = (value == 0x00) ? 1 : 0;
                break;
        }
        sendResultToPad(returnValue);
    }

    private void getTouchM3S(int type) {
        int value = 0;
        switch (type) {
            case 0x00://0头部
                value = getAIValue(AI_TYPE_TOUCH_HEAD);
                break;
            case 0x01://1前方
                value = getAIValue(AI_TYPE_TOUCH_CHEST);
                break;
            case 0x02://2后方
                value = getAIValue(AI_TYPE_TOUCH_BACK);
                break;
            case 0x03://3左耳
                value = getAIValue(AI_TYPE_TOUCH_EAR_LEFT);
                break;
            case 0x04://4右耳
                value = getAIValue(AI_TYPE_TOUCH_EAR_RIGHT);
                break;
            case 0x05://5左臂
                value = getAIValue(AI_TYPE_TOUCH_ARM_LEFT);
                break;
            case 0x06://6右臂
                value = getAIValue(AI_TYPE_TOUCH_ARM_RIGHT);
                break;
        }
        sendResultToPad(value);
    }

    private void playMusic(byte type, int num) {
        switch (type){
            case 0x1E:
                String[] animal = {"niu.mp3", "hu.mp3", "haitun.mp3", "ququ.mp3",
                        "yazi.mp3", "mifeng.mp3"};
                if (num < animal.length) {
                    mPlayer.play(animal[num]);
                }
                break;
            case 0x1F:
                String[] musicTool = {"sakesi.mp3", "gangqin.mp3", "gudian.mp3",
                        "datiqin.mp3", "xiaohao.mp3", "jita.mp3"};
                if (num <= musicTool.length) {
                    mPlayer.play(musicTool[num]);
                }
                break;
            case 0x20:
                String[] musicSelf = {"changedansw1.mp3", "changedansw2.mp3", "changedansw3.mp3",
                        "changedansw4.mp3", "changedansw5.mp3", "changedansw6.mp3", "changedansw7.mp3", "changedansw8.mp3"};
                if (num <= musicSelf.length) {
                    mPlayer.play(musicSelf[num]);
                }
                break;
        }
    }

    private void getTouchHead() {
        int value = getAIValue(AI_TYPE_TOUCH_HEAD);
        sendResultToPad(value);
    }

    /**
     *下视传感器是否悬空
     * @param type 下视传感器类型：0代表是否左悬空，1代表是否右悬空，2代表是否左右悬空
     * @return 0 否 1是
     */
    private void getDownLook(int type) {
        //下视传感器：0代表后左右没有悬空，1代表后左有悬空，2代表后右有悬空，3代表左右悬空
        int value = getAIValue(AI_TYPE_DOWN_LOOK);
        int returnValue = 0;
        switch (type) {
            case 0x03://没有悬空
                returnValue = (value == 0x00) ? 1 : 0;
                break;
            case 0x00://后左有悬空
                returnValue = value & 0x01;
                break;
            case 0x01://后右有悬空
                returnValue = (value >> 1) & 0x01;
                break;
            case 0x02://左右悬空
                returnValue = (value == 0x03) ? 1 : 0;
                break;
        }
        sendResultToPad(returnValue);
    }

    private void getposM(int num) {
        int value = 0;
        float[] SN = mSensor.getmO();
        LogMgr.e("SN value is : " + (SN == null ? ("SN == null"):SN.toString()));
        // 上下是value[1].下正 上负。
        if (SN != null) {
            // 对于上扬 下府这个值是需要减去70的。
            if (num == 0) {
                // 对于下府取10~90度之间。  //bug 11637 把5改成4（暂时）
                if (SN[1] >= (4 + 70) && SN[1] <= (90 + 70)) {
                    value = 1;
                }
                // 上扬。
            } else if (num == 1) {
                // 这里应该取-10 ~ -90之间。 基础值是70度。
                if (SN[1] >= (70 - 90) && SN[1] <= (70 - 10)) {
                    value = 1;
                }
                // 左翻，右翻。 左正右负。 左右翻转的值是对的。
            } else if (num == 2) {
                if (SN[2] >= 5 && SN[2] <= 90) {
                    value = 1;
                }
                // 右翻。
            } else if (num == 3) {
                if (SN[2] >= -90 && SN[2] <= -5) {
                    value = 1;
                }
            }
            LogMgr.e("上下 vlaue[1] is: " + SN[1] + "左右value[2] is: " + SN[2]);
        }
        sendResultToPad(value);
    }

    private void getGroundGray(int id) {
        int reslt = 0;
        try {
            byte[] readBuff = new byte[30];
            for (int i = 0; i < 2; i++) {
                readBuff = sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x38, null,toStm32);
                Thread.sleep(500);
            }
            for (int i = 0; i < readBuff.length; i++) {
                if (readBuff[i] == (byte) 0xAA && readBuff[i + 1] == (byte) 0x55 &&
                        readBuff[i + 5] == (byte) 0xF0 && readBuff[i + 6] == (byte) 0x31) {
                    reslt =  readBuff[i +  11 + id];
//                    i = 30;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendResultToPad(reslt);
    }

    /**
     * 是否检测到碰撞
     * @param type 碰撞类型：0代表左前碰撞，1代表右前碰撞，2代表正前碰撞
     * @return 0 无碰撞 1 检测到碰撞
     */
    private void getCollision(int type) {
        int value = getAIValue(AI_TYPE_COLLISION);
        int returnValue = 0;
        switch (type) {
            case 0x00://左碰撞
                if (value == 1) {
                    returnValue = 1;
                }
                break;
            case 0x01://右碰撞
                if (value == 2) {
                    returnValue = 1;
                }
                break;
            case 0x02://前碰撞
                if (value == 3) {
                    returnValue = 1;
                }
                break;
        }
        //返回至pad端
        sendResultToPad(returnValue);
    }

    private void sendResultToPad(int returnValue) {
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(returnValue), 0, sendArray, 4, 4);// 这里写死就是4个字节。
        sendProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    /**
     *前方是否有障碍物
     * @param type 超声类型：0前超声，1后超声
     * @return 0 否 1 是
     *
     */
    private void findBarM(int type) {
        int value = getUltrasonic(type);
        int result = 0;
        if (value > 0 && value < 30) {
            result = 1;
        }
        //返回至pad端
        sendResultToPad(result);
    }

    private byte[] intToByte(int value) {
        byte[] res = new byte[4];
        res[3] = (byte) (value & 0xFF);
        res[2] = (byte) ((value >> 8) & 0xFF);
        res[1] = (byte) ((value >> 16) & 0xFF);
        res[0] = (byte) ((value >> 24) & 0xFF);
        return res;
    }
    /**
     * 超声距离
     * @param type 超声类型：0前超声，1后超声
     * @return int 超声距离
     */
    public int getUltrasonic(int type) {
        if (type == 0) {
            return getAIValue(AI_TYPE_ULTRASONIC_FRONT);
        } else {
            return getAIValue(AI_TYPE_ULTRASONIC_BACK);
        }
    }
    private int getAIValue(int type) {
        try {
            byte[] readBuff = sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x37, null,toStm32);// 发送获取传感器协议。
            if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 && readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0x30) {
                /*8字节：第1字节代表前端超声距离；第2字节代表后端超声距离；第3字节代表碰撞：0代表没有碰撞，1代表左碰撞，2代表右碰撞，3代表前碰撞；
                第4字节代表触摸：0代表没有触摸，1代表有触摸；第5字节代表下视：0代表后左右没有悬空，1代表后左有悬空，2代表后右有悬空，3代表左右悬空；
                第6-7字节代表后端红外距离值。*/
                //aa 55 00 0f 02 f0 30 00 00 00 00 6f 06 00 01 00 0b cf b1
                int value = -1;
                switch (type) {
                    case AI_TYPE_ULTRASONIC_FRONT:
                        value = readBuff[11] & 0xFF;
                        break;
                    case AI_TYPE_ULTRASONIC_BACK:
                        value = readBuff[12] & 0xFF;
                        break;
                    case AI_TYPE_COLLISION:
                        //TODO
                        value = readBuff[13] & 0xFF;
                        break;
                    case AI_TYPE_INFRARED:
//                        value = ByteUtils.byte2int_2byteHL(readBuff, 16);
                        value = Utils.byteToIntHL(readBuff,16);
                        break;
                    case AI_TYPE_DOWN_LOOK://下视
                        value = readBuff[15] & 0xFF;
                        break;
                    case AI_TYPE_TOUCH_HEAD:
                        value = readBuff[14] & 0xFF;
                        break;
                    default:
                        value = -1;
                        break;
                }
                return value;
            } else if (readBuff[0] == (byte) 0xAA && readBuff[1] == (byte) 0x55 && readBuff[5] == (byte) 0xF0 && readBuff[6] == (byte) 0x35) {
                /*"7字节：
                第1字节：前超声距离
                第2字节：后超声距离
                第3字节,碰撞 0 无碰撞，1表示左碰撞，2右碰撞，3前碰撞
                第4字节,使用低五位表示下视 bit0代表后左，bit1代表后中,bit2代表后右,bit3代表前左,bit4代表前右；0为没悬空，1为悬空；
                即00000000没有悬空，00000001后左悬空，00000010后中悬空，00000100后右悬空，00001000前左悬空，00010000前右悬空。等等
                第5-6字节,红外测距值（高位在前，低位在后）
                第7字节：低7位表示触摸传感器 依次为左臂，右臂，头部，左耳，右耳，前胸，后背。0为没触摸，1为有触摸。"*/
                int value = -1;
                switch (type) {
                    case AI_TYPE_ULTRASONIC_FRONT:
                        value = readBuff[11] & 0xFF;
                        break;
                    case AI_TYPE_ULTRASONIC_BACK:
                        value = readBuff[12] & 0xFF;
                        break;
                    case AI_TYPE_COLLISION:
                        value = readBuff[13] & 0xFF;
                        break;
                    case AI_TYPE_INFRARED:
                        value = Utils.byteToIntHL(readBuff,15);
                        break;
                    case AI_TYPE_DOWN_LOOK:
                        //TODO
                        value = readBuff[14] & 0xFF;
                        break;
                    case AI_TYPE_TOUCH_ARM_LEFT:
                        value = readBuff[17] >> 6 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_ARM_RIGHT:
                        value = readBuff[17] >> 5 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_HEAD:
                        value = readBuff[17] >> 4 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_EAR_LEFT:
                        value = readBuff[17] >> 3 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_EAR_RIGHT:
                        value = readBuff[17] >> 2 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_CHEST:
                        value = readBuff[17] >> 1 & 0x01;
                        break;
                    case AI_TYPE_TOUCH_BACK:
                        value = readBuff[17] & 0x01;
                        break;
                }
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void Msetled4(int color, int mode) {
        setColorMode(0x02, color, mode);
    }

    private void Msetled3(int color, int mode) {
        setColorMode(0x04, color, mode);
    }

    private void Msetled2(int color, int mode) {
        setColorMode(0x01, color, mode);
    }
    /**
     *
     * @param part 0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param colorSet (0-3)代表{红绿蓝白}
     * @param mode （0-3）代表{正弦，方波，常亮，常灭}
     */
    private void setColorMode(int part, int colorSet, int mode) {
        int r = 0;
        int g = 0;
        int b = 0;
        switch (colorSet) {
            case 0x00:// 红
                r = 0xFF;
                g = 0x00;
                b = 0x00;
                break;
            case 0x01:// 绿
                r = 0x00;
                g = 0xFF;
                b = 0x00;
                break;
            case 0x02:// 蓝
                r = 0x00;
                g = 0x00;
                b = 0xFF;
                break;
            case 0x03:// 白
                r = 0xFF;
                g = 0xFF;
                b = 0xFF;
                break;
        }
        setColor(part, r, g, b);
        setWave(part, mode + 1);
    }
    /**
     * @param mode     0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param wavemode 0x1 正弦波 0x2 宽波 0x3 高电平 0x4 低电平
     */
    private void setWave(int mode, int wavemode) {
        byte[] data = new byte[2];//"2字节：第1字节代表灯光模式：1脖子灯光模式，2轮子灯光模式，3底部灯光模式，
        //第2字节代表波形模式：1代表正弦波，2代表宽波，3代表高平，4代表低电平。"
        switch (mode) {
            case 0x01://脖子
                data[0] = 0x01;
                break;
            case 0x02:
            case 0x03://轮子
                data[0] = 0x02;
                break;
            case 0x04://底部
                data[0] = 0x03;
                break;
        }
        data[1] = (byte) wavemode;
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x36, data,toStm32);
    }

    /**
     * @param mode 0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param r
     * @param g
     * @param b
     */
    public void setColor(int mode, int r, int g, int b) {
        byte[] data = new byte[3];
        data[0] = (byte) r;
        data[1] = (byte) g;
        data[2] = (byte) b;
        switch (mode) {
            case 0x01://脖子
               sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x32, data,toStm32);
                break;
            case 0x02:
            case 0x03://轮子
                sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x34, data,toStm32);
                break;
            case 0x04://底部
                sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x33, data,toStm32);
                break;
        }
    }
    /**
     *设置眼睛LED
     * @param colorSet 颜色类型：0~4("灭","红","绿","蓝","全亮")
     */
    public void setEyeColorMode(int colorSet) {
        int r = 0;
        int g = 0;
        int b = 0;
        switch (colorSet) {
            case 0x00:// 灭
                r = 0x00;
                g = 0x00;
                b = 0x00;
                break;
            case 0x01:// 红
                r = 0xFF;
                g = 0x00;
                b = 0x00;
                break;
            case 0x02:// 绿
                r = 0x00;
                g = 0xFF;
                b = 0x00;
                break;
            case 0x03:// 蓝
                r = 0x00;
                g = 0x00;
                b = 0xFF;
                break;
            case 0x04:// 全亮
                r = 0xFF;
                g = 0xFF;
                b = 0xFF;
                break;
        }
        setEyeColor(16, r, g, b);
    }
    // 按新协议发眼睛协议
    public void setEyeColor(int count, int r, int g, int b) {
        // 48字节：分别代表眼睛1~16号LED的红绿蓝值。
        byte[] data = new byte[48];
        for (int i = 0; i < count; i++) {
            data[i * 3 + 0] = (byte) r;
            data[i * 3 + 1] = (byte) g;
            data[i * 3 + 2] = (byte) b;
        }
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x31, data,toStm32);
    }

    private void SetNeckLRMotor(int progress) {
        progress += 130;
        setNeckMotor(0, progress);
    }
    private void SetNeckUPMotor(int progress) {
        progress += 15;
        setNeckMotor(1, progress);
    }
    /**
     *
     * @param mode 0代表左右电机，1代表俯仰电机
     * @param angle 角度值：俯仰电机角度范围-15～+35（发送值+15），左右电机角度范围 -130～+130（发送值+130）
     */
    private void setNeckMotor(int mode, int angle) {
        byte[] data = new byte[3];
        data[0] = (byte) mode;
        data[1] = (byte) (angle >> 8 & 0xFF);
        data[2] = (byte) (angle & 0xFF);
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x3A, data,toStm32);
    }
    private void setVacuumPower(int power) {
        byte[] data = new byte[1];
        data[0] = (byte) power;
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x39, data,toStm32);
    }
    private void Mrunstop() {
        setMotorSpeed(0, 0);
    }
    private void Mrunwise(int direction, int speed) {
        if (direction == 0) {//顺时针
            setMotorSpeed(speed, -speed);
        } else {//逆时针
            setMotorSpeed(-speed, speed);
        }
    }
    private void MrunRotate(int direction, int speed) {
        if (direction == 0) {
            setMotorSpeed(0, speed);
        } else {
            setMotorSpeed(speed, 0);
        }
    }
    private void motorSet(int drection, int speed) {
        if (Math.abs(drection) > 1) {
            LogMgr.e("参数解析错误");
            return;
        }
        int sign = (drection == 0) ? 1 : -1;
        speed *= sign;
        setMotorSpeed(speed, speed);
    }
    private void setMotorSpeed(int leftSpeed, int rightSpeed) {
        setNewAllWheelMoto(0, 1, leftSpeed, 1, rightSpeed, 0, 0);
    }
    /**
     *
     * @param type 类型(0-闭环 1-位移 2-开环)
     * @param leftSet 左轮子是否设置(0-不设置，1-设置
     * @param leftSpeed 左轮子速度 范围为-100~100
     * @param rightSet 右轮子是否设置(0-不设置，1-设置
     * @param rightSpeed 右轮子速度 范围为-100~100
     * @param leftDis 左轮子位移(0-4095)单位cm
     * @param rightDis 右轮子位移(0-4095)单位cm
     */
    public void setNewAllWheelMoto(int type, int leftSet, int leftSpeed, int rightSet, int rightSpeed, int leftDis, int rightDis) {
        // 数据越界限制。
        if (leftSpeed > 100) {
            leftSpeed = 100;
        } else if (leftSpeed < -100) {
            leftSpeed = -100;
        }
        if (rightSpeed > 100) {
            rightSpeed = 100;
        } else if (rightSpeed < -100) {
            rightSpeed = -100;
        }

        byte[] data = new byte[8];
        if (type == 2) { // 类型(0-闭环 1-位移 2-开环) 解决开环速度过小电机不转问题
            leftSpeed = checkOpenLoopSpeed(leftSpeed);
            rightSpeed = checkOpenLoopSpeed(rightSpeed);
            LogMgr.d(String.format(Locale.US, "OpenLoopSpeed{%d, %d}", leftSpeed, rightSpeed));
        }
        //0x11两个电机，0x10左电机,0x01右电机。
        data[0] = (byte) ((leftSet & 0x01) << 1 | rightSet & 0x01);
        data[1] = (byte) ((leftSpeed + 100) & 0xff);
        data[2] = (byte) ((rightSpeed + 100) & 0xff);
        data[3] = (byte) (type & 0xff);
        data[4] = (byte) ((leftDis >> 8) & 0xff);
        data[5] = (byte) (leftDis & 0xff);
        data[6] = (byte) ((rightDis >> 8) & 0xff);
        data[7] = (byte) (rightDis & 0xff);
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x41, data,toStm32);
    }
    private int checkOpenLoopSpeed(int speed) {
        if (speed == 0) {
            return 0;
        }

        int preSign = speed < 0 ? -1 : 1;
        return (8 * speed) / 10 + preSign * 20;
    }
    /*************************函数方法*************************************/
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
            // 第一个参数从15,
            if (data.length > 15) {
//                System.arraycopy(data, 15, array, 0, 1);
                res = data[15] & 0xFF;
            }
        } else if (n == 2) {
            if (data.length > 19) {//
                System.arraycopy(data, 16, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        } else if (n == 3) {
            if (data.length > 23) {
                System.arraycopy(data, 20, array, 0, array.length);
                res = Utils.byteAray2Int(array);
            }
        }
        return res;
    }

    private void checkFunction(final byte[] data) {
        //LogMgr.e("1111111111111111111" + Thread.currentThread().getName());
        // 运动向前 向后。----------------------------------------ok
        if (data[6] == 0x01) {
            //flag = false;
            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] sudubuff = new byte[4];
            System.arraycopy(data, 16, sudubuff, 0, sudubuff.length);
            int speed = Utils.byteAray2Int(sudubuff);
            LogMgr.e(" " + speed);
            if (speed > 100) {
                speed = 100;
            } else if (speed < -100) {//因为这里前后已经有方向了。范围就是0~100。
                speed = -100;
            }
            if (data[15] == 0x00) {

                speed = (byte) (speed + 100);
                motor[8] = (byte) speed;
                motor[9] = (byte) speed;
                // 向后
            } else if (data[15] == 0x01) {

                speed = (byte) (100 - speed);
                motor[8] = (byte) speed;
                motor[9] = (byte) speed;
            }
            writeBuff(motor);
            // 左转 右转 转速-------------------------------------ok
        } else if (data[6] == 0x02) {
            //flag = false;
            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // 这里是速度 速度只有一个byte 那么只取最后一个byte 16 17 18 19
            byte[] sudubuff = new byte[4];
            System.arraycopy(data, 16, sudubuff, 0, sudubuff.length);
            int speed = Utils.byteAray2Int(sudubuff);
            if (speed > 100) {
                speed = 100;
            } else if (speed < -100) {
                speed = -100;
            }

            // 左转。
            if (data[15] == 0x00) {

                speed = (byte) (speed + 100);
                motor[9] = (byte) speed;
                // 左转左轮停止
                motor[8] = (byte) 100;
                // 右转
            } else if (data[15] == 0x01) {

                speed = (byte) (100 + speed);
                // 右转右轮停止。
                motor[9] = (byte) 100;
                motor[8] = (byte) speed;
            }
            writeBuff(motor);
            // 顺时针 逆时针 转速 时长。------------------------------------这个指令有问题已改。
        } else if (data[6] == 0x03) {

            // flag = true;
            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            // 顺时针
            if (data[15] == 0x00) {
                // 速度
                byte speedleft = (byte) (data[19] + 100);
                byte speedright = (byte) (100 - data[19]);

                motor[8] = (byte) speedleft;
                motor[9] = (byte) speedright;
                // 逆时针
            } else if (data[15] == 0x01) {

                byte speedleft = (byte) (data[19] + 100);
                byte speedright = (byte) (100 - data[19]);

                motor[8] = (byte) speedright;
                motor[9] = (byte) speedleft;
            }
            writeBuff(motor);
            try {
                LogMgr.d("((int)data[22]*256+(int)data[23])* 1000 = " + ((int) data[22] * 256 + (int) data[23]) * 1000);
                Thread.sleep(((int) data[22] * 256 + (int) data[23]) * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 这里的stop 左右轮速度都设为0.
            byte[] motorstop = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x64, 0x64, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeBuff(motorstop);

            // 停止运动。
        } else if (data[6] == 0x04) {

            //flag = false;
            byte[] motorstop = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x64, 0x64, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeBuff(motorstop);
            // 开启吸尘器。
        } else if (data[6] == 0x05) {
            // p1 = getParam(str, 2);

            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x04, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            motor[8] = data[18]; // 这里是功率。测试18看看
            writeBuff(motor);
            // 关闭吸尘器。
        } else if (data[6] == 0x06) {
            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x04, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            writeBuff(motor);
            // 脖子上下俯仰------------------------------------------------有问题更可能是设备问题。
        } else if (data[6] == 0x07) {

            // p1 = getParam(str, 2); // 这个应该获取的是角度。
            LogMgr.e("666666666666666666   " + data[18]);
            byte[] nec = new byte[4];
            System.arraycopy(data, 15, nec, 0, 4);
            int angle = Utils.bytesToInt2(nec, 0);

            byte[] motor = new byte[]{0x55, 'N', 'E', 'C', 'K', 'M', 'O',
                    'T', 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            motor[9] = (byte) (angle + 15); // 这里是角度。
            //做一个阈值限定的过滤。
            if (motor[9] > 45) {
                motor[9] = 45;
            } else if (motor[9] < 0) {
                motor[9] = 0;
            }
            writeBuff(motor);
            // 脖子左右转动。---------------------------------------------ok.
        } else if (data[6] == 0x08) {

            byte[] motor = new byte[]{0x55, 'N', 'E', 'C', 'K', 'M', 'O',
                    'T', 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // motor[9] = (byte) Integer.parseInt(p1);
            // 这里写一个角度的转换。
            // float sp = Utils.byte2float(data, 16);
            // int spw = Utils.
            byte[] nec = new byte[4];
            System.arraycopy(data, 15, nec, 0, 4);
            int angle = Utils.bytesToInt2(nec, 0);

            angle += 130;
            if (angle >= 256) {
                if (angle == 256) {
                    motor[10] = 1;
                    motor[9] = 0;
                } else {
                    motor[10] = 1;
                    motor[9] = (byte) (angle - 256);
                }
            } else {
                motor[9] = (byte) angle;
                motor[10] = 0;
            }

            motor[9] = (byte) angle;
            writeBuff(motor);
            // 播放音乐
        } else if (data[6] == 0x09) {
            //LogMgr.e("111111111111");
            mPlayer.play();
            // 停止播放
        } else if (data[6] == 0x0A) {
            mPlayer.stop();
            // 眼睛LED。
        } else if (data[5] == 0x05 && data[6] == 0x0B) {
            // p1 = getParam(str, 2); // 这是RGB三个颜色。
            // LogMgr.e("参数p1 "+p1 +"function "+strFun);
            byte[] motor = new byte[]{0x55, 'C', 'O', 'L', 'O', 'R', 0x00, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // 灭 红 绿 蓝 全亮
            if (data[15] == 0x00) {
                motor[7] = 0x00;
                // 红
            } else if (data[15] == 0x01) {
                motor[7] = (byte) 0xff;
                // 绿
            } else if (data[15] == 0x02) {
                motor[8] = (byte) 0xff;
                // 蓝
            } else if (data[15] == 0x03) {
                motor[9] = (byte) 0xff;
                // 全亮
            } else if (data[15] == 0x04) {
                motor[7] = (byte) 0xff;
                motor[8] = (byte) 0xff;
                motor[9] = (byte) 0xff;
            }
            writeBuff(motor);
            // 脖子LED
        } else if (data[6] == 0x0C) {

            LogMgr.e("bozi:" + "color: " + data[15] + "type: " + data[16]);
            ledset(1, data[15], data[16]);
            // 底部LED
        } else if (data[6] == 0x0D) {

            LogMgr.e("底部:" + "color: " + data[15] + "type: " + data[16]);
            ledset(2, data[15], data[16]);
            // 轮子LED
        } else if (data[6] == 0x0E) {

            LogMgr.e("轮子:" + "color: " + data[15] + "type: " + data[16]);
            ledset(3, data[15], data[16]);

        } else if (data[6] == 0x0F) {

            byte[] motor = new byte[]{0x56, 'A', 'I', 'R', 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] returnvalue = ReadAIValue(motor);

            int pre_dis = 0;
            int next_dis = 0;
            int hight = 0;
            int low = 0;
            hight = returnvalue[4];
            low = returnvalue[5];
            if (hight > 0) {
                hight = hight << 8;
            }
            pre_dis = hight + (low & 255);
            // 后端超声障碍物。
            hight = 0;
            low = 0;
            hight = returnvalue[6];
            low = returnvalue[7];
            if (hight > 0) {
                hight = (hight & 255) << 8;
            }
            next_dis = hight + (low & 255);


            if (pre_dis > 30 || pre_dis < 0) {
                pre_dis = 0;
            } else {
                pre_dis = 1;
            }
            if (data[15] == 1) { // 2017-5-19 17:12:36 lz 这边不是获取到值而是判断是否有障碍物

                if (next_dis > 30 || next_dis < 0) {
                    next_dis = 0;
                } else {
                    next_dis = 1;
                }
                dealwith(next_dis, data, 1);
            } else {
                dealwith(pre_dis, data, 1);
            }


            // 探测障碍物的距离。
        } else if (data[6] == 0x10) {
//            LogMgr.e("碍物的距离 buff: ");
            // strReturn = getParam(str, 10);
            byte[] motor = new byte[]{0x56, 'A', 'I', 'R', 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] returnvalue = ReadAIValue(motor);
            LogMgr.e("碍物的距离 buff: " + Utils.bytesToString(returnvalue));
            int pre_dis = 0; // 前方超声距离
            int next_dis = 0;
            int hight = 0;
            int low = 0;
            hight = returnvalue[4];
            low = returnvalue[5];
            if (hight > 0) {
                hight = (hight & 255) << 8;
            }
            pre_dis = hight + (low & 255);
            //后端距离。
            hight = 0;
            low = 0;
            hight = returnvalue[6];
            low = returnvalue[7];
            if (hight > 0) {
                hight = (hight & 255) << 8;
            }
            next_dis = hight + (low & 255);

            if (data[15] == 1) {
                dealwith(next_dis, data, 1);
            } else {
                dealwith(pre_dis, data, 1);
            }
            // 机器人碰撞到物体。
        } else if (data[6] == 0x11) {
            LogMgr.e("come peng zhuang hanshu  ");
            byte[] motor = new byte[]{0x56, 'C', 'O', 'L', 'L', 'I', 'S',
                    'I', 'O', 'N', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] returnvalue = ReadObjectValue(motor);
            int value = returnvalue[10]; // 碰撞data[10] 就是碰撞。
            int collisionvalue = 0;
            //copy 解释执行代码。
            LogMgr.e("zhuang hanshu value is: " + value);
            if (data[15] == 0) {//左前碰撞
                if (value == 1) {
                    collisionvalue = 1;
                } else {
                    collisionvalue = 0;
                }
            } else if (data[15] == 1) {//右前碰撞
                if (value == 2) {
                    collisionvalue = 1;
                } else {
                    collisionvalue = 0;
                }
            } else if (data[15] == 2) {//正前碰撞
                if (value == 3) {
                    collisionvalue = 1;
                } else {
                    collisionvalue = 0;
                }
            }
            dealwith(collisionvalue, data, 1);

            //地面灰度。 地面灰度需要先开启后读取
        } else if (data[6] == 0x13) {

            int value = 0;
            byte[] revalue = Read2gray();
            LogMgr.e("gray buff: " + Utils.bytesToString(revalue));
            //这里只要第三个值。
            int hight = 0;
            int low = 0;
            int grayIndex = data[18] * 2 + 5;
            hight = revalue[grayIndex];
            low = revalue[grayIndex + 1];
            if (hight > 0) {
                hight = (hight & 0xff) << 8;
            }
            value = hight + (low & 0xff);
            dealwith(value, data, 1);


            //这里直接返回角度。
        } else if (data[6] == 0x14) {
            //东南西北  0~3.
            float value = 0;
            //float[] SN =mSensor.getmO();
            //LogMgr.e("sn is null: "+(SN==null));
            value = mSensor.getMcompass();

            if (value != 0) {

                dealwith((int) value, data, 1);
            }

            //下俯，上扬，左翻，右翻。
        } else if (data[6] == 0x15) {

            int value = 0;
            float[] SN = mSensor.getmO();
            LogMgr.e("SN value is : " + (SN == null)+"data[15] =" + data[15]);
            LogMgr.e("SN[0] = " + SN[0] +"; SN[1] = " + SN[1] + "; SN[2] = "+ SN[2]);
            //上下是value[1].下正  上负。
            if (SN != null) {
                //对于上扬  下府这个值是需要减去70的。
                if (data[15] == 0) {
                    //对于下府取10~90度之间。
                    //bug 11637 把5改成4（暂时）
                    if (SN[1] >= (4 + 70) && SN[1] <= (90 + 70)) {
                        value = 1;
                    }
                    //上扬。
                } else if (data[15] == 1) {
                    //这里应该取-10 ~ -90之间。 基础值是70度。
                    if (SN[1] >= (70 - 90) && SN[1] <= (70 - 10)) {
                        value = 1;
                    }
                    //左翻，右翻。 左正右负。   左右翻转的值是对的。
                } else if (data[15] == 2) {
                    if (SN[2] >= 5 && SN[2] <= 90) {
                        value = 1;
                    }
                    //右翻。
                } else if (data[15] == 3) {
                    if (SN[2] >= -90 && SN[2] <= -5) {
                        value = 1;
                    }
                }

                LogMgr.e("上下 vlaue[1] is: " + SN[1] + "左右value[2] is: " + SN[2]);
                dealwith(value, data, 1);
            }
        } else if (data[6] == 0x16) {//下视。


            byte[] motor = new byte[]{0x56, 'L', 'O', 'O', 'K', 'D', 'O',
                    'W', 'N', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            byte[] returnvalue = ReadFlyValue(motor);
            int value = returnvalue[9];// 0没有 1后左 2后右 3后左右。
//			if (value == 0) {
//				value = 3;
//			} else if (value == 3) {
//				value = 0;
//			}else if(value == 2){
//				value = 1;
//			}else if(value == 1){
//				value = 2;
//			}
            LogMgr.e("下视value is：  " + value);
            int lookDown = 0;
            switch (data[15]) {

                case 0://是否左悬空。
                    if (value == 2 || value == 0) {
                        lookDown = 1;
                    } else {
                        lookDown = 0;
                    }
                    break;
                case 1://是否右悬空
                    if (value == 1 || value == 0) {
                        lookDown = 1;
                    } else {
                        lookDown = 0;
                    }
                    break;
                case 2://是否左右悬空
                    if (value == 0) {//全悬挂是0.
                        lookDown = 1;
                    } else {
                        lookDown = 0;
                    }
                    break;

            }
            dealwith(lookDown, data, 1);


        } else if (data[6] == 0x17) { //录音功能。
            //添加去除界面
//            Message msg = mHandler.obtainMessage();
//            msg.what = 0;
//            mHandler.sendMessage(msg);

            SendScratchToBrain(5, displayClose, new byte[2]);
            byte[] temp = new byte[4];
            System.arraycopy(data, 15, temp, 0, temp.length);
            int time = Utils.byteAray2Int(temp);
            LogMgr.e("time = " + time);
            Record_pre = System.currentTimeMillis();
            if (Record_pre - record_next > 1000 && Record_pre - record_next > 2 * time * 900) {
                record_next = Record_pre;

                if (Utils.byteAray2Int(temp) > 0) {
                    SendScratch(3, temp);
                }

            }
            //时钟复位。
        } else if (data[6] == 0x18) {
            dealwith(0, data, 1); //这里没有意义都返回0
            //系统时间。
        } else if (data[6] == 0x19) {
            dealwith(0, data, 1);//这里没有意义都返回0
            //显示的是1a.
        } else if (data[6] == 0x1A) {
            //显示 命令不做。
            display(data);
            //感受到触摸。
        } else if (data[6] == 0x1B) {

            int value = gettouchhead();
            dealwith(value, data, 1);//头部触摸。

        } else if (data[6] == 0x1C) {//拍照

            pic_pre = System.currentTimeMillis();
            if (pic_pre - pic_next > 3500) {
                pic_next = pic_pre;
                byte[] temp = new byte[1];
                temp[0] = 1;
                SendScratch(2, temp);
            }

        } else if (data[6] == 0x1D) { //校准指南针。
        } else if (data[6] == 0x1E) {// 扬声器模拟动物。
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

        } else if (data[6] == 0x1F) {//控制机器人播放乐器声音

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

        } else if (data[6] == 0x20) {//控制机器人播放自我介绍。

            // 介绍一
            if (data[15] == 0x00) {
                mPlayer.play("changedansw1.mp3");
                // 介绍二
            } else if (data[15] == 0x01) {
                mPlayer.play("changedansw2.mp3");
                // 介绍三
            } else if (data[15] == 0x02) {
                mPlayer.play("changedansw3.mp3");
                // 介绍四
            } else if (data[15] == 0x03) {
                mPlayer.play("changedansw4.mp3");
                // 介绍五
            } else if (data[15] == 0x04) {
                mPlayer.play("changedansw5.mp3");
                // 介绍六
            } else if (data[15] == 0x05) {
                mPlayer.play("changedansw6.mp3");
                // 介绍七
            } else if (data[15] == 0x06) {
                mPlayer.play("changedansw7.mp3");
                // 介绍八
            } else if (data[15] == 0x07) {
                mPlayer.play("changedansw8.mp3");
            }
        } else if (data[6] == 0x21) {//左右伦速度。---------------------------???????

            byte[] motor = new byte[]{0x55, 'B', 'L', 'E', 'S', 'E', 'T',
                    0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            //这里需要解析下左右轮速度。然后填进去。
            byte[] left = new byte[4];
            System.arraycopy(data, 15, left, 0, 4);
            int leftspeed = Utils.bytesToInt2(left, 0);
            if (leftspeed > 100) {
                leftspeed = 100;
            } else if (leftspeed < -100) {
                leftspeed = -100;
            }

            byte[] right = new byte[4];
            System.arraycopy(data, 19, right, 0, 4);
            int rightspeed = Utils.bytesToInt2(right, 0);
            if (rightspeed > 100) {
                rightspeed = 100;
            } else if (rightspeed < -100) {
                rightspeed = -100;
            }
            LogMgr.e("leftspeed: " + leftspeed + "rightspeed: " + rightspeed);
            //byte speed = (byte) (100 + data[19]);
            // 右转右轮停止。
            motor[8] = (byte) (leftspeed + 100);
            motor[9] = (byte) (rightspeed + 100);

            writeBuff(motor);
        }else if(data[5] == (byte) 0x11 && data[6] == (byte)0x0B){//
            //停止命令
            stopScratch();
        }

    }
    // 为M设备添加读取返回值的方法。
    private byte[] ReadAIValue(byte[] val) {
        byte[] readbuff = new byte[40];
        // byte[] errbuff = new byte[20];
        byte[] rest = new byte[20];
        // 数组初始化。
        for (int m = 0; m < 40; m++) {
            if (m < 20) {
                rest[m] = readbuff[m] = 0x00;
            } else {
                readbuff[m] = 0x00;
            }

        }
        byte[] AIBuff = new byte[20];
        System.arraycopy(val, 0, AIBuff, 0, 20);// 这里将20个字节的命令拷贝进来。
        // 这里的循环保证成功。
        for (int n = 0; n < 30; n++) {
            //
            try {
                // mOutputStream.write(AIBuff);
                LogMgr.d("发送指令："+Utils.bytesToString(AIBuff));
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("readbuff = Exception");
                return rest;
            }
            LogMgr.e("readbuff = " + Utils.bytesToString(readbuff));
            // ///////////////循环查找第一个字节//////////////////
            for (int i = 0; i < 20; i++) {
                // 这里返回值有0x55,也有0x56.
                if (readbuff[i] == 0x56 && readbuff[i + 1] == 'A'
                        && readbuff[i + 2] == 'I' && readbuff[i + 3] == 'R') {
                    // byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, rest, 0, 20);
                    // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    // i = 50;
                    return rest;
                }
            }
        }

        return rest;
    }

    // M系列碰撞到物体。
    private byte[] ReadObjectValue(byte[] val) {
        byte[] readbuff = new byte[40];
        // byte[] errbuff = new byte[20];
        byte[] rest = new byte[20];
        // 数组初始化。
        for (int m = 0; m < 40; m++) {
            if (m < 20) {
                rest[m] = readbuff[m] = 0x00;
            } else {
                readbuff[m] = 0x00;
            }

        }
        byte[] AIBuff = new byte[20];
        System.arraycopy(val, 0, AIBuff, 0, 20);// 这里将20个字节的命令拷贝进来。
        // 这里的循环保证成功。
        for (int n = 0; n < 30; n++) {
            //
            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                return rest;
            }
            // ///////////////循环查找第一个字节//////////////////
            for (int i = 0; i < 20; i++) {
                // 这里返回值有0x55,也有0x56.
                if (readbuff[i] == 0x56 && readbuff[i + 1] == 'C'
                        && readbuff[i + 2] == 'O' && readbuff[i + 3] == 'L'
                        && readbuff[i + 4] == 'L' && readbuff[i + 5] == 'I'
                        && readbuff[i + 6] == 'S' && readbuff[i + 7] == 'I'
                        && readbuff[i + 8] == 'O' && readbuff[i + 9] == 'N') {
                    // byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, rest, 0, 20);
                    // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    // i = 50;
                    return rest;
                }
            }
        }

        return rest;
    }

    // M系列悬空。
    private byte[] ReadFlyValue(byte[] val) {
        byte[] readbuff = new  byte[40];
        // byte[] errbuff = new byte[20];
        byte[] rest = new byte[20];
        // 数组初始化。
        for (int m = 0; m < 40; m++) {
            if (m < 20) {
                rest[m] = readbuff[m] = 0x00;
            } else {
                readbuff[m] = 0x00;
            }

        }
        byte[] AIBuff = new byte[20];
        System.arraycopy(val, 0, AIBuff, 0, 20);// 这里将20个字节的命令拷贝进来。
        // 这里的循环保证成功。
        for (int n = 0; n < 30; n++) {
            //
            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                return rest;
            }
            // ///////////////循环查找第一个字节//////////////////
            for (int i = 0; i < 20; i++) {
                // 这里返回值有0x55,也有0x56. 这里匹配lookdown.
                if (readbuff[i] == 0x56 && readbuff[i + 1] == 'L'
                        && readbuff[i + 2] == 'O' && readbuff[i + 3] == 'O'
                        && readbuff[i + 4] == 'K' && readbuff[i + 5] == 'D'
                        && readbuff[i + 6] == 'O' && readbuff[i + 7] == 'W'
                        && readbuff[i + 8] == 'N') {
                    // byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, rest, 0, 20);
                    // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    // i = 50;
                    return rest;
                }
            }
        }

        return rest;
    }
    //停止命令；
    private void stopScratch() {
        record_next = 0;
        byte[] result1 = sendProtocol((byte) 0x02, (byte) 0x11, (byte) 0x08, new byte[]{1,0,0,0,0,0,0,0},toStm32);//暂时定为休息-gqg
        mPlayer.stop();//停止播放；
        SendScratchToBrain(5, displayClose, new byte[2]);//关闭显示要。(字符串，照片。
        SendScratchToBrain(5, 7, new byte[1]);//mode用7.关闭录音界面
        byte[] result2 = sendProtocol((byte) 0x02, (byte) 0x11, (byte) 0x08, new byte[]{0,0,0,0,0,0,0,0},toStm32);//暂时定为休息-gqg
    }
    //M系列地面灰度传感器。//    //老协议
    private byte[] Read2gray() {
        byte[] readbuff = new byte[40];
        // byte[] errbuff = new byte[20];
        byte[] rest = new byte[20];
        // 数组初始化。
        for (int m = 0; m < 40; m++) {
            if (m < 20) {
                rest[m] = readbuff[m] = 0x00;
            } else {
                readbuff[m] = 0x00;
            }

        }
        //开启灰度传感器。
        byte[] AIBuff = {0x55, 'G', 'R', 'A', 'Y', 'O', 'P', 0x01, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0};
        //开启五次确保灰度传感器打开。
        for (int i = 0; i < 5; i++) {
            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        byte[] comd = {0x56, 'G', 'R', 'A', 'Y', 'R', 'D', 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0};
        // 这里的循环保证成功。
        for (int n = 0; n < 30; n++) {
            //
            try {

                readbuff = SP.request(comd);
            } catch (Exception e) {
                e.printStackTrace();
                return rest;
            }
            // ///////////////循环查找第一个字节//////////////////
            for (int i = 0; i < 20; i++) {
                // 这里返回值有0x55,也有0x56. 这里匹配lookdown.
                if (readbuff[i] == 0x56 && readbuff[i + 1] == 'G'
                        && readbuff[i + 2] == 'R' && readbuff[i + 3] == 'A'
                        && readbuff[i + 4] == 'Y' && readbuff[i + 5] == 'R'
                        && readbuff[i + 6] == 'D') {
                    // byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, rest, 0, 20);
                    // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    // i = 50;
                    return rest;
                }
            }
        }

        return rest;
    }

    //新协议
    public int gettouchhead() {
        byte[] readbuff =  sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x37, null,toStm32);// 发送获取传感器协议。
            //LogMgr.e("head "+ Utils.bytesToString(readbuff, readbuff.length));
            for (int j = 0; j < 20; j++) {

                if ((readbuff[j] & 0xff) == 0xf0 && (readbuff[j + 1] & 0xff) == 0x30) {
                    return readbuff[j + 9] & 0xff;
                }
            }
        return 0;
    }
    //老协议
    public void ledset(int head, int color, int type) {
        //脖子1，底部2，轮子3    color 红绿蓝白，   类型。
        //byte[] motor = new byte[] { 0x55, 'C', 'O', 'L', 'E', 'D', 0x02, 0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        lightled(head, color,type);
        lightWave(head, type);
    }
    //老协议
    private void lightled(int head, int color,int type) {
        byte[] motor = new byte[]{0x55, 'C', 'O', 'L', 'E', 'D', 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //第6位。
        switch (head) {
            case 1:
                motor[6] = 0x01;
                break;
            case 2:
                motor[6] = 0x04;
                break;
            case 3:
                motor[6] = 0x02;
                break;

        }
        if (color == 3) {//白色。
            motor[7] = (byte) 0xff;
            motor[8] = (byte) 0xff;
            motor[9] = (byte) 0xff;
        } else {
            motor[7 + color] = (byte) 0xff;
        }
        if(type == 3 ){
            motor[7] = (byte) 0x00;
            motor[8] = (byte) 0x00;
            motor[9] = (byte) 0x00;
        }
        writeBuff(motor);


    }

    //这个和C的显示为啥没共有同一个函数？关键里面的逻辑怎么还能不一样？！!!!!
    private void display(byte[] data) {
        //发命令对应的显示的方法
        LogMgr.e("data = " + Utils.bytesToString(data));
        String content = "";
        int len = Utils.byte2int_2byteHL(data, 2);
        byte[] displayContext = new byte[len - 11];
        // 第二个参数从15开始。
        System.arraycopy(data, 15, displayContext, 1, displayContext.length - 1);
        displayContext[0] = 0;
        // 这里应该直接发给brain来显示。
        byte[] sendData=null;
        try {
            content += new String(displayContext, "UTF-8");
            LogMgr.e("contains---------"+content);
            if (content.contains(NUM_ZH)||content.contains(NUM_EN)){
                content = ControlApplication.instance.getString(R.string.guoda);
            }
            LogMgr.e("content: " + content);
            byte[] contentData=content.getBytes();
            sendData=new byte[contentData.length+1];
            System.arraycopy(contentData,0,sendData,1,contentData.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SendScratchToBrain(5, display_String_mode, sendData);//
    }
    //老协议
    private void lightWave(int head, int type) {

        byte[] motor = new byte[]{0x55, 'W', 'A', 'V', 'E', 'F', 'O', 'R', 'M', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        if (head == 2) {
            motor[9] = 0x04;//底部。
        } else {
            motor[9] = (byte) head;//脖子，轮子不变。
        }

        motor[10] = (byte) ((type + 1) & 0xff);
        writeBuff(motor);

    }

    // 向客户端发送协议
    public byte[] sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data,int to) {
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
        LogMgr.e("tostm32" + Utils.bytesToString(sendbuff));
        if(to == toPad){
            SendScratch(1,sendbuff);
        }else if(to == toStm32){
            try {
                byte[] result = SP.request(sendbuff,100);
                if(result != null){
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    return  new byte[30];
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
        record_next = 0;
        pic_next = 0;
        mPlayer.stop();
    }
}
