package com.abilix.control.scratch;

import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class CScratchExecutor extends AbstractScratchExecutor {

    protected MySensor mSensor;
    // 添加麦克风。
    private AudioRecordDemo mAudioRecordDemo = null;
    private double value = 0;
    private boolean VoiceFlag = true;

    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;

    public CScratchExecutor(Handler mHandler) {
        super(mHandler);

        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e(Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private int dc0speed = 100;
    private int dc1speed = 100;
    private int dc2speed = 100;
    private int dc3speed = 100;

    // private boolean flag = false;
    // private Thread mthread = null;

    private void checkFunction(final byte[] data) {
        // 函数名是第六个字节。设置电机。
        if (data[6] == 0x01) {

            // LogMgr.e("111111111111111111111111111111111111111111");
            // flag = false;
            // byte[] motor = new byte[] { 'U', 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x64, 0x64, 0x64, 0x64, 0, 0, 0, 0, 0, 0, 0, 'O'};
            // byte[] motor = new byte[] { 'C', 'S', 'E', 'T', 'M', 0x64, 0x64,
            // 0x64, 0x64, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 'O'};
            // 这里是速度 速度只有一个byte 那么只取最后一个byte 16 17 18 19
            // float sp = Utils.byte2float(data, 16);
            // byte sp = data[19];
            // sp = sp + 100;//这里加一百。
            // byte speed = (byte) (sp + 100);
            motorChoice(data[15], data[19]);
            // 这里是电机0~3.
            // if (data[15] == 0x00) {
            // // motor[8] = speed;
            // dc0speed = speed;
            // } else if (data[15] == 0x01) {
            // // motor[9] = speed;
            // dc1speed = speed;
            // } else if (data[15] == 0x02) {
            // // motor[10] = speed;
            // dc2speed = speed;
            // } else if (data[15] == 0x03) {
            // // motor[11] = speed;
            // dc3speed = speed;
            // }
            // motor[5] = (byte) dc0speed;
            // motor[6] = (byte) dc1speed;
            // motor[7] = (byte) dc2speed;
            // motor[8] = (byte) dc3speed;
            // writeBuff(motor);

            // 这里是runmovetime.
        } else if (data[6] == 0x02) {

            // flag = true;
            // byte[] motor = new byte[] { 'U', 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x64, 0x64, 0x64, 0x64, 0, 0, 0, 0, 0, 0, 0, 'O' };
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
            // 停止运动将所有的电机口置为100.
            // dc0speed = 100;
            // dc1speed = 100;
            // dc2speed = 100;
            // dc3speed = 100;
            // //flag = false;
            // //mthread.interrupt();
            // byte[] motorstop = new byte[] { 'C', 'S', 'E', 'T', 'M', 0x64,
            // 0x64,
            // 0x64, 0x64, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 'O'};
            // writeBuff(motorstop);
            sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x14, null);// 停止所有电机
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
            // strReturn = getParam(str, 10);
            int count = 0;
            int num = 0;
            int value = 0;
            for (int i = 0; i < 5; i++) {

                value = ReadColorValue(); // 获取颜色传感器的值
                LogMgr.e("color value is: " + value);
                if (value < 0) {
                    // 如果小于0 就是没有检测到值。这个是不参与计算的。
                    value = 0;
                    count++;
                }
                if (data[15] == 0x00) {
                    if (value != 1) {
                        value = 0;
                    }
                    // 绿色
                } else if (data[15] == 0x01) {

                    if (value != 2) {
                        value = 0;
                    } else {
                        value = 1;

                    }
                    // 蓝色
                } else if (data[15] == 0x02) {

                    if (value != 3) {
                        value = 0;
                    } else {
                        value = 1;
                    }
                    // 现改为白色0x03
                } else if (data[15] == 0x03) {

                    if (value != 5) {
                        value = 0;
                    } else {
                        value = 1;
                    }
                    // 黄色改为0x04
                } else if (data[15] == 0x04) {

                    if (value != 6) {
                        value = 0;
                    } else {
                        value = 1;
                    }

                } else {

                    value = 0;
                }
                num += value;
            }
            if (count < 5 && num >= 1 && num > 3 - count) {

                value = 1;

            } else {
                value = 0;
            }

            dealwith(value, data, 1);
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
//			byte[] temp = new byte[4];
//			System.arraycopy(data, 15, temp, 0, temp.length);
//			LogMgr.e("1111111111    :" + Utils.bytesToString(temp, temp.length));
//			SendScratch(3, temp);

            Record_pre = System.currentTimeMillis();
            if (Record_pre - record_next > 3500) {
                record_next = Record_pre;
                byte[] temp = new byte[4];
                System.arraycopy(data, 15, temp, 0, temp.length);
                if (Utils.byteAray2Int(temp) > 0) {
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

            // byte[] bybuff = new byte[4];
            // SendScratch(4, bybuff);
        }

    }

    /************************ 函数 ********************************/
    // 获取Type型号的AI所在的位置
    private int ReadAIType(int type) {
        byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'S', 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 'O'};
        for (int n = 0; n < 30; n++) {
            byte[] readbuff = null;
            for (int m = 0; m < 40; m++)
                readbuff[m] = 0x00;

            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            // ///////////////循环查找第一个字节//////////////////
            // LogMgr.e("ReadAIType buf is: "+Utils.bytesToString(readbuff,
            // readbuff.length));
            for (int i = 0; i < 20; i++) {
                if (readbuff[i] == 'C' && readbuff[i + 1] == 'G'
                        && readbuff[i + 2] == 'E' && readbuff[i + 3] == 'T') {
                    byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, tempbuff, 0, 20);
                    System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    i = 50;
                }
            }
            // /////////////////////////////////////////////

            if (readbuff[0] == 'C' && readbuff[1] == 'G' && readbuff[2] == 'E'
                    && readbuff[3] == 'T' && readbuff[4] == 'S') {
                int min = 0, max = 0, value = 0;
                if (type == 0) { // 超声 -- 测试OK
                    min = 1640;
                    max = 2260;
                } else if (type == 1) { // 按钮 -- 测试OK
                    min = 100;
                    max = 410;
                } else if (type == 2) { // 灰度

                    min = 820;
                    max = 1230;
                }
                for (int i = 5; i < 19; i += 2) {
                    value = (int) ((readbuff[i + 1] & 0xFF) | ((readbuff[i] & 0xFF) << 8));
                    if (value > min && value < max) { // 查找到Type所在的AI
                        return (i - 5) / 2;
                    }
                }
            }
        }
        return -1;
    }

    // 获取颜色传感器值
    private int ReadColorValue() {
        byte[] readbuff = null;
        for (int m = 0; m < 40; m++)
            readbuff[m] = 0x00;

        byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'C', 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 'O'};
        for (int n = 0; n < 5; n++) {
            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            // ///////////////循环查找第一个字节//////////////////
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
                                return 6;
                            } else {
                                // self.colorView.randomView.backgroundColor =
                                // [UIColor whiteColor];
                                return 5;
                            }

                        } else {
                            // self.colorView.randomView.backgroundColor
                            // =[UIColor yellowColor];
                            return 6;
                        }
                    } else {
                        if ((h == 0 && s == 0 && l == 240) || (l > 150)) {
                            // self.colorView.randomView.backgroundColor =
                            // [UIColor whiteColor];
                            return 5;
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
        return -1;
    }

    // 获取AI值
    // private int ReadAIValue(int type) {
    // byte[] readbuff = new byte[40];
    // for (int m = 0; m < 40; m++)
    // readbuff[m] = 0x00;
    //
    // byte[] AIBuff = new byte[] { 'U', 'B', 'L', 'E', 'G', 'E', 'T', 0, 0,
    // 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'O' };
    // for (int n = 0; n < 30; n++) {
    // try {
    // SerialPortCommunicator.getInstance().write(AIBuff, 10);
    // SerialPortCommunicator.getInstance().read(readbuff);
    // } catch (IOException e) {
    // e.printStackTrace();
    // return -1;
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // return -1;
    // }
    // // ///////////////循环查找第一个字节//////////////////
    // //LogMgr.e("ReadAIvalue buf is: "+Utils.bytesToString(readbuff,
    // readbuff.length));
    // for (int i = 0; i < 20; i++) {
    // if (readbuff[i] == 'U') {
    // byte[] tempbuff = new byte[20];
    // System.arraycopy(readbuff, i, tempbuff, 0, 20);
    // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
    // i = 50;
    // // n = 30; 下面直接return了 对的。 这里还是没问题的。
    // }
    // }
    // // /////////////////////////////////////////////
    // if (readbuff[0] == 85 && readbuff[1] == 'B' && readbuff[2] == 'L'
    // && readbuff[3] == 'E' && readbuff[4] == 'A'
    // && readbuff[5] == 'C' && readbuff[6] == 'K'
    // && readbuff[7] == 0x00) {
    // if (type >= 0 && type <= 5) {
    // int dx = type * 2;
    // return (readbuff[8 + 1 + dx] & 0xFF | (readbuff[8 + dx] & 0XFF) << 8);
    //
    // } else if (type >= 6 && type <= 11) {
    // int dx = type % 2;
    // return readbuff[8 + type + 1 - 6 + dx] & 0xFF
    // | (readbuff[8 + type + 6 + dx] & 0XFF) << 8;
    // }
    // }
    // }
    // return -1;
    // }
    // 新协议。
    private int ReadAIValue(int type) {
        byte[] readbuff = null;
        for (int m = 0; m < 40; m++)
            readbuff[m] = 0x00;

        byte[] AIBuff = new byte[]{'C', 'G', 'E', 'T', 'A', 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 'O'};
        for (int n = 0; n < 30; n++) {
            try {
                readbuff = SP.request(AIBuff);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            // ///////////////循环查找第一个字节//////////////////
            // LogMgr.e("ReadAIvalue buf is: "+Utils.bytesToString(readbuff,
            // readbuff.length));
            for (int i = 0; i < 20; i++) {
                if (readbuff[i] == 'C') {
                    byte[] tempbuff = new byte[20];
                    System.arraycopy(readbuff, i, tempbuff, 0, 20);
                    System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                    i = 50;
                    // n = 30; 下面直接return了 对的。 这里还是没问题的。
                }
            }
            // /////////////////////////////////////////////
            if (readbuff[0] == 'C' && readbuff[1] == 'G' && readbuff[2] == 'E'
                    && readbuff[3] == 'T' && readbuff[4] == 'A') {
                if (type >= 0 && type <= 7) {
                    int dx = type * 2;
                    return (readbuff[5 + 1 + dx] & 0xFF | (readbuff[5 + dx] & 0xFF) << 8);

                } else if (type >= 8 && type <= 11) {
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

    public void motorChoice(int port, int sudu) {
        if (sudu < -100) {
            sudu = -100;
        } else if (sudu > 100) {
            sudu = 100;
        }
        sudu = sudu + 100;
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
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x13, data);
    }

    public void sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data) {
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
        try {
            SP.write(sendbuff);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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

    }

}
