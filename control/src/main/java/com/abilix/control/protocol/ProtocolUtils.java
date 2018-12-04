package com.abilix.control.protocol;


import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.EngineTestActivity;
import com.abilix.control.GlobalConfig;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;



public class ProtocolUtils {

    private static final byte MULTI_MEDIA_AUDIO = (byte) 0x01;
    private static final byte MULTI_MEDIA_VEDIO = (byte) 0x02;
    private static final byte MULTI_MEDIA_PIC = (byte) 0x03;

    public static final int ENGINE_STATE_FIXED = 1;
    public static final int ENGINE_STATE_RELEASE = 2;
    public static final int ENGINE_STATE_RETURN_ZERO = 3;

    /**
     * 数据头 不需要返回
     */
    public final static int DATA_HEAD = 0x55;

    /**
     * 数据头 返回
     */
    public final static int DATA_HEAD_ = 0x56;

    /**
     * 1 ONE
     */
    public final static int DATA_ONE = 0x01;

    /**
     * 2 TWO
     */
    public final static int DATA_TWO = 0x02;

    /**
     * 3 THREE
     */
    public final static int DATA_THREE = 0x03;

    /**
     * 4 FOUR
     */
    public final static int DATA_FOUR = 0x04;

    /**
     * 5 FIVE
     */
    public final static int DATA_FIVE = 0x05;

    /**
     * 6 SIX
     */
    public final static int DATA_SIX = 0X06;

    /**
     * 0 ZERO
     */
    public final static int DATA_ZERO = 0x00;
    /**
     * 轮子协议
     */
    public final static byte A = 'A';

    public final static byte B = 'B';

    public final static byte C = 'C';

    public final static byte D = 'D';

    public final static byte E = 'E';

    public final static byte F = 'F';

    public final static byte G = 'G';

    public final static byte H = 'H';

    public final static byte I = 'I';

    public final static byte J = 'J';

    public final static byte K = 'K';

    public final static byte L = 'L';

    public final static byte M = 'M';

    public final static byte N = 'N';

    public final static byte O = 'O';

    public final static byte P = 'P';

    public final static byte Q = 'Q';

    public final static byte DR = 'R';

    public final static byte S = 'S';

    public final static byte T = 'T';

    public final static byte U = 'U';

    public final static byte V = 'V';

    public final static byte W = 'W';

    public final static byte X = 'X';

    public final static byte Y = 'Y';

    public final static byte Z = 'Z';

    /**
     * 轮子协议
     */
    public static byte[] WHEELBYTE = {DATA_HEAD, B, L, E, S, E, T, 0X03, 100,
            100, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};

    /**
     * 头部
     */
    public static byte[] HEADBYTE = {DATA_HEAD, N, E, C, K, M, O, T, 0X00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    /**
     * 设置眼睛颜色
     */
    public static byte[] EYE_COLOR = {DATA_HEAD, E, Y, 1, 0X00, 0X00, 0X00,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00,
            0X00, 0X00};

    /**
     * 设置眼睛数量
     */
    public static byte[] EYE_COUNT = {DATA_HEAD, E, Y, 2, 0X00, 0X00, 0X00,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00,
            0X00, 0X00};

    /**
     * 设置颜色
     */
    public static byte[] COLOR = {DATA_HEAD, C, O, L, E, D, 0X00, 0X00, 0X00,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};

    /**
     * 设置亮度
     */
    public static byte[] LUMINANCE = {DATA_HEAD, L, U, M, I, N, A, N, C, E,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};

    /**
     * 设置波形
     */
    public static byte[] WAVEMODE = {DATA_HEAD, W, A, V, E, F, O, DR, M, 0X00,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};

    /**
     * 设置吸尘
     */
    public static byte[] VACUUM = {DATA_HEAD, B, L, E, S, E, T, 0X04, 0X00,
            0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};
    /**
     * 超声波 协议
     */
    public static final byte[] mUltrasonicByte = {DATA_HEAD_, A, I, DR, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00};

    /**
     * 下视 协议
     */
    public static final byte[] mDown_WatchByte = {DATA_HEAD_, L, O, O, K, D,
            O, W, N, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00};

    /**
     * 后端红外测距
     */
    public static final byte[] mRear_End_InfraredByte = {DATA_HEAD_, I, N, F,
            DR, A, 0x00, 0x00, 0X00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00};

    /**
     * 碰撞测试
     */
    public static final byte[] mCrashInfraredByte = {DATA_HEAD_, C, O, L, L,
            I, S, I, O, N, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00};
    /**
     * 地面灰度 打开
     */
    public static final byte[] mGround_Grayscale_OpenByte = {DATA_HEAD, G, DR,
            A, Y, O, P, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00};
    /**
     * 地面灰度 发送
     */
    public static final byte[] mGround_Grayscale_SendByte = {DATA_HEAD_, G,
            DR, A, Y, DR, D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00};

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
        LogMgr.d("buildProtocol----1");
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

    /**
     * 增加协议头前两个字节为AA 55，三，四字节为长度，最后一个字节为校验位
     *
     * @param buff 类型，命令字1，命令字2，保留字，数据位
     * @return
     */
    public static byte[] addProtocol(byte[] buff) {
        short len = (short) (buff.length + 5);
        byte[] sendbuff = new byte[len];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        // 扫描二维码协议更改为高位在前，地位在后
        sendbuff[3] = (byte) ((len - 4) & 0x00FF); // 长度: 从type到check
        sendbuff[2] = (byte) (((len - 4) >> 8) & 0x00FF);
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data

        byte check = 0x00; // 校验位
        for (int n = 0; n < len - 1; n++) {
            check += sendbuff[n];
        }
        sendbuff[len - 1] = (byte) (check & 0x00FF);
        return sendbuff;
    }

    /**
     * M机器人轮子速度命令构造
     *
     * @param leftWheelSpeed  左轮速度 0-200
     * @param rightWheelSpeed 右轮速度 0-200
     */
    public static byte[] getMWheelSpeedCmd(byte leftWheelSpeed, byte rightWheelSpeed) {

        byte leftWheelSpeedToSet = leftWheelSpeed;
        byte rightWheelSpeedToSet = rightWheelSpeed;
//		byte leastForwordSpeed = (byte)(105&0xFF);
//		byte leastBackSpeed = (byte)(95&0xFF);
//		
//		int leftWheelSpeedInt = (int)(leftWheelSpeedToSet&0xFF);
//		int rightWheelSpeedInt = (int)(rightWheelSpeedToSet&0xFF);
//		if(leftWheelSpeedInt >= 101 && leftWheelSpeedInt <= 104){
//			leftWheelSpeedToSet = leastForwordSpeed;
//		}else if(leftWheelSpeedInt <= 99 && leftWheelSpeedInt >= 96){
//			leftWheelSpeedToSet = leastBackSpeed;
//		}
//		
//		if(rightWheelSpeedInt >= 101 && rightWheelSpeedInt <= 104){
//			rightWheelSpeedToSet  = leastForwordSpeed;
//		}else if(rightWheelSpeedInt <= 99 && rightWheelSpeedInt >= 96){
//			rightWheelSpeedToSet = leastBackSpeed;
//		}

        byte[] result = new byte[20];

        result[0] = (byte) 0x55;
        result[1] = (byte) 'B';
        result[2] = (byte) 'L';
        result[3] = (byte) 'E';
        result[4] = (byte) 'S';
        result[5] = (byte) 'E';
        result[6] = (byte) 'T';
        result[7] = (byte) 0x03;
        result[8] = leftWheelSpeedToSet;
        result[9] = rightWheelSpeedToSet;

        return result;
    }

    /**
     * 是否M轮子命令
     * @param cmd
     * @return
     */
    public static boolean isMWheelSpeedCmd(byte[] cmd){
        boolean result = false;
        if(cmd!=null && cmd.length == 20 &&
                cmd[0] == (byte) 0x55 &&
                cmd[1] == (byte) 'B' &&
                cmd[2] == (byte) 'L' &&
                cmd[3] == (byte) 'E' &&
                cmd[4] == (byte) 'S' &&
                cmd[5] == (byte) 'E' &&
                cmd[6] == (byte) 'T' &&
                cmd[7] == (byte) 0x03 ){
            result = true;
        }
        return result;
    }

    /**
     * M机器人轮子速度命令构造 新协议
     *
     * @param leftWheelSpeed  左轮速度 0-200
     * @param rightWheelSpeed 右轮速度 0-200
     */
    public static byte[] getMWheelSpeedCmdNew(byte leftWheelSpeed, byte rightWheelSpeed) {
        byte[] data = new byte[7];
        data[0] = leftWheelSpeed;
        data[1] = rightWheelSpeed;
        data[2] = (byte) 0x00;

        byte[] result = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_M, GlobalConfig.M_WHEEL_SPEED_OUT_CMD_1, GlobalConfig.M_WHEEL_SPEED_OUT_CMD_2, data);

        return result;
    }

    /**
     * M机器人眼睛颜色命令构造
     *
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static byte[] getMEyeColorCmd(byte red, byte green, byte blue) {
        byte[] result = new byte[20];

        result[0] = (byte) 0x55;
        result[1] = (byte) 'C';
        result[2] = (byte) 'O';
        result[3] = (byte) 'L';
        result[4] = (byte) 'O';
        result[5] = (byte) 'R';
        result[6] = (byte) 0x00;
        result[7] = red;
        result[8] = green;
        result[9] = blue;

        return result;

    }

    /**
     * M机器人脖子颜色命令构造
     *
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static byte[] getMNeckColorCmd(byte red, byte green, byte blue) {
        byte[] result = new byte[20];

        result[0] = (byte) 0x55;
        result[1] = (byte) 'C';
        result[2] = (byte) 'O';
        result[3] = (byte) 'L';
        result[4] = (byte) 'E';
        result[5] = (byte) 'D';
        result[6] = (byte) 0x01;
        result[7] = red;
        result[8] = green;
        result[9] = blue;

        return result;

    }

    /**
     * M机器人底部颜色命令构造
     *
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static byte[] getMBottomColorCmd(byte red, byte green, byte blue) {
        byte[] result = new byte[20];

        result[0] = (byte) 0x55;
        result[1] = (byte) 'C';
        result[2] = (byte) 'O';
        result[3] = (byte) 'L';
        result[4] = (byte) 'E';
        result[5] = (byte) 'D';
        result[6] = (byte) 0x04;
        result[7] = red;
        result[8] = green;
        result[9] = blue;

        return result;

    }

    /**
     * M机器人脖子动作命令构造
     *
     * @param motoType 0:左右电机 1：俯仰电机
     * @param angle    俯仰电机角度范围-37～+16，左右电机角度范围 -130～+130
     * @return
     */
    public static byte[] getMNeckMoveCmd(byte motoType, byte angle) {
        byte[] result = new byte[20];

        result[0] = (byte) 0x55;
        result[1] = (byte) 'N';
        result[2] = (byte) 'E';
        result[3] = (byte) 'C';
        result[4] = (byte) 'K';
        result[5] = (byte) 'M';
        result[6] = (byte) 'O';
        result[7] = (byte) 'T';
        result[8] = motoType;
        result[9] = angle;

        return result;
    }

    /**
     * M机器人脖子动作命令构造
     *
     * @param motoType 0:左右电机 1：俯仰电机
     * @param angle    俯仰电机角度范围-37～+16，左右电机角度范围 -130～+130
     * @return
     */
    public static byte[] getMNeckMoveCmd(byte motoType, byte angle, byte angle2) {
        byte[] result = new byte[20];

        result = getMNeckMoveCmd(motoType, angle);
        result[10] = angle2;

        // result[0] = (byte)0x55;
        // result[1] = (byte)'N';
        // result[2] = (byte)'E';
        // result[3] = (byte)'C';
        // result[4] = (byte)'K';
        // result[5] = (byte)'M';
        // result[6] = (byte)'O';
        // result[7] = (byte)'T';
        // result[8] = motoType;
        // result[9] = angle;

        return result;
    }

    /**
     * @param speedL
     * @param speedR
     * @return
     * @Description M1左右轮速度
     * @author lz
     * @time 2017-4-21 下午4:25:46
     */
    public static byte[] getM1WheelSpeedCmd(byte speedL, byte speedR) {
        byte[] bs = new byte[8];
        bs[0] = (byte) 0x03;
        bs[1] = speedL;
        bs[2] = speedR;
        byte[] result = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, (byte) 0x3b, bs);
        return result;
    }

    /**
     * @param motoType
     * @param angle
     * @param angle2
     * @return
     * @Description M1脖子电机
     * @author lz
     * @time 2017-4-21 下午4:34:58
     */
    public static byte[] getM1NeckMoveCmd(byte motoType, byte angle, byte angle2) {
        byte[] bs = new byte[3];
        bs[0] = motoType;
        bs[1] = angle;
        bs[2] = angle2;
        byte[] result = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, (byte) 0x39, bs);
        return result;
    }

    /**
     * @param type  1脖子灯光模式，2轮子灯光模式，3底部灯光模式
     * @param red
     * @param green
     * @param blue
     * @return
     * @Description M1灯光
     * @author lz
     * @time 2017-4-21 下午4:42:27
     */
    public static byte[] getM1NeckColorCmd(byte type, byte red, byte green, byte blue) {
        byte[] bs = new byte[3];
        bs[0] = red;
        bs[1] = green;
        bs[2] = blue;
        byte[] result = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, type, bs);
        return result;
    }

    /**
     * @param type 1脖子灯光模式，2轮子灯光模式，3底部灯光模式
     * @return
     * @Description M1灯光模式，默认正玄波
     * @author lz
     * @time 2017-4-21 下午4:51:07
     */
    public static byte[] getWaveform(int type) {
        byte[] bs = new byte[2];
        bs[0] = (byte) type;
        bs[1] = (byte) 1;
        byte[] result = ProtocolUtils.buildProtocol((byte) 0x0b, (byte) 0xA6, (byte) 0x36, bs);
        return result;
    }

    /**
     * 根据想要的角度，获取下传的角度
     *
     * @param wantedAngle
     * @return
     */
    public static int getDownAngle(int wantedAngle) {
        int result;

        if (wantedAngle >= 0) {
            result = 130 - wantedAngle;
        } else {
            if (wantedAngle == -130) {
                result = -1;
            } else {
                result = -130 - wantedAngle;
            }
        }

        return result;
    }

    /**
     * 获取下发命令的回应数据
     *
     * @return
     */
    public static byte[] getFeedbackData() {
        byte[] data = new byte[13];
        try {
            SP.request(data);
        } catch (Exception e) {
            LogMgr.e("getFeedbackData() 读取数据异常");
            e.printStackTrace();
        }
        return data;
    }

    /**
     * 解析下发命令的回应数据
     *
     * @param data 回应数据
     * @return
     */
    public static boolean isFeedbackCorrect(byte[] data) {
        if (null == data || data.length < 13) {
            LogMgr.e("isFeedbackCorrect() 数据异常");
            return false;
        }
        boolean result = false;

        if (data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 &&
                data[5] == GlobalConfig.M_FEEDBACK_IN_CMD_1 && data[6] == GlobalConfig.M_FEEDBACK_IN_CMD_2) {
            if (data[11] == GlobalConfig.M_FEEDBACK_OK) {
                LogMgr.d("下发命令  成功");
                result = true;
            } else if (data[11] == GlobalConfig.M_FEEDBACK_WRONG_HEAD) {
                LogMgr.e("下发命令 数据头出错(非AA 55)");
                result = false;
            } else if (data[11] == GlobalConfig.M_FEEDBACK_WRONG_CHECK) {
                LogMgr.e("下发命令 校验值出错");
                result = false;
            } else {
                LogMgr.e("下发命令 返回错误1");
                result = false;
            }
        } else {
            LogMgr.e("下发命令 返回错误2");
            result = false;
        }

        return result;
    }

    /**
     * 是否扩展多媒体命令
     *
     * @param data
     * @return
     */
    public static boolean isMultiMediaCmd(byte[] data) {
        if (data == null) {
            return false;
        }

        if (data.length > 6 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MULTI_MEDIA_IN_CMD_1 &&
                (data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_PLAY || data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_PAUSE ||
                        data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_STOP || data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_RESUME)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否多媒体音频播放命令
     *
     * @param data
     * @return
     */
    public static boolean isMultiMediaAudioPlayCmd(byte[] data) {
        if (data == null) {
            return false;
        }

        if (data.length > 18 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MULTI_MEDIA_IN_CMD_1 &&
                data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_PLAY && data[12] == MULTI_MEDIA_AUDIO) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否多媒体暂停命令
     *
     * @param data
     * @return
     */
    public static boolean isMultiMediaPauseCmd(byte[] data) {
        if (data == null) {
            return false;
        }

        if (data.length > 6 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MULTI_MEDIA_IN_CMD_1 &&
                data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_PAUSE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否多媒体停止命令
     *
     * @param data
     * @return
     */
    public static boolean isMultiMediaStopCmd(byte[] data) {
        if (data == null) {
            return false;
        }

        if (data.length > 6 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MULTI_MEDIA_IN_CMD_1 &&
                data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_STOP) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否多媒体继续播放命令
     *
     * @param data
     * @return
     */
    public static boolean isMultiMediaResumeCmd(byte[] data) {
        if (data == null) {
            return false;
        }

        if (data.length > 6 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MULTI_MEDIA_IN_CMD_1 &&
                data[6] == GlobalConfig.MULTI_MEDIA_IN_CMD_2_RESUME) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否C201模型相关命令
     * @param data
     * @return
     */
    public static boolean isModelCmd(byte[] data){
        if (data == null) {
            return false;
        }
        if(data.length >= 12 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MODEL_C_201_IN_CMD_1){
            return true;
        } else {
            return false;
        }
    }
    /**
     * 是否模型类型通知协议
     * @param data
     * @return
     */
    public static boolean isModelTypeNotifyCmd(byte[] data){
        if (data == null) {
            return false;
        }
        if(data.length >= 14 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MODEL_C_201_IN_CMD_1
                && data[6] == GlobalConfig.MODEL_C_201_IN_CMD_2_MODEL_TYPE){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 是否模型移动命令协议
     * @param data
     * @return
     */
    public static boolean isModelMoveCmd(byte[] data){
        if (data == null) {
            return false;
        }
        if(data.length >= 15 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MODEL_C_201_IN_CMD_1
                && data[6] == GlobalConfig.MODEL_C_201_IN_CMD_2_MOVE){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 是否模型功能开关命令协议
     * @param data
     * @return
     */
    public static boolean isModelFunctionCmd(byte[] data){
        if (data == null) {
            return false;
        }
        if(data.length >= 14 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MODEL_C_201_IN_CMD_1
                && data[6] == GlobalConfig.MODEL_C_201_IN_CMD_2_FUNCTION){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 是否模型动作命令协议
     * @param data
     * @return
     */
    public static boolean isModelActionCmd(byte[] data){
        if (data == null) {
            return false;
        }
        if(data.length >= 14 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1 && data[5] == GlobalConfig.MODEL_C_201_IN_CMD_1
                && data[6] == GlobalConfig.MODEL_C_201_IN_CMD_2_ACTION){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取22个舵机的角度
     *
     * @return 返回长度为22的int数组。x号舵机的角度存放在第x-1个位置
     */
    public static int[]  get22EngineAngle() {
        int[] result = new int[22];

        byte[] tempCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_ANGLE_FIRMWARE_OUT_CMD_2, null);
        byte[] buffer=null;
        for (int i = 0; i < 1; i++) {
            try {
//				StringBuilder sb = new StringBuilder();
                if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3 || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H5){
                    buffer=SP.request(tempCmd);

                }else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S){
                    buffer=SP.request(tempCmd);
                }else{
                    LogMgr.e("当前类型不是H或S，获取舵机角度失败");
                }
                LogMgr.v("所有舵机角度的命令 获取到的数据1 = " + Utils.bytesToString(buffer));
                if(buffer==null){
                    //这里是应该采取一些措施做处理的
                    return null;
                }
                if (buffer[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1 && buffer[6] == GlobalConfig.ENGINE_ANGLE_FIRMWARE_IN_CMD_2) {
                    byte[] dataOf22Engine = new byte[44];
                    System.arraycopy(buffer, 11, dataOf22Engine, 0, dataOf22Engine.length);
                    for (int d = 0; d < dataOf22Engine.length; d = d + 2) {
                        int k = d / 2 + 1;
                        int angle = (int) (((dataOf22Engine[0 + d] & 0xFF) << 8) | (dataOf22Engine[1 + d] & 0xFF));
                        LogMgr.d("舵机号 = " + k + " 角度 = " + angle);
                        result[k - 1] = angle;
//						sb.append("舵机号"+k+":"+angle+"  ");
//						if(k%3==0){
//							sb.append("\n");
//						}
                    }
//					textView_engineAngleData.setText(sb.toString());
                } else {
                    LogMgr.e("获取数据不正确");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 获取H某一舵机号的角度
     *
     * @param engineId 舵机ID
     * @return
     */
    public static int getEngineAngle(int engineId) {
         if (engineId < 0 || engineId > 23) {
            return -1;
        }
        int[] engineAngle = get22EngineAngle();

        for(int i=0;i<engineAngle.length;i++){
            LogMgr.e("舵机"+(i+1)+"反馈角度："+engineAngle[i]);
        }

        return engineAngle[engineId];
    }

    /**
     * 控制H机器人舵机状态变化
     *
     * @param engineCount  ID个数N
     * @param engineIDs    舵机ID号
     * @param engineStates 舵机配置：0x01代表固定； 0x02代表释放； 0x03代表归零
     */
    public static void engineStateChange(int engineCount, int[] engineIDs, int[] engineStates) {
        if (engineCount <= 0 || engineCount >= 23 || engineIDs == null || engineStates == null
                || engineCount != engineIDs.length || engineCount != engineStates.length) {
            LogMgr.e("参数异常 engineCount = " + engineCount + ((engineIDs == null) ? " engineIDs is null" : " engineIDs.length = " + engineIDs.length) +
                    ((engineStates == null) ? " engineStates is null" : " engineStates.length = " + engineStates.length));
            return;
        }
        byte[] data = new byte[1 + 2 * engineCount];

        data[0] = (byte) engineCount;
        for (int i = 0; i < engineCount; i++) {
            data[i * 2 + 1] = (byte) engineIDs[i];
            data[i * 2 + 2] = (byte) engineStates[i];
        }
        byte[] tempCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_STATE_CHANGE_OUT_CMD_2, data);

        if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
            SP.write(tempCmd);
        }else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
            LogMgr.e("释放舵机");
            SP.write(tempCmd);
        }

    }

    /**
     * 控制H机器人所有舵机状态变化
     *
     * @param engineStateChange ENGINE_STATE_FIXED = 1;
     *                          ENGINE_STATE_RELEASE = 2;
     *                          ENGINE_STATE_RETURN_ZERO = 3;
     */
    public static void engineStateChangeAll(int engineStateChange) {
        if (engineStateChange != ENGINE_STATE_FIXED && engineStateChange != ENGINE_STATE_RELEASE && engineStateChange != ENGINE_STATE_RETURN_ZERO) {
            LogMgr.e("engineStateChangeAll() 参数错误");
            return;
        }
        int engineCount = 22;
        int[] engineIDs = new int[engineCount];
        int[] engineStates = new int[engineCount];

        for (int i = 0; i < engineCount; i++) {
            engineIDs[i] = i + 1;
            engineStates[i] = engineStateChange;
        }
        engineStateChange(engineCount, engineIDs, engineStates);
    }

    /**
     * 控制H机器人特定舵机固定
     *
     * @param engineID
     * @param engineStateChange ENGINE_STATE_FIXED = 1;
     *                          ENGINE_STATE_RELEASE = 2;
     *                          ENGINE_STATE_RETURN_ZERO = 3;
     */
    public static void engineStateChangeOne(int engineID, int engineStateChange) {
        int engineCount = 1;
        int[] engineIDs = new int[engineCount];
        int[] engineStates = new int[engineCount];

        engineIDs[0] = engineID;
        engineStates[0] = engineStateChange;

        engineStateChange(engineCount, engineIDs, engineStates);
    }

    /**
     * 获取所有舵机的零位偏移量
     *
     * @return
     */
    public static int[] getEngineZeroOffset() {
        int[] result = null;
        byte[] engineZeroOffsetData = new byte[12 + 1 + 22 * 3];

        byte[] tempCmd = buildProtocol((byte) ControlInfo.getMain_robot_type(), GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_GET_ZERO_OFFSET_OUT_CMD_2, null);
        if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
/*            SP.readMotor(engineZeroOffsetData);
            LogMgr.d("getEngineZeroOffset()1");
            SP.writeMotor(tempCmd, 500);
            LogMgr.d("getEngineZeroOffset()2");
            SP.readMotor(engineZeroOffsetData);*/
            engineZeroOffsetData = SP.request(tempCmd,1000);
        }else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S){
/*            SP.read(engineZeroOffsetData);
            LogMgr.d("getEngineZeroOffset()1");
            SP.write(tempCmd, 500);
            LogMgr.d("getEngineZeroOffset()2");
            SP.read(engineZeroOffsetData);*/
            engineZeroOffsetData = SP.request(tempCmd);
        }else{
            LogMgr.e("当前类型不是H或S，获取舵机零位失败");
        }

        LogMgr.d("getEngineZeroOffset() 获取零位偏移量返回数据 " + Utils.bytesToString(engineZeroOffsetData));
        if (engineZeroOffsetData[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1 && engineZeroOffsetData[6] == GlobalConfig.ENGINE_GET_ZERO_OFFSET_IN_CMD_2) {
            LogMgr.d("getEngineZeroOffset() 获取零位偏移量返回数据正常");
            int engineCount = (int) (engineZeroOffsetData[11] & 0xFF);
            LogMgr.d("getEngineZeroOffset() engineCount = " + engineCount);
            result = new int[engineCount * 2];
            for (int i = 0; i < engineCount; i++) {
                result[2 * i] = (int) (engineZeroOffsetData[11 + 3 * i + 1] & 0xFF);
//				result[2*i + 1] = (int)(((engineZeroOffsetData[11+3*i+2]&0xFF)<<8) | (engineZeroOffsetData[11+3*i+2]&0xFF));
                result[2 * i + 1] = (int) (engineZeroOffsetData[11 + 3 * i + 3]);
                LogMgr.d("第" + result[2 * i] + "个舵机的零位偏移量 = " + result[2 * i + 1]);
            }
        } else {
            LogMgr.e("getEngineZeroOffset() 获取零位偏移量返回数据异常");
        }

        return result;
    }

    /**
     * 获取制定舵机的零位偏移量
     *
     * @param engineID
     * @return 如果返回10000代表没有取到正确数据
     */
    public static int getEngineZeroOffset(int engineID) {
        int result = 10000;

        int[] allEngineZeroOffset = getEngineZeroOffset();
        for (int i = 0; i < allEngineZeroOffset.length - 1; i = i + 2) {
            int tempEngineID = allEngineZeroOffset[i];
            if (tempEngineID == engineID) {
                result = allEngineZeroOffset[i + 1];
                LogMgr.i("第" + tempEngineID + "个舵机的零位偏移量 = " + result);
            }
        }

        return result;
    }

    /**
     * 获取制定舵机的零位偏移量
     *
     * @param engineID
     * @return 如果返回10000代表没有取到正确数据
     */
    public static int getEngineZeroOffsetS(int engineID) {
        int result = 10000;

        byte[] cmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_GET_ZERO_OFFSET_OUT_CMD_2, new byte[]{(byte)0x01,(byte)engineID});
        byte[] result1 = new byte[12 + 4];
/*        SP.read(result1);
        SP.write(cmd,100);
        SP.read(result1);*/
        result1 = SP.request(cmd);

        LogMgr.i("getEngineZeroOffsetS result1 = "+Utils.bytesToString(result1));
        int engineId = (int) (result1[12] & 0xFF);
        if(engineId == engineID){
            result = (int)result1[13];
        }
//
//        int[] allEngineZeroOffset = getEngineZeroOffset();
//        for (int i = 0; i < allEngineZeroOffset.length - 1; i = i + 2) {
//            int tempEngineID = allEngineZeroOffset[i];
//            if (tempEngineID == engineID) {
//                result = allEngineZeroOffset[i + 1];
//                LogMgr.i("第" + tempEngineID + "个舵机的零位偏移量 = " + result);
//            }
//        }

        return result;
    }

    /**
     * 设定零位偏移 多个舵机同时设定
     *
     * @param engineCount  ID个数N
     * @param engineIDs    舵机ID号
     * @param engineZeroOffsets 舵机零位偏移设定值
     */
    public static void setEngindZeroOffset(int engineCount, int[] engineIDs, int[] engineZeroOffsets) {
        if (engineCount <= 0 || engineCount >= 23 || engineIDs == null || engineZeroOffsets == null
                || engineCount != engineIDs.length || engineCount != engineZeroOffsets.length) {
            LogMgr.e("参数异常 engineCount = " + engineCount + ((engineIDs == null) ? " engineIDs is null" : " engineIDs.length = " + engineIDs.length) +
                    ((engineZeroOffsets == null) ? " engineZeroOffsets is null" : " engineZeroOffsets.length = " + engineZeroOffsets.length));
            return;
        }
        byte[] data = new byte[1 + 2 * engineCount];

        data[0] = (byte) engineCount;
        for (int i = 0; i < engineCount; i++) {
            data[i * 2 + 1] = (byte) engineIDs[i];
            data[i * 2 + 2] = (byte) engineZeroOffsets[i];

            LogMgr.i("第" + data[i * 2 + 1] + "个舵机的设置零位为 = " + data[i * 2 + 2]);
        }
        byte[] tempCmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_SET_ZERO_OFFSET_OUT_CMD_2, data);
        LogMgr.i("setEngindZeroOffset tempCmd = "+Utils.bytesToString(tempCmd));

        if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
/*            SP.writeMotor(tempCmd);
            SP.readMotor(new byte[20]);*/
            SP.request(tempCmd);
        }else if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S){
 /*           SP.write(tempCmd, 10);
            SP.read(new byte[20]);*/
            SP.request(tempCmd);
        }else{
            LogMgr.e("当前类型不是H或S，设定零位偏移失败");
        }
    }

    /**
     * 设定零位偏移 设定单个舵机
     *
     * @param engineID         舵机ID号
     * @param engineZeroOffset 舵机零位偏移设定值
     */
    public static void setEngindZeroOffset(int engineID, int engineZeroOffset) {
        int engineCount = 1;
        int[] engineIDs = new int[engineCount];
        int[] engineZeroOffsets = new int[engineCount];

        engineIDs[0] = engineID;
        engineZeroOffsets[0] = engineZeroOffset;

        setEngindZeroOffset(engineCount, engineIDs, engineZeroOffsets);
    }

    /**
     * 控制单个舵机运动到某一角度
     * @param angle 角度值
     * @param engineID 舵机ID
     */
    public static void goToAngleS(int angle, int engineID){
        byte[] tempData = new byte[3];
        tempData[0] = (byte)engineID;
        tempData[1] = (byte)((angle >> 8) & 0xFF);
        tempData[2] = (byte)(angle & 0xFF);
        byte[] tempCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1,GlobalConfig.ENGINE_SET_SINGLE_ANGLE_OUT_CMD_2, tempData);
        LogMgr.i("goToAngleS tempCmd = "+ Utils.bytesToString(tempCmd));
        SP.write(tempCmd);
    }

    /**
     * 设置S舵机ID
     * @param currentID 当前ID
     * @param targetID  目标ID
     */
    public static void setEngineID(int currentID,int targetID) {
        byte[] cmd = new byte[]{(byte)0xFE,(byte)0x68,(byte)0x5A,(byte)0x00,(byte)0x00,(byte)0x0A,(byte)0xFF,(byte)0xFF,
                (byte)0x01,(byte)0x04,(byte)0x03,(byte)0x03,(byte)0x02,(byte)0xF2,(byte)0xAA,(byte)0x16};
        cmd[8] = (byte)currentID;
        cmd[12] = (byte)targetID;
        cmd[13] = (byte)~(cmd[8] + cmd[9] + cmd[10] + cmd[11] + cmd[12]);
        LogMgr.i("setEngineID cmd[8] = "+cmd[8]+" cmd[12] = "+cmd[12]+" cmd[13] = "+cmd[13] + " cmd = "+Utils.bytesToString(cmd));
        SP.write(cmd);
    }

    /**
     * 舵机标零
     * @param engineNo
     */
    public static boolean adjustZeroS(int engineNo) {
        LogMgr.d("adjustAngle() engineNo = " + engineNo);
        boolean result = true;
//        if (mSelectedEngineNo <= 0 || mSelectedEngineNo >= 13) {
//            LogMgr.e("不支持舵机号 = " + mSelectedEngineNo);
//        }
//        LogMgr.d("adjustAngle()1");

        int realAngle = ProtocolUtils.getEngineAngle(engineNo);
        LogMgr.d("adjustAngle()2 realAngle = " + realAngle);
        if(realAngle <= 0){
            LogMgr.e("获取当前角度失败 校准失败");
            result = false;
            return result;
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int zeroOffset = ProtocolUtils.getEngineZeroOffsetS(engineNo);
        LogMgr.d("adjustAngle()3 zeroOffset = " + zeroOffset);

        if (zeroOffset == 10000) {
            LogMgr.e("获取零度偏移错误");
            result = false;
            return result;
        }

//        int tempAngle = (int) (1.0 * limitAngels[mSelectedEngineNo - 1] * 1024 / 300);
//        LogMgr.d("tempAngle = " + tempAngle);

        int wantedZeroOffset = realAngle + zeroOffset - 512;
        LogMgr.d("adjustAngle()4 wantedZeroOffset = " + wantedZeroOffset);

        ProtocolUtils.setEngindZeroOffset(engineNo, wantedZeroOffset);
        LogMgr.d("adjustAngle()5");
        return result;
    }

    /**
     * 检测当前是否有舵机
     * @return
     */
    public static int hasEngine() {
        int result = EngineTestActivity.STATE_UNKNOWN;

        byte[] cmd = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_CHECK_ENGINE,null);
        byte[] returnData = SP.request(cmd);
        if (returnData == null) {
            LogMgr.e("SP.request Error! null");
            return result;
        }
        LogMgr.d("returnData = "+Utils.bytesToString(returnData));
        if(returnData[5] == GlobalConfig.ENGINE_TEST_S_IN_CMD_1 && returnData[6] == GlobalConfig.ENGINE_TEST_S_IN_CMD_2_CHECK_ENGINE){
            if(returnData[12] == (byte)0x00){
                result = EngineTestActivity.STATE_NO_ENGINE;
            }else{
                result = EngineTestActivity.STATE_HAS_ENGINE;
            }
        }
        return result;
    }

    /**
     * 关闭H3头部灯光 因为固件的原因，需要发两次才能关闭
     */
    public static void closeH3Led(){
        LogMgr.i("停止H3的头部灯光1");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] closeLightCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1,
                GlobalConfig.ENGINE_SET_H34_LIGHT_CMD_2,new byte[3]);
        SP.request(closeLightCmd,20);
        SP.request(closeLightCmd,20);
    }

    /**
     * 设置舵机速度
     * @param iCount 舵机个数
     * @param pID 舵机ID号
     * @param pPos 舵机角度（无用参数）
     * @param speed 舵机速度
     */
    public static void setEngineSpeed(byte iCount, byte[] pID, byte[] pPos, int speed) {

        LogMgr.v("setEngineSpeed speed = " + speed);
        // FF FF FE 07 83 1E 02 id FF 03 D4
        // byte[] Buffer = new byte[16];
        int iLength = 0;
        byte bChecksum = 0;
        int i = 0;
        // int iTest = 0;

        if (iCount < 1 || iCount > 30)
            return;
        iLength = 8 + iCount * 3;
        byte[] gSendBuff = new byte[iLength];

        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) 0xFE;
        gSendBuff[3] = (byte) (4 + iCount * 3);
        gSendBuff[4] = (byte) 0x83;
        gSendBuff[5] = (byte) 0x20;
        gSendBuff[6] = 0x02;

        for (i = 0; i < iCount; i++) // 22次
        {
            if (pID[i] < 254) {
                gSendBuff[7 + i * 3] = pID[i];

                gSendBuff[7 + i * 3 + 1] = (byte) (speed & 0xFF); // (pPos[i*2]);
                gSendBuff[7 + i * 3 + 2] = (byte) 0x00; // (pPos[i*2+1]);
            } else {
                gSendBuff[7 + i * 3] = 0x00;
                gSendBuff[7 + i * 3 + 2] = 0x00;
                gSendBuff[7 + i * 3 + 1] = 0x00;
            }
        }

        for (i = 2; i < iLength - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[iLength - 1] = bChecksum;

        DoWriteFrame(gSendBuff, iLength);
    }

    private static void DoWriteFrame(byte[] pBuf, int dwLen) {
        // byte[] readbuffer1 = new byte[30];
        int iLength = dwLen + 3;//构建数据位
        //高位在前，低位在后
        byte[] sendBuff = new byte[iLength];
        sendBuff[0] = (byte) 0x02;
        sendBuff[1] = (byte) ((dwLen >> 8) & 0xFF);
        sendBuff[2] = (byte) (dwLen & 0xFF);
        System.arraycopy(pBuf, 0, sendBuff, 3, dwLen);


        LogMgr.v("往底层串口1传数据");
//        LogMgr.v("sendBuff = " + Utils.bytesToString(sendBuff, sendBuff.length));
        // handleHandAndFootLight();
        byte[] gSendBuffer = buildProtocol(ControlInitiator.ROBOT_TYPE_H,(byte) 0x11,(byte) 0x15,sendBuff);
        /** 添加类型判断 lz 2017-3-23 */
//        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {

            SP.write(gSendBuffer);
//            handleHandAndFootLight();
//        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
//                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
////            SP.request(sendBuff);
//            SP.write(sendBuff);
//        }
//        int iLength = dwLen + 8;
//        byte[] sendBuff = new byte[iLength];
//
//        sendBuff[0] = (byte) 0xFE;
//        sendBuff[1] = (byte) 0x68;
//        sendBuff[2] = (byte) 'Z';
//        sendBuff[3] = 0x00;
//        sendBuff[4] = (byte) ((dwLen >> 8) & 0xFF);
//        sendBuff[5] = (byte) (dwLen & 0xFF);
//
//        sendBuff[iLength - 2] = (byte) 0xAA;
//        sendBuff[iLength - 1] = (byte) 0x16;
//        System.arraycopy(pBuf, 0, sendBuff, 6, dwLen);
//        LogMgr.v("往底层串口1传数据");
////        LogMgr.v("sendBuff = " + Utils.bytesToString(sendBuff, sendBuff.length));
//        // handleHandAndFootLight();
//
//        /** 添加类型判断 lz 2017-3-23 */
//        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
//
//            SP.write(sendBuff);
////            handleHandAndFootLight();
//        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S || ControlInfo
//                .getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
////            SP.request(sendBuff);
//            SP.write(sendBuff);
//        }

        // try {

        // mOutputStream.write(sendBuff);

		/*
         * String str="", str1="发送：  "; for(int n=0; n<sendBuff.length; n++){
		 * str = String.format("%02x  ", sendBuff[n]); str1 += str; }
		 * //Display(1, str1); Log.v("AIRIGHT", str1);
		 */
        /*
         * Display(1, "DoWriteFrame发送成功!");
		 *
		 * try { Thread.sleep(2); } catch (InterruptedException e) {
		 * e.printStackTrace(); } for(int n1=0; n1<readbuffer1.length; n1++)
		 * readbuffer1[n1] = 0x00;
		 *
		 * String str2="", str3="接收：  "; mInputStream.write(readbuffer1); for(int
		 * n=0; n<readbuffer1.length; n++){ str2 = String.format("%02x  ",
		 * readbuffer1[n]); str3 += str2; } Log.v("AIRIGHT", str3);
		 */

        // } catch (IOException e) {
        // e.printStackTrace();
        // LogMgr.e("TAG", "Servo_SetPosAll发送失败!");
        // }
    }
    //判断机器的姿态是不是下蹲：根据1，2，5，6，7，8号舵机角度判断
    public static boolean isStateXiadun(){
        LogMgr.d("isStateXiaDun = ");
        final boolean[] isStatuXiaDun = {false};
        final byte[] pID = new byte[]{1,2,5,6,7,8};
        final int[] CAngle = new int[6];//要6个舵机的角度
        final int[] sTateXiaDun = new int[]{325,690,886,138,342,682};//这里对应的是6个角度，1,2,5,6,7,8
        //获取的舵机角度--下蹲状态：1 要小于325，2大于699 ，5大于886，6要小于138，7要小于342，8要大于682
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                for (int i = 0; i < 6; i++) {
                    int mCurrentID = pID[i];//舵机ID
                    int SrawLength = 0;//长度
                    int[] Sraw = new int[10];//存放舵机角度
                    for (int j = 0; j <5 ; j++) {
                        int position = 0;//舵机位置
                        position = getSingleServoPos(mCurrentID,0);
                        if(position != 0 ){
                            Sraw[SrawLength++] = position;
                        }
                    }
                    if(SrawLength > 0){
                        CAngle[i] = ValueDebounce(Sraw,SrawLength);
                    }
                }
                LogMgr.e(" isStateXiaDun CAngle  = " + Utils.intsToString(CAngle));
                if((CAngle[0] <= sTateXiaDun[0]) && (CAngle[1] >= sTateXiaDun[1]) &&
                        (CAngle[2] >= sTateXiaDun[2]) && (CAngle[3] <= sTateXiaDun[3]) &&
                        (CAngle[4] <= sTateXiaDun[4]) && (CAngle[5] >= sTateXiaDun[5])){
                    isStatuXiaDun[0] = true;
                    LogMgr.d("isStateXiaDun = " + isStatuXiaDun[0]);
                }
//            }
//        }).start();
        return isStatuXiaDun[0];
    }

    /**
     * 局部标定
     *
     * @param iCount 舵机个数
     * @param pID 舵机ID号
     * @param
     */
    public static void ServoCalibration(final int iCount, final byte[] pID){
        final int[] CAngle = new int[22];//22个舵机角度位置
        final int[] ReadZeroPos = new int[22];//读取到的22个舵机零位偏移量
        final int[] CalculationZeroPos = new int[22];//计算得到的22个舵机零位偏移量
        final int[] ServoZeroPosition = {512, 512, 512, 512, 512, 512, 542, 481, 512, 512, 512, 512,
                719, 305, 818, 205, 512, 512, 512, 512, 512, 512};//22个舵机初始的零位
        new Thread(new Runnable() {
            @Override
            public void run() {
                //1.获取舵机角度
                for (int i = 0; i < iCount; i++) {
                    int mCurrentID = pID[i];//舵机ID
                    int SrawLength = 0;//长度
                    int[] Sraw = new int[10];//存放舵机角度
                    for (int j = 0; j <5 ; j++) {
                        int position = 0;//舵机位置
                        position = getSingleServoPos(mCurrentID,0);
                        if(position != 0 ){
                            Sraw[SrawLength++] = position;
                        }
                    }
                    if(SrawLength > 0){
                        CAngle[mCurrentID - 1] = ValueDebounce(Sraw,SrawLength);
                    }
                }
                LogMgr.e("CAngle  = " + Utils.intsToString(CAngle));
                //2.获取舵机偏移量
                for (int i = 0; i < iCount; i++) {
                    int mCurrentID = pID[i];//舵机ID
                    ReadZeroPos[mCurrentID-1] = getSingleServoPos(mCurrentID,1);
                }
                LogMgr.e("ReadZeroPos  = " + Utils.intsToString(ReadZeroPos));
                //3.计算舵机偏移量
                for (int i = 0; i < iCount; i++) {
                    int mCurrentID = pID[i];//舵机ID
                    CalculationZeroPos[mCurrentID-1] = CAngle[mCurrentID -1] + ReadZeroPos[mCurrentID - 1];
                    CalculationZeroPos[mCurrentID-1] -= ServoZeroPosition[mCurrentID - 1];
                }
                LogMgr.e("CalculationZeroPos  = " + Utils.intsToString(CalculationZeroPos));
                //4.设置舵机偏移量
                for (int i = 0; i < iCount; i++) {
                    int mCurrentID = pID[i];//舵机ID
                    try {
                        Thread.sleep(500);
                        if(CalculationZeroPos[mCurrentID-1] >= - 256 && CalculationZeroPos[mCurrentID - 1] <= 256){
                            if(CalculationZeroPos[mCurrentID -1] >=0){
                                SetServo(mCurrentID,(byte)0x0A,CalculationZeroPos[mCurrentID - 1],1);
                            }else {
                                SetServo(mCurrentID,(byte)0x0A,CalculationZeroPos[mCurrentID - 1]+256,1);
                            }
                        }else{
                            LogMgr.e("CalculationZeroPos  = " + Utils.intsToString(CalculationZeroPos));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //舵机归零;H5初始零位
    public static int[] motor_zero = new int[]{  512, 512, 512, 512, 512, 512, 542, 481, 512, 512, 512, 512,
            719, 305, 818, 205, 512, 512, 512, 512, 512, 512};//
    public static void goServePosZeros(){
        int angle1=0;
        byte[] motor_id = new byte[22];
        byte[] pPos = new byte[22*2];
        for (int i = 0; i < 22; i++) {
            angle1 = motor_zero[i];
            motor_id[i] = (byte) (i+1);
            pPos[i*2] = (byte) (angle1 & 0xff);
            pPos[i*2 + 1] = (byte) ((angle1 & 0xff00) >> 8);
        }
        sendEngineAngles((byte) 22, motor_id, pPos);
    }
    //单舵机 ， 23 24 左右脚部灯光，通用
    public static void setAMotor(int mCurrentID,int angleV){
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
//        gSendBuff[6] = (byte) (angleV & 0xff);
//        gSendBuff[7] = (byte) (angleV & 0xff00>>8);
        for (int i = 2; i < gSendBuff.length - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[8] = bChecksum;
        LogMgr.e("gSendBuff = " + Utils.bytesToString(gSendBuff));
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    //手部灯  /脚部灯光，亮
    public static void handAndFeetLight(boolean onOff){
        try{
            if(onOff){//开
                handLight(1,19);
                Thread.sleep(20);
                handLight(1,20);
                Thread.sleep(20);
                setAMotor(23,512);
                Thread.sleep(20);
                setAMotor(24,512);
            }else{//关
                handLight(0,19);
                Thread.sleep(20);
                handLight(0,20);
                Thread.sleep(20);
                setAMotor(23,0);
                Thread.sleep(20);
                setAMotor(24,0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //手部 左19 右20
    public static void handLight(int onOrOff, int mCurrentID){
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
        LogMgr.e("gSendBuff = " + Utils.bytesToString(gSendBuff));
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    //脚步 左24 右23
    /**
     * type:0:释放,1:固定   reg:寄存器：0x18 释放上电
     *
     */
    public static void relAndFix(int type,byte reg){
        byte bChecksum = 0;
        byte[] gSendBuff = new byte[8 + 2*22];
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) 0xFE;
        gSendBuff[3] = (byte) 48;
        gSendBuff[4] = (byte) 0x83;
        gSendBuff[5] = (byte) reg;
        gSendBuff[6] = 0x01;
        for (int i = 0; i < 22; i++) {
            gSendBuff[7 + i*2 ] = (byte) (i + 1);
            gSendBuff[7 + i*2 +1] = (byte) type;
        }
        for (int i = 2; i < gSendBuff.length - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[gSendBuff.length - 1] = bChecksum;
        DoWriteFrame(gSendBuff,gSendBuff.length);
    }
    /**
     * mCurrentID 舵机ID
     * reg 寄存器 0x0A
     * data 发送的数据 2 byte
     * dataLen data的长度
     * */
    private static void SetServo(int mCurrentID, byte reg, int data, int dataLen) {
        byte bChecksum = 0;
        byte[] gSendBuff = new byte[8];
        byte[] sendBuff = new byte[11];
        sendBuff[0] = (byte) 0x02;
        sendBuff[1] = (byte) 0;
        sendBuff[2] = (byte) 8;
        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) mCurrentID;
        gSendBuff[3] = (byte) 4 ;//长度
        gSendBuff[4] = (byte) 3;//写
        gSendBuff[5] = (byte) reg;
        gSendBuff[6] = (byte) data;
        for (int i = 2; i <  7; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[7] = bChecksum;
        System.arraycopy(gSendBuff,0,sendBuff,3,8);
        SP.write(buildProtocol(ControlInitiator.ROBOT_TYPE_H,(byte) 0x11,(byte) 0x15,sendBuff));
    }
    //冒泡排序法
    private static int ValueDebounce(int[] sraw, int srawLength) {
        int[] normol = new int[srawLength];
        int temp;
        int result = 0;
        for (int i = 0; i < srawLength; i++) {
            normol[i] = sraw[i];
        }
        for (int i = 0; i < srawLength - 1; i++) {
            for (int j = i+1; j < srawLength - 1- i; j++) {
                if(normol[i] > normol[j]){
                    temp = normol[i];
                    normol[i] = normol[j];
                    normol[j] = temp;
                }
            }
        }
//        for (int i = 0; i < srawLength - 1; i++) {
//            for (int j = 0; j < srawLength - 1 - i; j++) {
//                if(normol[j] > normol[j+1]){
//                    temp = normol[j];
//                    normol[j] = normol[j+1];
//                    normol[j+1] = temp;
//                }
//            }
//        }
        temp = normol[srawLength/2 -1] + normol[srawLength/2];
        result = temp/2;
        LogMgr.d("result = " + result);
        return result;
    }
    //type 0:获取舵机角度  1：获取零位偏移量
    public static int getSingleServoPos(int mCurrentID,int type) {
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
            byte[] resquest = SP.request(buildProtocol(ControlInitiator.ROBOT_TYPE_H,(byte) 0x11,(byte) 0x15,sendBuff),15);
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

    /**
     * 往串口发送一次动作数据
     *
     * @param iCount 舵机个数
     * @param pID 舵机ID号
     * @param pPos 舵机角度
     */
    public static void sendEngineAngles(byte iCount, byte[] pID, byte[] pPos) {
        // FF FF FE 07 83 1E 02 id FF 03 D4
        // byte[] Buffer = new byte[16];
        int iLength = 0;
        byte bChecksum = 0;
        int i = 0;
        // int iTest = 0;

        if (iCount < 1 || iCount > 30)
            return;
        iLength = 8 + iCount * 3;
        byte[] gSendBuff = new byte[iLength];

        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) 0xFE;
        gSendBuff[3] = (byte) (4 + iCount * 3);
        gSendBuff[4] = (byte) 0x83;
        gSendBuff[5] = (byte) 0x1E;
        gSendBuff[6] = 0x02;

        for (i = 0; i < iCount; i++) // 22次
        {
            if (pID[i] < 254) {
                gSendBuff[7 + i * 3] = pID[i];
                // gSendBuff[7+i*3+2] = (byte)((pPos[i]>>8) & 0x00FF);
                // gSendBuff[7+i*3+1] = (byte)((pPos[i] & 0x00FF));

                gSendBuff[7 + i * 3 + 1] = (byte) (pPos[i * 2]);
                gSendBuff[7 + i * 3 + 2] = (byte) (pPos[i * 2 + 1]);
            } else {
                gSendBuff[7 + i * 3] = 0x00;
                gSendBuff[7 + i * 3 + 2] = 0x00;
                gSendBuff[7 + i * 3 + 1] = 0x00;
            }
        }

        for (i = 2; i < iLength - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[iLength - 1] = bChecksum;

        DoWriteFrame(gSendBuff, iLength);
    }

    // 释放
    public static void Servo_SetRelesePosAll(byte iCount, byte[] pID, byte[] pPos) {
        // FF FF FE 07 83 1E 02 id FF 03 D4
        byte[] Buffer = new byte[16];
        int iLength = 0;
        byte bChecksum = 0;
        int i = 0;
        int iTest = 0;

        if (iCount < 1 || iCount > 30) {
            return;
        }
        iLength = 8 + iCount * 2;
        byte[] gSendBuff = new byte[iLength];

        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) 0xFE;
        gSendBuff[3] = (byte) (4 + iCount * 2);
        gSendBuff[4] = (byte) 0x83;
        gSendBuff[5] = (byte) 0x18; // 24
        gSendBuff[6] = 0x01;

        for (i = 0; i < iCount; i++) // 22次
        {
            if (pID[i] < 254) {
                gSendBuff[7 + i * 2] = pID[i];
                // gSendBuff[7+i*3+2] = (byte)((pPos[i]>>8) & 0x00FF);
                // gSendBuff[7+i*3+1] = (byte)((pPos[i] & 0x00FF));

                gSendBuff[7 + i * 2 + 1] = (byte) (pPos[i * 2]);
                // gSendBuff[7+i*2+2] = (byte)(pPos[i*2+1]);
            } else {
                gSendBuff[7 + i * 2] = 0x00;
                // gSendBuff[7+i*2+2] = 0x00;
                gSendBuff[7 + i * 2 + 1] = 0x00;
            }
        }

        for (i = 2; i < iLength - 1; i++) {
            bChecksum += gSendBuff[i];
        }
        bChecksum = (byte) ~bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        gSendBuff[iLength - 1] = bChecksum;

        DoWriteFrame(gSendBuff, iLength);
    }
}
