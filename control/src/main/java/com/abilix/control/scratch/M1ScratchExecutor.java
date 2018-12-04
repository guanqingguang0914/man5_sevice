package com.abilix.control.scratch;


import android.os.Handler;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import static com.abilix.control.uav.DataBuffer.readData;

public class M1ScratchExecutor extends AbstractScratchExecutor {

    protected MySensor mSensor;

    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;

    public M1ScratchExecutor(Handler mHandler) {
        super(mHandler);

        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e("come function" + Utils.bytesToString(data));
        checkFunction(data);
    }

    @Override
    public void clearState() {
        // LogMgr.e("线程:" + Thread.currentThread());
        // LogMgr.e("Main:"+Looper.getMainLooper().getThread());

        record_next = 0;
        pic_next = 0;
        mPlayer.stop();
//		try {
//			byte[] data = { (byte) 0xAA, 0x55, 00, 0x08, 0x0B, 0x11, 0x0B, 00, 00, 00, 00, (byte) 0x2E };
//			SP.write(data);
//			TimeUnit.MILLISECONDS.sleep(18);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

    }

    /**
     * @param data
     * @Description 命令解析
     * @author lz
     * @time 2017-5-8 下午3:31:25
     */
    private void checkFunction(final byte[] data) {
        if (data[5] != (byte) 0x0e) {
            LogMgr.e("不是M1、M2类型机器人");
            return;
        }
        // 运动向前 向后。
        if (data[6] == 0x01) {
            // byte[] motor = new byte[] { 0x55, 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0,
            // 0, 0, 0 };
            byte[] sudubuff = new byte[4];
            System.arraycopy(data, 16, sudubuff, 0, sudubuff.length);
            int speed = Utils.byteAray2Int(sudubuff);
            LogMgr.d("speed:" + speed);
            if (speed > 60) {
                speed = 60;
            } else if (speed < -60) {// 因为这里前后已经有方向了。范围就是0~100。
                speed = -60;
            }
            if (data[15] == 0x00) {
                speed = (byte) (speed + 100);
                // 向后
            } else if (data[15] == 0x01) {
                speed = (byte) (100 - speed);
            }
            byte[] bs = new byte[8];
            bs[0] = (byte) 0x03;
            bs[1] = (byte) ((int) speed);
            bs[2] = (byte) ((int) speed);
            combinationData((byte) 0x3b, bs);
            // 左转 右转 转速
        } else if (data[6] == 0x02) {
            // byte[] motor = new byte[] { 0x55, 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0,
            // 0, 0, 0 };
            // 这里是速度 速度只有一个byte 那么只取最后一个byte 16 17 18 19
            byte[] sudubuff = new byte[4];
            System.arraycopy(data, 16, sudubuff, 0, sudubuff.length);
            int speed = Utils.byteAray2Int(sudubuff);
            if (speed > 60) {
                speed = 60;
            } else if (speed < -60) {
                speed = -60;
            }
            byte[] bs = new byte[8];
            bs[0] = (byte) 0x03;
            bs[1] = (byte) ((int) speed);
            bs[2] = (byte) ((int) speed);
            // 左转。
            if (data[15] == 0x00) {
                speed = (byte) (speed + 100);
                bs[0] = (byte) 0x01;
                // 左转左轮停止
                bs[1] = (byte) ((int) 100);
                bs[2] = (byte) ((int) speed);
                // 右转
            } else if (data[15] == 0x01) {
                speed = (byte) (100 + speed);
                bs[0] = (byte) 0x02;
                // 右转右轮停止。
                bs[1] = (byte) ((int) speed);
                bs[2] = (byte) ((int) 100);
            }
            combinationData((byte) 0x3b, bs);
            // 顺时针 逆时针 转速 时长。
        } else if (data[6] == 0x03) {

            // byte[] motor = new byte[] { 0x55, 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0,
            // 0, 0, 0 };
            byte[] bs = new byte[8];
            bs[0] = (byte) 0x03;
            // 顺时针
            if (data[15] == 0x00) {
                // 速度
                byte speedleft = (byte) (data[19] + 100);
                byte speedright = (byte) (100 - data[19]);
                bs[1] = (byte) ((int) speedleft);
                bs[2] = (byte) ((int) speedright);
                // 逆时针
            } else if (data[15] == 0x01) {
                byte speedleft = (byte) (data[19] + 100);
                byte speedright = (byte) (100 - data[19]);
                bs[1] = (byte) ((int) speedleft);
                bs[2] = (byte) ((int) speedright);
            }
            combinationData((byte) 0x3b, bs);
            try {
                Thread.sleep((int) data[23] * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 这里的stop 左右轮速度都设为0.
            bs[1] = (byte) ((int) 100);
            bs[2] = (byte) ((int) 100);
            combinationData((byte) 0x3b, bs);

            // 停止运动。
        } else if (data[6] == 0x04) {
            byte[] bs = new byte[8];
            bs[0] = (byte) 0x03;
            bs[1] = (byte) ((int) 100);
            bs[2] = (byte) ((int) 100);
            combinationData((byte) 0x3b, bs);
            // 脖子上下俯仰------------------------------------------------有问题更可能是设备问题。
        } else if (data[6] == 0x07) {
            byte[] nec = new byte[4];
            System.arraycopy(data, 15, nec, 0, 4);
            int angle = Utils.bytesToInt2(nec, 0);
            angle += 15;
            // 做一个阈值限定的过滤。
            if (angle > 45) {
                angle = 45;
            } else if (angle < 0) {
                angle = 0;
            }

            byte[] bs = Utils.intToBytes(angle);
            byte[] bs2 = new byte[3];
            bs2[0] = (byte) 0x01;
            System.arraycopy(bs, 0, bs2, 1, bs.length);
            combinationData((byte) 0x39, bs2);
            // 脖子左右转动。---------------------------------------------ok.
        } else if (data[6] == 0x08) {

            // byte[] motor = new byte[] { 0x55, 'N', 'E', 'C', 'K', 'M', 'O',
            // 'T', 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0,
            // 0, 0 };
            // motor[9] = (byte) Integer.parseInt(p1);
            // 这里写一个角度的转换。
            // float sp = Utils.byte2float(data, 16);
            // int spw = Utils.
            byte[] nec = new byte[4];
            System.arraycopy(data, 15, nec, 0, 4);
            int angle = Utils.bytesToInt2(nec, 0);

            angle += 130;
            if (angle > 260) {
                angle = 260;
            } else if (angle < 0) {
                angle = 0;
            }

            byte[] bs = Utils.intToBytes(angle);
            byte[] bs2 = new byte[3];
            bs2[0] = (byte) 0x00;
            System.arraycopy(bs, 0, bs2, 1, bs.length);
            combinationData((byte) 0x39, bs2);
            // 播放音乐
        } else if (data[6] == 0x09) {
            // LogMgr.e("111111111111");
            mPlayer.play();
            // 停止播放
        } else if (data[6] == 0x0A) {
            mPlayer.stop();
            // 眼睛LED。
        } else if (data[6] == 0x0B) {
            if (data[15] == 0x00) {
                byte[] bs = new byte[1];
                bs[0] = (byte) 0x01;
                combinationData((byte) 0x35, bs);
                // 红
            } else if (data[15] == 0x01) {
                setEyeColor(255, 0, 0);
                // 绿
            } else if (data[15] == 0x02) {
                setEyeColor(0, 255, 0);
                // 蓝
            } else if (data[15] == 0x03) {
                setEyeColor(0, 0, 255);
                // 全亮
            } else if (data[15] == 0x04) {
                setEyeColor(255, 255, 255);
            }
            // 脖子LED
        } else if (data[6] == 0x0C) {
            ledset(1, data[15], data[16]);
            LogMgr.d("bozi:" + "color: " + data[15] + "type: " + data[16]);
            // ledset(1, data[15], data[16]);
            // 底部LED
        } else if (data[6] == 0x0D) {

            LogMgr.d("底部:" + "color: " + data[15] + "type: " + data[16]);
            ledset(3, data[15], data[16]);
            // 轮子LED
        } else if (data[6] == 0x0E) {

            LogMgr.d("轮子:" + "color: " + data[15] + "type: " + data[16]);
            ledset(2, data[15], data[16]);
            // 判断是否有障碍物
        } else if (data[6] == 0x0F) {

            byte[] returnvalue = ReadAIValue((byte) 0x37, new byte[0], (byte) 0x30);
            byte[] bs = null;
            int pre_dis = 0;
            if (returnvalue.length > 12) {
                bs = Utils.getData(returnvalue);
                LogMgr.d("接收数据位：" + Utils.bytesToString(bs));
            }
            if (bs != null && bs.length > 3) {
                byte[] bs2 = new byte[2];
                System.arraycopy(bs, 2, bs2, 0, bs2.length);
                pre_dis = Utils.byteToInt(bs2);
            }
//			int next_dis = 0;
//			int hight = 0;
//			int low = 0;
//			hight = returnvalue[4];
//			low = returnvalue[5];
//			if (hight > 0) {
//				hight = hight << 8;
//			}
//			pre_dis = hight + (low & 255);
//			// 后端超声障碍物。
//			hight = 0;
//			low = 0;
//			hight = returnvalue[6];
//			low = returnvalue[7];
//			if (hight > 0) {
//				hight = (hight & 255) << 8;
//			}
//			next_dis = hight + (low & 255);

            if (pre_dis > 30 || pre_dis < 0) {
                pre_dis = 0;
            }
            if (data[15] == 1) {

//				if (next_dis > 30 || next_dis < 0) {
//					next_dis = 0;
//				}
//				dealwith(next_dis, data, 1);
            } else {
                dealwith(pre_dis, data, 1);
            }

            // 探测障碍物的距离。
        } else if (data[6] == 0x10) {
            byte[] returnvalue = ReadAIValue((byte) 0x37, new byte[0], (byte) 0x30);
            byte[] bs = null;
            int pre_dis = 0;
            if (returnvalue.length > 12) {
                bs = Utils.getData(returnvalue);
                LogMgr.d("接收数据位：" + Utils.bytesToString(bs));
            }
            if (bs != null && bs.length > 3) {
                byte[] bs2 = new byte[2];
                System.arraycopy(bs, 2, bs2, 0, bs2.length);
                pre_dis = Utils.byteToInt(bs2);
            }
//			// strReturn = getParam(str, 10);
//			byte[] motor = new byte[] { 0x56, 'A', 'I', 'R', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
//			byte[] returnvalue = ReadAIValue(motor);
//			LogMgr.e("碍物的距离 buff: " + Utils.bytesToString(returnvalue, returnvalue.length));
//			int pre_dis = 0; // 前方超声距离
//			int next_dis = 0;
//			int hight = 0;
//			int low = 0;
//			hight = returnvalue[4];
//			low = returnvalue[5];
//			if (hight > 0) {
//				hight = (hight & 255) << 8;
//			}
//			pre_dis = hight + (low & 255);
//			// 后端距离。
//			hight = 0;
//			low = 0;
//			hight = returnvalue[6];
//			low = returnvalue[7];
//			if (hight > 0) {
//				hight = (hight & 255) << 8;
//			}
//			next_dis = hight + (low & 255);

            if (data[15] == 1) {
//				dealwith(next_dis, data, 1);
            } else {
                dealwith(pre_dis, data, 1);
            }
            // 机器人碰撞到物体。
        } else if (data[6] == 0x11) {


            byte[] returnvalue = ReadAIValue((byte) 0x37, new byte[0], (byte) 0x30);
            byte[] bs = null;
            if (returnvalue.length > 12) {
                bs = Utils.getData(returnvalue);
                LogMgr.d("接收数据位：" + Utils.bytesToString(bs));
            }
            int collisionvalue = 0;
            if (bs != null && bs.length > 0) {
                int value = bs[0]; //
                if (data[15] == 0) {// 左前碰撞
                    if (value == 1) {
                        collisionvalue = 1;
                    } else {
                        collisionvalue = 0;
                    }
                } else if (data[15] == 1) {// 右前碰撞
                    if (value == 2) {
                        collisionvalue = 1;
                    } else {
                        collisionvalue = 0;
                    }
                } else if (data[15] == 2) {// 正前碰撞
                    if (value == 3) {
                        collisionvalue = 1;
                    } else {
                        collisionvalue = 0;
                    }
                }
                dealwith(collisionvalue, data, 1);
            }

//			LogMgr.e("come peng zhuang hanshu  ");
//			byte[] motor = new byte[] { 0x56, 'C', 'O', 'L', 'L', 'I', 'S', 'I', 'O', 'N', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
//			byte[] returnvalue = ReadObjectValue(motor);
//			int value = returnvalue[10]; // 碰撞data[10] 就是碰撞。
//			int collisionvalue = 0;
//			// copy 解释执行代码。
//			LogMgr.e("zhuang hanshu value is: " + value);
//			if (data[15] == 0) {// 左前碰撞
//				if (value == 1) {
//					collisionvalue = 1;
//				} else {
//					collisionvalue = 0;
//				}
//			} else if (data[15] == 1) {// 右前碰撞
//				if (value == 2) {
//					collisionvalue = 1;
//				} else {
//					collisionvalue = 0;
//				}
//			} else if (data[15] == 2) {// 正前碰撞
//				if (value == 3) {
//					collisionvalue = 1;
//				} else {
//					collisionvalue = 0;
//				}
//			}
//			dealwith(collisionvalue, data, 1);

            // 地面灰度。 地面灰度需要先开启后读取
        } else if (data[6] == 0x13) {

            byte[] returnvalue = ReadAIValue((byte) 0x38, new byte[0], (byte) 0x31);
            byte[] bs = null;
            if (returnvalue.length > 12) {
                bs = Utils.getData(returnvalue);
                LogMgr.d("接收数据位：" + Utils.bytesToString(bs));
            }

            if (bs != null && bs.length > 4) {
                int value = bs[data[15] - 1];
                dealwith(value, data, 1);
            }

//			int value = 0;
//			byte[] revalue = Read2gray();
//			LogMgr.e("gray buff: " + Utils.bytesToString(revalue, revalue.length));
//			// 这里只要第三个值。
//			int hight = 0;
//			int low = 0;
//			int grayIndex = data[18] * 2 + 5;
//			hight = revalue[grayIndex];
//			low = revalue[grayIndex + 1];
//			if (hight > 0) {
//				hight = (hight & 0xff) << 8;
//			}
//			value = hight + (low & 0xff);
//			dealwith(value, data, 1);

            // 这里直接返回角度。
        } else if (data[6] == 0x14) {
            // 东南西北 0~3.
            float value = 0;
            // float[] SN =mSensor.getmO();
            // LogMgr.e("sn is null: "+(SN==null));
            value = mSensor.getMcompass();

            if (value != 0) {

                dealwith((int) value, data, 1);
            }

            // 下俯，上扬，左翻，右翻。
        } else if (data[6] == 0x15) {

            int value = 0;
            float[] SN = mSensor.getmO();
            LogMgr.e("SN value is : " + (SN == null));
            // 上下是value[1].下正 上负。
            if (SN != null) {
                // 对于上扬 下府这个值是需要减去70的。
                if (data[15] == 0) {
                    // 对于下府取10~90度之间。
                    if (SN[1] >= (10 + 70) && SN[1] <= (90 + 70)) {
                        value = 1;
                    }
                    // 上扬。
                } else if (data[15] == 1) {
                    // 这里应该取-10 ~ -90之间。 基础值是70度。
                    if (SN[1] >= (70 - 90) && SN[1] <= (70 - 10)) {
                        value = 1;
                    }
                    // 左翻，右翻。 左正右负。 左右翻转的值是对的。
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

                LogMgr.e("上下 vlaue[1] is: " + SN[1] + "左右value[2] is: " + SN[2]);
                dealwith(value, data, 1);

            }

        } else if (data[6] == 0x16) {// 下视。


            byte[] returnvalue = ReadAIValue((byte) 0x37, new byte[0], (byte) 0x30);
            byte[] bs = null;
            int pre_dis = 0;
            if (returnvalue.length > 12) {
                bs = Utils.getData(returnvalue);
                LogMgr.d("接收数据位：" + Utils.bytesToString(bs));
            }
            if (bs == null || bs.length < 2) {
                LogMgr.e("接收数据出错：" + Utils.bytesToString(bs));
                return;
            }
            int value = bs[1];
            LogMgr.d("下视value is：  " + value);
            int lookDown = 0;
            if (value == 0) {
                lookDown = 0;
            } else {
                lookDown = 1;
            }
            dealwith(lookDown, data, 1);
//			switch (data[15]) {
//
//			case 0:// 是否后左悬空。
//				if (value == 2) {
//					lookDown = 1;
//				} else {
//					lookDown = 0;
//				}
//				break;
//			case 1:// 是否后中悬空
//				if (value == 1) {
//					lookDown = 1;
//				} else {
//					lookDown = 0;
//				}
//				break;
//			case 2:// 是否后右悬空
//				if (value == 0) {// 全悬挂是0.
//					lookDown = 1;
//				} else {
//					lookDown = 0;
//				}
//				break;
//			case 3:// 是否前左悬空
//				if (value == 0) {// 全悬挂是0.
//					lookDown = 1;
//				} else {
//					lookDown = 0;
//				}
//				break;
//			case 4:// 是否前右悬空
//				if (value == 0) {// 全悬挂是0.
//					lookDown = 1;
//				} else {
//					lookDown = 0;
//				}
//				break;
//
//			}


//			byte[] motor = new byte[] { 0x56, 'L', 'O', 'O', 'K', 'D', 'O', 'W', 'N', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
//			byte[] returnvalue = ReadFlyValue(motor);
//			int value = returnvalue[9];// 0没有 1后左 2后右 3后左右。
            // if (value == 0) {
            // value = 3;
            // } else if (value == 3) {
            // value = 0;
            // }else if(value == 2){
            // value = 1;
            // }else if(value == 1){
            // value = 2;
            // }


        } else if (data[6] == 0x17) { // 录音功能。

            byte[] temp = new byte[4];
            System.arraycopy(data, 15, temp, 0, temp.length);
            int time = Utils.byteAray2Int(temp);
            Record_pre = System.currentTimeMillis();
            if (Record_pre - record_next > 3500 && Record_pre - record_next > time * 1000) {
                record_next = Record_pre;

                if (Utils.byteAray2Int(temp) > 0) {
                    SendScratch(3, temp);
                }

            }
            // 时钟复位。
        } else if (data[6] == 0x18) {
            dealwith(0, data, 1); // 这里没有意义都返回0
            // 系统时间。
        } else if (data[6] == 0x19) {
            dealwith(0, data, 1);// 这里没有意义都返回0
            // 显示的是1a.
        } else if (data[6] == 0x1A) {
            // 显示 命令不做。
            // 拍照
        } else if (data[6] == 0x1C) {

            pic_pre = System.currentTimeMillis();
            if (pic_pre - pic_next > 3500) {
                pic_next = pic_pre;
                byte[] temp = new byte[1];
                temp[0] = 1;
                SendScratch(2, temp);
            }

        } else if (data[6] == 0x1D) { // 校准指南针。

            // byte[] temp = new byte[2];
            // SendScratch(4, temp);

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

        } else if (data[6] == 0x1F) {// 控制机器人播放乐器声音

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

        } else if (data[6] == 0x20) {// 控制机器人播放自我介绍。

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
            // } else if (data[6] == 0x21) {//
            // 左右伦速度。---------------------------???????
            //
            // byte[] motor = new byte[] { 0x55, 'B', 'L', 'E', 'S', 'E', 'T',
            // 0x03, 0x00, 0x00, 0x00, 0, 0, 0, 0, 0, 0,
            // 0, 0, 0 };
            //
            // // 这里需要解析下左右轮速度。然后填进去。
            // byte[] left = new byte[4];
            // System.arraycopy(data, 15, left, 0, 4);
            // int leftspeed = Utils.bytesToInt2(left, 0);
            // if (leftspeed > 100) {
            // leftspeed = 100;
            // } else if (leftspeed < -100) {
            // leftspeed = -100;
            // }
            //
            // byte[] right = new byte[4];
            // System.arraycopy(data, 19, right, 0, 4);
            // int rightspeed = Utils.bytesToInt2(right, 0);
            // if (rightspeed > 100) {
            // rightspeed = 100;
            // } else if (rightspeed < -100) {
            // rightspeed = -100;
            // }
            // LogMgr.e("leftspeed: " + leftspeed + "rightspeed: " +
            // rightspeed);
            // // byte speed = (byte) (100 + data[19]);
            // // 右转右轮停止。
            // motor[8] = (byte) (leftspeed + 100);
            // motor[9] = (byte) (rightspeed + 100);
            //
            // combinationData(motor);
        }

    }

    /**
     * 设置眼睛灯光
     */
    private void setEyeColor(int r, int g, int b) {
        byte[] bs = new byte[30];
        for (int i = 0; i < 10; i++) {
            bs[i * 3] = (byte) r;
            bs[i * 3 + 1] = (byte) g;
            bs[i * 3 + 2] = (byte) b;
        }
        combinationData((byte) 0x31, bs);
    }

    /**
     * @param head  脖子1，轮子2，底部3
     * @param color 0-3 红绿蓝白
     * @param type  0-3 正弦，方波，常亮，常灭
     * @Description 设置灯光
     * @author lz
     * @time 2017-5-9 上午10:28:31
     */
    public void ledset(int head, int color, int type) {
        setWaveform(head, type + 1);
        switch (head) {
            case 1:
                setColor((byte) 0x32, color);
                break;
            case 2:
                setColor((byte) 0x34, color);
                break;
            case 3:
                setColor((byte) 0x33, color);
                break;

            default:
                break;
        }
    }


    private void setColor(byte cmd, int color) {
        byte[] bs = new byte[3];
        switch (color) {
            case 0:
                bs[0] = (byte) 255;
                bs[1] = (byte) 0;
                bs[2] = (byte) 0;
                break;
            case 1:
                bs[0] = (byte) 0;
                bs[1] = (byte) 255;
                bs[2] = (byte) 0;
                break;
            case 2:
                bs[0] = (byte) 0;
                bs[1] = (byte) 0;
                bs[2] = (byte) 255;
                break;
            case 3:
                bs[0] = (byte) 255;
                bs[1] = (byte) 255;
                bs[2] = (byte) 255;
                break;
            default:
                break;
        }
        combinationData(cmd, bs);
    }

    /**
     * @param type
     * @Description 设置波形
     * @author lz
     * @time 2017-5-9 上午11:05:27
     */
    public void setWaveform(int type, int wave) {
        byte[] bs = new byte[2];
        bs[0] = (byte) type;
        bs[1] = (byte) wave;
        combinationData((byte) 0x36, bs);
    }

    /**
     * @Description 组合数据
     * @author lz
     * @time 2017-5-8 下午4:52:35
     */
    private void combinationData(byte cmd2, byte[] buff) {
        byte[] bs = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, cmd2, buff);
        Log.d("lz", "写的数据:" + Utils.bytesToString(bs));
        writeBuff(bs);
    }

    /**
     * @param writeCmd 写的命令字
     * @param val      发送数据位
     * @param readCmd  读的命令字
     * @return
     * @Description 为M设备添加读取返回值的方法
     * @author lz
     * @time 2017-5-9 下午1:24:55
     */
    private byte[] ReadAIValue(byte writeCmd, byte[] val, byte readCmd) {
        byte[] rest = new byte[20];
        // // 数组初始化。
        // for (int m = 0; m < 40; m++) {
        // if (m < 20) {
        // rest[m] = readbuff[m] = 0x00;
        // } else {
        // readbuff[m] = 0x00;
        // }
        // }
        byte[] AIBuff = new byte[20];
        System.arraycopy(val, 0, AIBuff, 0, 20);// 这里将20个字节的命令拷贝进来。
        // 这里的循环保证成功。
        for (int n = 0; n < 30; n++) {
            try {
                combinationData(writeCmd, val);
                Thread.sleep(10);
                byte[] readbuff = readData();
                Thread.sleep(1);
                if (readbuff == null) {
                    continue;
                }
                // ///////////////循环查找第一个字节//////////////////
                for (int i = 0; i < readbuff.length - 12; i++) {
                    if ((readbuff[i] & 0xFF) == 0xaa && (readbuff[i + 1] & 0xFF) == 0x55
                            && (readbuff[i + 5] & 0xFF) == 0xf3 && (readbuff[i + 6] & 0xFF) == readCmd) {
                        // byte[] tempbuff = new byte[20];
                        System.arraycopy(readbuff, i, rest, 0, (readbuff.length - i) > 20 ? 20 : (readbuff.length - i));
                        // System.arraycopy(tempbuff, 0, readbuff, 0, 20);
                        // i = 50;
                        return rest;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return rest;
            }
        }

        return rest;
    }

    @Override
    protected void writeBuff(byte[] buff) {
        try {
            SP.write(buff);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("write data error::" + e);
        }
    }

}
