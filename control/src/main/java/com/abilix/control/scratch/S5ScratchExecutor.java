package com.abilix.control.scratch;

import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.R;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class S5ScratchExecutor extends AbstractScratchExecutor {
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

    private int value = 0;
    // 单个电机控制协议。
    private byte[] onemotor = {(byte) 0xFE, 0x68, 'Z', 0, 0, 0x0B, (byte) 0xFF, (byte) 0xFF,
            0, 0x07, 0x03, 0x1E, 0, 0, 0, 0, 0, (byte) 0xAA, 0x16};
    private byte[] motorIdAndangle = {(byte) 0xFE, 0x68, 'Z', 0, 0, 0x09, (byte) 0xFF, (byte) 0xFF, 0, 0x05,
            // 后10个字节。
            0x03, 0x1E, 0, 0, 0, (byte) 0xAA, 0x16};
    //照片路劲。
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
    protected MySensor mSensor;

    public S5ScratchExecutor(Handler mHandler) {
        super(mHandler);

        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come S5:" + Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private void checkFunction(final byte[] data) {

        int parms1 = -1, parms2 = -1, parms3 = -1;
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[5]){
            case 0x11:
                stopScratch();
                break;
            default:
                switch (data[6]) {
                    case 0x09:// 前方是否有障碍物。
//			LogMgr.e("0x09: ");
                        distance(1);
                        break;
                    case 0x0A:// 探测障碍物距离
//			LogMgr.e("0x0A: ");
                        distance(0);
                        break;
                    case 0x01://0x0C(舵机控制)  第一个参数舵机ID号(1-22)； 第二个参数舵机速度（0-1023）；第三个参数舵机角度（-180~+180）
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        parms3 = getparms(data, 3);
                        LogMgr.e("parms1:  " + parms1 + "  parms2: " + parms2 + "   parms3" + parms3);
                        // id,角度，速度。
                        motorControlNew(parms1, parms3, parms2);
                        break;
                    //这三条命令缺少协议。
                    case 0x02://舵机上电。 没有协议。第一个参数舵机ID号(0-22)，0为全部
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 1);
                        break;
                    case 0x03://舵机释放。 没有协议。第一个参数舵机ID号(0-22)，0为全部
                        LogMgr.e("0X03");
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 2);
                        break;
                    case 0x04://舵机归零。第一个参数舵机ID号(0-22)，0为全部
                        parms1 = getparms(data, 1);
                        freeAndZero(parms1, 3);
                        break;
                    case 0x05://启动显示。
                        display(data);
                        break;
                    case 0x06://启动扬声器。
//			LogMgr.e("0x11: ");

                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("0x06: parms1 = " + parms1 + ";parms2=" + parms2);
                        try{
                            Thread.sleep(100);
                            playMusic(parms1, parms2);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        break;
                    case 0x07://设置LED。  第一个参数为颜色：1~10(亮度)；第二个参数为状态1~10（频率）
//			LogMgr.e("0x12: ");
//                LogMgr.e("0X07");
                        parms1 = getparms(data, 1);
                        parms2 = getparms(data, 2);
                        LogMgr.e("parms1: " + parms1 +";parms2 = "+parms2);
                        SendScratchToBrain(5, 14, new byte[]{(byte) ((parms2 * 16 + parms1) & 0xFF)});
                        try {
                            Thread.sleep(100);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
//			byte[] Led = new byte[3];
//			Led[parms1] =(byte)0xff;
//			sendProtocol((byte)0x05, (byte)0xA3, (byte)0xA0, Led, toStm32);
                        //需要底层给协议。
                        break;
                    case 0x08://关闭    第一个参数为类型：0~2("显示","扬声器","LED")
//			LogMgr.e("0x13: ");
//                LogMgr.e("0X08");
                        parms1 = getparms(data, 1);
                        close(parms1);
                        break;
                    case 0x0B://指南针校准。
//			LogMgr.e("0x14: ");
                        //命令不做指南针校准。
                        ResponseZero();
                        break;
                    case 0x0C://指南针角度。
//			LogMgr.e("0x15: ");
                        getCompass();
                        break;
                    case 0x0D://麦克风录音。
                        //用一个时间戳来限制。
//			LogMgr.e("0x16: ");
//                LogMgr.e("0X0D");
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
                                && Record_pre - record_next >= parms2 * 1000 - 100) {
                            record_next = Record_pre;
                            timeInit = parms2;
                            if (parms2 >= 1) { // 录音时间小于0都不处理。
                                RecordVoice(parms1, parms2);
                            }

                        }
                        break;
                    case 0x0E://拍照。
//			LogMgr.e("0x17");
//                LogMgr.e("0X0E");
                        pic_pre = System.currentTimeMillis();
                        if (pic_pre - pic_next > 3500) {
                            pic_next = pic_pre;

                            parms1 = getparms(data, 1);
                            takepicture(parms1);
                        }
                        break;
                    case 0x0F://启动新的扬声器(暂时未加)
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
                    case 0x10://获取陀螺仪的角度
                        parms1 = getparms(data, 1);//0：X轴；1：Y轴；2：Z轴
                        getXYZ(parms1);
                        break;
                }
                break;
        }


    }


    /************************************************* 函数部分 ***************************************************/
    // 0x01
    private void motorSet(int id, int angle) {

        byte[] motor = new byte[17];
        // 复制数据。
        System.arraycopy(motorIdAndangle, 0, motor, 0, motor.length);
        // 设置ID。
        motor[8] = (byte) id;
        // 设置角度。
        System.arraycopy(intToBytes(angle), 0, motor, 12, 2);
        // System.arraycopy(src, srcPos, dst, dstPos, length)
        // 设置校验位。
        byte check = 0;
        for (int i = 8; i < 14; i++) {
            check += motor[i];
        }
        motor[14] = (byte) ~(check & 0xFF);
        LogMgr.e("write data: " + Utils.bytesToString(motor));
        writeBuff(motor);
    }

    // 0x01
    private void motorSet(int id, int sudu, int angle) {

        byte[] motor = new byte[19];
        System.arraycopy(onemotor, 0, motor, 0, motor.length);
        motor[8] = (byte) id;
        System.arraycopy(intToBytes(angle), 0, motor, 12, 2);
        System.arraycopy(intToBytes(sudu), 0, motor, 14, 2);
        byte check = 0;
        for (int i = 8; i < 16; i++) {
            check += motor[i];
        }
        motor[16] = (byte) ~(check & 0xFF);
        LogMgr.e("write data: " + Utils.bytesToString(motor));
        writeBuff(motor);

    }

    //0x09 and 0x10 障碍物距离。0为距离，1为障碍物。
    private void distance(int type) {
        LogMgr.e("type:: " + type);
        //获取超声。
//        int result = 0;
//        byte[] send = new byte[16];
//        byte[] receive = new byte[40];
//
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
//        try {
//            receive = SP.request(send);
//            if (receive == null) {
//                LogMgr.e("SP.request Error! null");
//                return;
//            }
//            LogMgr.e("readbuff: " + Utils.bytesToString(receive, receive.length));
//            for (int i = 0; i < 30; i++) {
//                if ((receive[i] & 0xFF) == 0xFE && receive[i + 5] == 0x07
//                        && (receive[i + 8] & 0xFF) == 0xc8
//                        && receive[i + 9] == 0x04) {
//                    String str1 = String.format("%02x ", receive[11 + i]);
//                    String str2 = String.format("%02x ", receive[12 + i]);
//                    String reults = str2.trim() + str1.trim();
//                    result = Integer.valueOf(reults.trim(), 16);
//                    break;
//                }
//            }
//            if (result > 200 || result < 0) {
//                    result = 199;
//                }
//            if (type == 1) {
//                if (result > 0 && result < 20) {
//                    result = 1;
//                } else {
//                    result = 0;
//                }
//            }
//            byte[] sendArray = new byte[8];
//            System.arraycopy(id, 0, sendArray, 0, id.length);
//            System.arraycopy(intToByte(result), 0, sendArray, 4, 4);// 这里写死就是4个字节。
//            sendProtocol((byte) 0x05, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
//            }catch(Exception e){
//                e.printStackTrace();
//            }
        int dis = 0;
//        for (int i = 0; i < 3; i++){
        byte[] readbuff =sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xA3, null, toStm32);
//        }
//        LogMgr.e("readbuff: " + Utils.bytesToString(readbuff, readbuff.length));
//        for (int j = 0; j < 20; j++) {
//            if ((readbuff[j] & 0xff) == 0xf0
//                    && (readbuff[j + 1] & 0xff) == 0xA0) {
//                //return Utils.byte2int_2byteHL(readbuff, j+6);
//                dis = Utils.byte2int_2byteHL(readbuff, j + 6);
//                if (dis > 200 || dis < 0) {
//                    dis = 199;
//                } else if (dis == 0) {
////                    dis = 2;
//                }
//            }
//        }
        if(readbuff[5]  == (byte)0xF0  &&  readbuff[6] == (byte)0xA0){
            LogMgr.e("dis1:: " + dis);
            dis = Utils.byte2int_2byteHL(readbuff, 11);
            if (dis > 200 || dis < 0) {
                    dis = 199;
                }
        }
        LogMgr.e("dis:: " + dis);
        if (type == 1) {
            if (dis > 0 && dis < 20) {
                dis = 1;
            } else {
                dis = 0;
            }
        }
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(dis), 0, sendArray, 4, 4);// 这里写死就是4个字节。
        sendProtocol((byte) 0x05, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private String NUM_EN ="Number too large to display";
    private String NUM_ZH = "数字过大无法显示";
    //0x10 启动显示。
    private void display(byte[] data) {
//		第一个参数为类型：0~2("舵机角度","字符","照片")，
//		（1）当第一个参数为"舵机角度"时，第二个参数为0~22,0为全部
//		（2）当第一个参数为"字符"时，第二个参数为字符串数组（最大19个字节）
//		（3）当第一个参数为"照片"时，第二个参数为0~9
        int type = getparms(data, 1);
        int num = getparms(data, 2);
        LogMgr.e("num:: " + num);

        String content = "";
        switch (type) {
            case 2:
                //获取舵机角度。
                String ss = GetAngle(num);
                //	LogMgr.e("ss:: "+ss.toString() +"ss.getBytes() = " +Arrays.toString(ss.getBytes()));
                byte[] senddata = new byte[ss.getBytes().length + 1];
                System.arraycopy(ss.getBytes(), 0, senddata, 1, senddata.length - 1);
                //		LogMgr.e("senddata:: "+Utils.bytesToString(senddata, senddata.length));
                SendScratchToBrain(5, display_String_mode, senddata);
                break;
            case 0:
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
                        content = ControlApplication.instance.getString(R.string.guoda);
//                        content = FileUtils.isCH() == true ? NUM_ZH : NUM_EN;
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
            case 1:
                byte[] sendnum = new byte[5];
                sendnum[0] = 1;
                System.arraycopy(intToByte(num), 0, sendnum, 1, 4);
                File mfile = new File(SCRATCH_VJC_IMAGE_ + num + SCRATCH_VJC_IMAGE_JPG);
                if (mfile.exists()) {
                    SendScratchToBrain(5, display_String_mode, sendnum);
                }
                break;
            case 3://陀螺仪角度值
                String gray2dis = getGrayToDisPlay(num);
                byte[] senddata_gray = new byte[gray2dis.getBytes().length + 1];
                System.arraycopy(gray2dis.getBytes(), 0, senddata_gray, 1, senddata_gray.length - 1);
                //		LogMgr.e("senddata:: "+Utils.bytesToString(senddata, senddata.length));
                SendScratchToBrain(5, display_String_mode, senddata_gray);
                break;
        }
    }

    private String getGrayToDisPlay(int num) {
        if(num <0 || num >4){
            return "num 越界";
        }
        float[] SN = mSensor.getmO();
        String ss = "";
        switch (num){
            case 0://全部 X,Y,Z"\n"
                ss = "X:"+(int)SN[1] +"\n"+"Y:"+(int)SN[2] +"\n"+"Z:"+(int)SN[0] ;
                break;
            case 1://X
                ss = "X:"+(int)SN[1];
                break;
            case 2://Y
                ss = "Y:"+(int)SN[2];
                break;
            case 3://Z
                ss = "Z:"+(int)SN[0];
                break;
        }
        return ss;
    }

    private String GetAngle(int id) {
        if (id < 0 || id > 22) {
            LogMgr.e("id 越界");
            return "id 越界";
        }
        String ss = "";
        int i =0;
        byte[] readbuff = null;
        for (int j = 0; j < 2; j++) {
            readbuff =sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x61, null, toStm32);//获取所有角度。
        }
//        LogMgr.e("readbuff1: " + Utils.bytesToString(readbuff, readbuff.length));
        while (((readbuff[0] & 0xff) == 0x00 ||(readbuff[1] & 0xff) == 0x00||(readbuff[5] & 0xff) == 0x00
                || (readbuff[6] & 0xff) == 0x00) && (i<10)) {
            i++;
            readbuff = sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x61, null, toStm32);//获取所有角度。
            LogMgr.e("readbuff2: " + Utils.bytesToString(readbuff));
        }
            ss = getAngle(id, readbuff);

        return ss;
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
                            ss += m + ": " + value + "     ";
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
    //停止所有
    private void stopScratch() {
        record_next = 0;
        sendProtocol((byte) 0x05, (byte) 0x11, (byte) 0x08, new byte[]{1,0,0,0,0,0,0,0}, toStm32);
        close(0);//关闭显示
        close(1);//关闭扬声器
        close(2);//关闭LED
//        takepicture(0);//关闭拍照
        SendScratchToBrain(5, 7, new byte[1]);//mode用7.关闭录音界面
        sendProtocol((byte) 0x05, (byte) 0x11, (byte) 0x08, new byte[]{0,0,0,0,0,0,0,0}, toStm32);
    }
    //0x11
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

    private void close(int type) { // 0~3("显示","扬声器","LED"，"陀螺仪数值")
        if (type == -1 || type > 4) {
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
            case 2:// 关闭LED    这个不确定。
                SendScratchToBrain(5, 14, new byte[]{0x00});
                break;
            default:
                break;
        }
    }

    //上电释放归零写一个函数。
    private void freeAndZero(int id, int type) {//固定  释放  归零。

        if (id == 0) {
            byte[] dataALL = new byte[45];
            dataALL[0] = 22;
            for (int i = 1; i <= 22; i++) {
                dataALL[2 * i - 1] = (byte) i;
                dataALL[2 * i] = (byte) type;
            }
            sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x65, dataALL, toStm32);//按照H的协议来。

        } else {
            byte[] data = new byte[3];
            data[0] = (byte) 1;//就是一个。
            data[1] = (byte) id;
            data[2] = (byte) type;
            sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x65, data, toStm32);//按照H的协议来。
        }

    }

    private void ResponseZero() {

        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }
    private void getXYZ(int dircation) {
        if(dircation < 0 || dircation > 2){
            return;
        }
        int compassValue = 0;
        float[] SN = mSensor.getmO();
        switch (dircation){
            case 0:
                compassValue = (int) SN[1];
                break;
            case 1:
                compassValue = (int) SN[2];
                break;
            case 2:
                compassValue = (int) SN[0];
                break;
        }
        // 这里返回pad端就好了
        byte[] sendArray = new byte[8];
        System.arraycopy(id, 0, sendArray, 0, id.length);
        System.arraycopy(intToByte(compassValue), 0, sendArray, 4, 4);
        sendProtocol((byte) 0x05, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
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
        sendProtocol((byte) 0x05, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    private void RecordVoice(int num, int time) {
        // 这里面应该这样改。
        byte[] array = new byte[8];
        System.arraycopy(intToByte(num), 0, array, 0, 4);
        System.arraycopy(intToByte(time), 0, array, 4, 4);
        SendScratchToBrain(5, 7, array);//mode用7.

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
        sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xA5, data, toWrite);

    }

    // 向底层发送信息不带返回值。
    protected void writeBuff(byte[] buff) {
        try {
            SP.request(buff);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
    }

    // FE 68 5A 00 00 08 FF FF 01 04 02 24 02 D2 AA 16
    public byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
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
                byte[] buffer = SP.request(sendbuff);
                if(buffer != null){
                    return buffer;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(to == toWrite){
            SP.request(sendbuff,20);
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

        pic_next = 0;
        record_next = 0;
        timeInit = 0;
        mPlayer.stop();
    }

}
