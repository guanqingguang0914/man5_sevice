package com.abilix.control.scratch;

import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;

/*****
 *
 * @author 4.11将命令字1修改为0xA5.
 *
 */
public class CXScratchExecutor extends AbstractScratchExecutor {

    protected MySensor mSensor;
    // 这里定义一个时间戳，防止拍照录音等无限循环卡死。
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;

    public CXScratchExecutor(Handler mHandler) {
        super(mHandler);

        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.i("come Scratch execute "
                + Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    // 这里还是要记忆的。
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int toPad = 0;
    private final int toStm32 = 1;

    // 这里先这么定义
    private final int display_String_mode = 12;
    private final int displayClose = 2;

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

    private void checkFunction(final byte[] data) {

        int parms1 = -1, parms2 = -1, parms3 = -1;
        // byte[] array = new byte[4];//用于复制数据的临时buff
        //调试用的，这个协议先不管。
//		if (data[5] != 0x0B) {
//			// CX系列跟这里完全一样，这里按要求改改。
//			LogMgr.e("协议类型错误");
//			return;
//		}
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[6]) {

            case 0x01:// 启动电机 第一个参数为电机端口：0~1("A","B")，第二个参数为转向0~1   ---------电机OK。
                // ("正转","反转")，第三个参数为速度：0~100("快","中","慢");
                parms1 = getparms(data, 1);
                parms2 = getparms(data, 2);
                parms3 = getparms(data, 3);
                LogMgr.e("parms1: " + parms1 + " 正反：" + parms2 + "  sudu:" + parms3);
                motorSet(parms1, parms2, parms3);
                break;
            case 0x02:// --------------------------------启动扬声器 这个完全一样。
                parms1 = getparms(data, 1);
                parms2 = getparms(data, 2);
                // parms2 = getParms(data, 2);
                // 启动扬声器，需要单独解析。
                playMusic(parms1, parms2);
                break;
            case 0x03:// 启动LED 第一个参数为颜色   灭 红  蓝  绿) ---------------OK
                parms1 = getparms(data, 1);
                LogMgr.e("parms1: " + parms1);
                if (parms1 >= 0 && parms1 < 3) {
                    //ledSet(parms1);
                    byte[] ledbuff = {1, 3, 2};
                    byte[] array = new byte[1];
                    array[0] = ledbuff[parms1];
                    sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x04, array, toStm32);
                }
                break;
            case 0x04:// 启动显示 --------------直接用Cnew的。
                // 第一个参数为类型：0~1("字符","照片")，（1）当第一个参数为"字符"时，第二个参数为字符串数组（最大19个字节）（2）当第一个参数为"照片"时，第二个参数为0~9
                display(data);
                break;
            case 0x05:// 关闭 第一个参数为类型：0~3("电机","扬声器","LED","显示") ----------
                parms1 = getparms(data, 1);
                close(parms1);
                break;
            case 0x06:// 超声探测到障碍物 第一个参数为端口：0~3 ----------------------ok
                parms1 = getparms(data, 1); // 这里改为4个字节。 先测试下端口，有问题。
                LogMgr.i("duankou: " + parms1);
                haveObject(parms1);
                break;
            case 0x07:// 超声探测距离 第一个参数为端口：0~7 --------------------------ok
                parms1 = getparms(data, 1);
                LogMgr.i("duankou: " + parms1);
                distance(parms1);
                break;
            case 0x08:// 碰到物体 第一个参数为端口：0~7 ------------------------ok

                parms1 = getparms(data, 1);
                LogMgr.e("duankou: " + parms1);
                touch(parms1);
                break;
            case 0x09:// 识别颜色
                // 第一个参数为端口：0~7，第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）-----------ok
                parms1 = getparms(data, 1);
                parms2 = getparms(data, 2);
                LogMgr.e("data:" + Utils.bytesToString(data));
                LogMgr.e("parms1:" + parms1 + " parms2:" + parms2);
                //颜色去掉端口选项，第一个参数为color；（待与scratch组确认）
                readColor(parms2, parms1);
                break;
            case 0x0A:// 探测灰度值 第一个参数为端口：0~7 --------- -----------------ok
                parms1 = getparms(data, 1);
                LogMgr.i("duankou: " + parms1);
                getGraySensor(parms1);
                break;
            case 0x0B:// 摄像头拍照 第一个参数为：0~9 这个暂时不做
                // 类似录音，加一个时间戳。
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

            case 0x0F:// 指南针探测角度    --------传感器没有
                //getCompass();
                break;
            case 0x10:// 陀螺仪探测 第一个参数为类型：0~3("下俯","后仰","左翻","右翻") ----------------传感器没有
                //parms1 = getparms(data, 1);
                //getPosition(parms1);
                break;
            case 0x11:// 麦克风录音 第一个参数为：0~9，第二个参数为时间：1~3600 ----------------OK

                // 用一个时间戳来限制。
                Record_pre = System.currentTimeMillis();
                if (Record_pre - record_next > 3500) {
                    record_next = Record_pre;

                    parms1 = getparms(data, 1);// 第一个参数应该是保存9个音频
                    parms2 = getparms(data, 2);// 第二个参数是时间。

                    if (parms2 > 0) {
                        RecordVoice(parms1, parms2);
                    }
                }

                break;
        }

    }

    /******************************* 函数执行部分 *********************************************/

    // 0x01 调用电机。
    private void motorSet(int port, int drection, int sudu) { // -------------------电机OK。
        // 暂设速度为100 60 20. 电机协议换成新的。

        if (Math.abs(drection) > 1 || Math.abs(port) > 2) {
            LogMgr.e("参数解析错误");
            return;
        }
        //速度还是要做一个越界处理。
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
        byte[] senddata = new byte[11];
        if (port == 0) {//这是A口转
            senddata[0] = 2;
        } else if (port == 1) {
            senddata[0] = 1;
        }
        senddata[1] = 0;
        senddata[2] = 0;
        senddata[3] = (byte) sudu;
        senddata[4] = 0;
        senddata[5] = 0;
        //B 电机。
        senddata[6] = 0;
        senddata[7] = 0;
        senddata[8] = (byte) sudu;
        senddata[9] = 0;
        senddata[10] = 0;
        sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x03, senddata, toStm32);
        // 这里用新协议进行单一端口控制，状态记录与否无所谓。
        //motorChoice(port, sudu);
//		byte[] sendArray =  new byte[4];
//		LogMgr.e("sudu: "+sudu);
//		if(port == 1){
//			sendArray[0] = (byte)sudu;
//			sendArray[1] = (byte)100;
//		}else if(port == 0){
//			sendArray[1] = (byte)sudu;
//			sendArray[0] = (byte)100;
//		}
//		sendProtocol((byte)0x09, (byte)0xA3, (byte)0x02, sendArray, toStm32);
    }

    // 0x02
    private void playMusic(int type, int num) {   //-----------------复用。
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
                if (num <= musicTool.length && num > 0) {
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

    // 0x03
    // 0x04
    private void display(byte[] data) {  //---------------------复用。
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
            byte[] displayContext = new byte[len - 16 + 1];
            // 第二个参数从19开始。
            System.arraycopy(data, 19, displayContext, 1, displayContext.length - 1);
            displayContext[0] = 0;
            // 这里应该直接发给brain来显示。
            try {
                content += new String(displayContext, "UTF-8");
                LogMgr.e("content: " + content);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            SendScratchToBrain(5, display_String_mode, content.getBytes());//

        } else if (type == 1) {// 照片
            int num = getparms(data, 2);
            byte[] sendnum = new byte[5];
            sendnum[0] = 1;
            System.arraycopy(intToByte(num), 0, sendnum, 1, 4);
            File mfile = new File(SCRATCH_VJC_IMAGE_ + num + SCRATCH_VJC_IMAGE_JPG);
            //LogMgr.e(" "+SCRATCH_VJC_IMAGE_+num+SCRATCH_VJC_IMAGE_JPG);
            if (mfile.exists()) {
                SendScratchToBrain(5, display_String_mode, sendnum);
            }

        }
    }

    // 0x05
    private void close(int type) { // "电机","扬声器","LED","显示"

        if (type == -1 || type > 3) {
            LogMgr.e("关闭类型越界");
            return;
        }
        // 我想用指针函数的，可是安卓我不会用，
        switch (type) {
            case 0:// 关闭电机
                byte[] closeMtor = new byte[4];
                closeMtor[0] = (byte) 100;
                closeMtor[1] = (byte) 100;
                sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x02, closeMtor, toStm32);
                break;
            case 1:// 关闭扬声器
                mPlayer.stop();
                break;
            case 2:// 关闭LED
                byte[] array = new byte[1];//0就是关闭。  ----------------------------------------------OK
                sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x04, array, toStm32);
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

        if (port == -1 || port > 3) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            //port = port - 1;
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

        if (port == -1 || port > 3) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            //port = port - 1;
            //int searchPort = ReadAIType(0);
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(0);// 超声是0，按钮是1，灰度是2.
            value = ReadAIValue(searchPort);
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

        if (port == -1 || port > 3) {
            LogMgr.e("端口错误");
            return;
        }
        int value = 0;
        if (port != 0) {
            //port = port - 1;
            value = ReadAIValue(port);// 这里调试一下端口。
        } else {
            int searchPort = ReadAIType(1);// 超声是0，按钮是1，灰度是2.
            value = ReadAIValue(searchPort);

        }
        LogMgr.e("value is------: " + value);
        //C1 碰撞传感器返回值可能大于4095
        value = value > 4095 ? 4095 : value;
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
    private void readColor(int port, int color) {// 颜色与端口没有关系。

        if (color == -1 || color > 4) {
            LogMgr.e("数组越界");
            return;
        }
        // 第二个参数为颜色值：0~4（"红","黄","绿","蓝","白"）
        int[] colorArray = new int[6];
        for (int m = 0; m < 6; m++) {
            colorArray[m] = ReadColorValue();// 这里存储6个值。
        }
        int sensorColor = getcolor(colorArray);
        int[] colorValue = {1, 6, 2, 3, 5};// 这里与上面的颜色顺序一致。
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

    // 0x0A
    private void getGraySensor(int port) {

        if (port == -1 || port > 6) {
            LogMgr.e("端口越界");
            return;
        }
        int grayValue = 0;
        if (port != 0) {
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

    // 0x0B 拍照     这里这样。
    private void takepicture(int num) {//拍照用系统的API。

        byte[] temp = new byte[2];
        temp[0] = 1;
        temp[1] = (byte) num;
        SendScratch(2, temp);
    }

    // 0x0C 时钟复位，系统时间，校准指南针，统统回复 0.
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
        sendProtocol((byte) 0x01, (byte) 0xa1, (byte) 0x00, sendArray, toPad);
    }

    // 0x10
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

        byte[] readbuff = null;
        try {
            readbuff =  SP.request(buff);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
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
        LogMgr.e("topad 0r tostm32 ::" + Utils.bytesToString(sendbuff));
        if (to == toPad) {
            SendScratch(1, sendbuff);
        } else if (to == toStm32) {

            try {
                return SP.request(sendbuff);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    return null;
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

    private int ReadAIValue(int type) {

        byte[] readbuff = null;
        for (int i = 0; i < 5; i++) {

            try {
                readbuff = sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x06, null,
                        toStm32);
            } catch (Exception e) {
                e.printStackTrace();
            }

            LogMgr.e("AIvalue " + Utils.bytesToString(readbuff));
            for (int j = 0; j < 20; j++) {

                if ((readbuff[j] & 0xff) == 0xf2
                        && (readbuff[j + 1] & 0xff) == 0x22) {
                    return Utils.byte2int_2byteHL(readbuff, j + 4 + type * 2);
                }
            }

        }
        return 0;
    }

    private int ReadColorValue() {
        byte[] readbuff = null;
        for (int i = 0; i < 5; i++) {

            try {
                readbuff =  sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x07, null,
                        toStm32);
            } catch (Exception e) {
                e.printStackTrace();
            }

            LogMgr.e("head " + Utils.bytesToString(readbuff));
            for (int j = 0; j < 20; j++) {

                if ((readbuff[j] & 0xff) == 0xf2
                        && (readbuff[j + 1] & 0xff) == 0x23) {
                    // return Utils.byte2int_2byteHL(readbuff, j+4+type*2);
                    int h = (readbuff[j + 6] & 0xff);
                    int s = (readbuff[j + 7] & 0xff);
                    int l = (readbuff[j + 8] & 0xff);
                    int c = (readbuff[j + 9] & 0xff);
                    if (h == 8 || l > 255) {
                        // NSLog(@"错误");
                        return -1;
                    } else if (h != 8) {
                        if (h > 12 && h <= 45) {
                            if (c < 90) {
                                if (s == 240) {
                                    // [UIColor yellowColor];
                                    return 6;
                                } else {
                                    // [UIColor whiteColor];
                                    return 5;
                                }

                            } else {
                                // =[UIColor yellowColor];
                                return 6;
                            }
                        } else {
                            if ((h == 0 && s == 0 && l == 240) || (l > 150)) {
                                // [UIColor whiteColor];
                                return 5;
                            } else if (((h >= 0 && h <= 10) || (h >= 200 && h < 255))
                                    && l < 150) {
                                // [UIColor redColor];
                                return 1;
                            } else if (h >= 120 && h < 160 && l < 150) {
                                if (l < 90) {
                                    if (h < 160 && h >= 145) {
                                        // = [UIColor blueColor];
                                        return 3;
                                    } else if (h > 60 && h < 144) {
                                        // = [UIColor greenColor];
                                        return 2;
                                    }
                                } else if (l >= 90) {
                                    if (h < 131 && h >= 120) {
                                        // = [UIColor greenColor];
                                        return 2;
                                    } else if (h > 130) {
                                        // = [UIColor blueColor];
                                        return 3;
                                    }
                                }
                            } else if ((h >= 60 && h <= 119) && l < 110) {
                                // [UIColor greenColor];
                                return 2;
                            } else if (h >= 160 && h <= 180 && l < 80) {
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

    private int ReadAIType(int type) {
        byte[] readbuff = null;
        for (int i = 0; i < 5; i++) {

            try {
                readbuff = sendProtocol((byte) 0x09, (byte) 0xA5, (byte) 0x05, null,
                        toStm32);
            } catch (Exception e) {
                e.printStackTrace();
            }

            LogMgr.e("AIType " + Utils.bytesToString(readbuff));
            int min = 0, max = 0, value = 0;
            if (type == 0) { // 超声 -- 测试OK
                min = 857;
                max = 1057;
            } else if (type == 1) { // 按钮 -- 测试OK
                min = 10;
                max = 264;
            } else if (type == 2) { // 灰度

                min = 536;
                max = 736;
            }
            for (int j = 0; j < 20; j++) {

                if ((readbuff[j] & 0xff) == 0xf2
                        && (readbuff[j + 1] & 0xff) == 0x21) {
                    // return Utils.byte2int_2byteHL(readbuff, j+4+type*2);
                    for (int n = 1; n < 4; n++) {// 这样就返回1.2.3.
                        value = Utils.byte2int_2byteHL(readbuff, j + 4 + n * 2);
                        LogMgr.e("AIType value is: " + value);
                        if (value > min && value < max) { // 查找到Type所在的AI
                            LogMgr.e("AIType duankou: " + n);
                            return n;
                        }
                    }
                }
            }

        }
        return 0;
    }

    private byte[] intToByte(int value) {
        byte[] res = new byte[4];
        res[3] = (byte) (value & 0xFF);
        res[2] = (byte) ((value >> 8) & 0xFF);
        res[1] = (byte) ((value >> 16) & 0xFF);
        res[0] = (byte) ((value >> 24) & 0xFF);
        return res;

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

    public void reSetStm32() {
        byte[] readbuff = new byte[40];
        sendProtocol((byte) 0x09, (byte) 0x11, (byte) 0x0B, null, toStm32);
    }

    @Override
    public void clearState() {
        pic_next = 0;
        record_next = 0;
        mPlayer.stop();
        reSetStm32();
    }

}
