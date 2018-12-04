package com.abilix.control;

import android.os.Environment;

import java.io.File;

public class GlobalConfig {

    /**
     * 仅供调试使用
     */
    public static final byte CONTROL_TYPE = ControlInitiator.ROBOT_TYPE_M1;// Brain 是针对哪个系列的 1：C系列 2：M系列 3:H系列  4：F系列

    public static final String PASSWORD = "64952827";
    public static final String WIFI_SSID = "Abilix-C-";

    public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Download";
    public static final String SKILLPLAYER_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" + File.separator + "Abilix_Skillplayer";
    public static final String PROGRAM_FOR_S_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" + File.separator + "media" + File.separator + "default" + File.separator + "movementOfProgramS";
    public static final String PROGRAM_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" + File.separator + "AbilixProjectProgram";
    public final static String KNOW_ROBOT_PATH = Environment.getExternalStorageDirectory() + File.separator + "Abilix" + File.separator +"AbilixKnowRobot";//平衡车bin文件对应的文件夹
    public final static String MOVE_BIN_PATH = Environment.getExternalStorageDirectory()+ File.separator + "Abilix"+ File.separator + "MoveBin";
    //436   587   484   689   336   687   614   409   539   484   512   512   512   512   791   232   477   546   512   512   512   512
    public static final byte[] H_MOVE_START_POSITION = new byte[]{
            (byte) 0x00, (byte) 0x00,//0
            (byte) 0xB4, (byte) 0x01,//436
            (byte) 0x4B, (byte) 0x02,//587
            (byte) 0xE4, (byte) 0x01,//484
            (byte) 0x1B, (byte) 0x02,//689
            (byte) 0x50, (byte) 0x01,//336
            (byte) 0xAF, (byte) 0x02,//687
            (byte) 0x66, (byte) 0x02,//614
            (byte) 0x99, (byte) 0x01,//409
            (byte) 0x1B, (byte) 0x02,//539
            (byte) 0xE4, (byte) 0x01,//484
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x17, (byte) 0x03,//791
            (byte) 0xE8, (byte) 0x00,//232
            (byte) 0xDD, (byte) 0x01,//477
            (byte) 0x22, (byte) 0x02,//546
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02,//512
            (byte) 0x00, (byte) 0x02 //512
    };

    /**
     * 动作过滤功能是否启用
     */
    public static final boolean isMoveFilterActive = true;
    /**
     * 是否使用动作灯光帧中的灯光
     */
    public static final boolean isLightEnableControl = true;
    /**
     * 是否使用动作灯光帧中的手部脚部灯光
     */
    public static final boolean isLightInHandFootEnableControl = true;
    /**
     * 是否启用防摔倒手指保护功能
     */
    public static final boolean isFingerProtectActive = true;
    /**
     * 是否使用位移定位方式执行M机器人的bin文件 很失败的尝试 毫无价值使用
     */
    public static final boolean isDisplacementMatterForMBin = false;
    /**
     * 是否使用补偿速度方式发送M的bin文件 速度补偿的方式会使
     * 原来顺畅的直线运动变成卡顿，效果反不如之前，待后续继续优化
     * 改成true时要将M的轮子速度命令的if条件去掉，保证第一条永远是轮子速度命令
     */
    public static final boolean isAdjustSpeedForMBin = false;
    /**
     * 是否启用M系列的stm32心跳功能
     */
    public static final boolean isMStm32HeartBeatActive = true;
    /**是否显示F固件升级进度*/
    public static final boolean isShowFFirmwareUpdateProgress = true;
    /**
     * stm32的心跳时间间隔秒数
     */
    public static final int M_STM32_HEART_BEART_TIME = 2;

    public static final String MOVE_FORWARD_BEGIN = "Walk_F_beg.bin";
    public static final String MOVE_FORWARD_MIDDLE = "Walk_F_mid.bin";
    public static final String MOVE_FORWARD_END = "Walk_F_end.bin";

    public static final String MOVE_BACK_BEGIN = "Walk_B_beg.bin";
    public static final String MOVE_BACK_MIDDLE = "Walk_B_mid.bin";
    public static final String MOVE_BACK_END = "Walk_B_end.bin";

    public static final String MOVE_RIGHT_BEGIN = "Right_B_beg.bin";
    public static final String MOVE_RIGH_MIDDLE = "Right_B_mid.bin";
    public static final String MOVE_RIGH_END = "Right_B_end.bin";

    public static final String MOVE_LEFT_BEGIN = "Left_F_beg.bin";
    public static final String MOVE_LEFT_MIDDLE = "Left_F_mid.bin";
    public static final String MOVE_LEFT_END = "Left_F_end.bin";

    public static final String MOVE_MARK_TIME = "H_tabu1step.bin";
    public static final String MOVE_RISE_FRONT = "H_qpqs.bin";
    public static final String MOVE_RISE_BACK = "H_hyqs.bin";
    public static final String MOVE_BOOT_RECOVER = "H_csh.bin";
    public static final String MOVE_BOOT_RECOVER_H = "Rig_H5.bin";
    public static final String MOVE_BOOT_RECOVER_XIADUN = "H5_xiadun.bin";

    public static final String MESSAGE_DELAY_TIME = "message_delay_time";

    /*上层APP类别*/
    public static final byte APP_TYPE_KNOW_ROBOT = (byte) 0x00;
    public static final byte APP_TYPE_PROGRAM_ROBOT = (byte) 0x01;
    public static final byte APP_TYPE_ABILIX_CHART = (byte) 0x02;
    public static final byte APP_TYPE_ABILIX_SCRATCH = (byte) 0x03;
    public static final byte APP_TYPE_SKILL_PLAYER = (byte) 0x04;
    public static final byte APP_TYPE_XICHEN_CONTROL = (byte) 0x05;
    public static final byte APP_TYPE_TIQIU = (byte) 0x06;
    public static final byte APP_TYPE_JIUYUAN = (byte) 0x07;

    /*播放动作,音频视频的类型*/
    public static final byte PLAY_TYPE_ACTION = (byte) 0x01;
    public static final byte PLAY_TYPE_SOUND = (byte) 0x02;
    public static final byte PLAY_TYPE_ACTION_AND_SOUND = (byte) 0x03;
    public static final byte PLAY_TYPE_VIDEO = (byte) 0x04;
    public static final byte PLAY_TYPE_ACTION_AND_VIDEO = (byte) 0x05;

    /*skillplayer文件传输命令字*/
    public static final byte FILE_RECEIVE_IN_CMD_1 = (byte) 0x01;
    public static final byte FILE_RECEIVE_IN_CMD_2_REQUEST = (byte) 0x01;
    public static final byte FILE_RECEIVE_IN_CMD_2_CANCEL = (byte) 0x02;
    public static final byte FILE_RECEIVE_IN_CMD_2_SEND = (byte) 0x03;
    public static final byte FILE_RECEIVE_IN_CMD_2_COMPLETE = (byte) 0x04;
    public static final byte FILE_RECEIVE_OUT_CMD_1 = (byte) 0xA0;
    public static final byte FILE_RECEIVE_OUT_CMD_2_NOTIFY = (byte) 0x01;
    public static final byte FILE_RECEIVE_OUT_CMD_2_CHECK_WRONG = (byte) 0x02;
    public static final byte FILE_RECEIVE_OUT_CMD_2_RECEIVE_SUCCESS = (byte) 0x03;

    public static final byte CMD_0 = (byte) 0xAA;
    public static final byte CMD_1 = (byte) 0x55;
    /**STM32主动上报时第二字节*/
    public static final byte CMD_1_REPORT = (byte) 0x66;

    /*播放动作,声音,视频文件命令字*/
    public static final byte PLAY_IN_CMD_1 = (byte) 0x02;
    public static final byte PLAY_IN_CMD_2_PLAY = (byte) 0x01;
    public static final byte PLAY_IN_CMD_2_PAUSE = (byte) 0x02;
    public static final byte PLAY_IN_CMD_2_STOP = (byte) 0x03;
    public static final byte PLAY_IN_CMD_2_GET_VOL = (byte) 0x04;
    public static final byte PLAY_IN_CMD_2_SET_VOL = (byte) 0x05;
    public static final byte PLAY_IN_CMD_2_RESUME = (byte) 0x06;

    public static final byte PLAY_OUT_CMD_1 = (byte) 0xA2;
    public static final byte PLAY_OUT_CMD_2_PLAY = (byte) 0x01;
    public static final byte PLAY_OUT_CMD_2_PAUSE = (byte) 0x02;
    public static final byte PLAY_OUT_CMD_2_STOP = (byte) 0x03;
    public static final byte PLAY_OUT_CMD_2_COMPLETE = (byte) 0x04;
    public static final byte PLAY_OUT_CMD_2_SEND_VOL = (byte) 0x05;
    public static final byte PLAY_OUT_CMD_2_RESUME = (byte) 0x06;

    /**
     * H系列获取SN
     */
    public static final byte GET_SN_CMD_1 = (byte) 0x11;
    public static final byte GET_SN_CMD_2 = (byte) 0x0F;

    /**
     * H系列设置子类型
     */
    public static final byte SET_CHILDREN_TYPE_CMD_1 = (byte) 0x11;
    public static final byte SET_CHILDREN_TYPE_CMD_2 = (byte) 0x0A;
    public static final byte GET_CHILDREN_TYPE_CMD_2 = (byte) 0x09;
    /*灯光控制*/
    public static final byte LIGHT_CONTROL_CMD_1 = (byte) 0x03;
    public static final byte LIGHT_CONTROL_CMD_2 = (byte) 0x01;

    /*触控控制pad端*/
    public static final byte HEAD_TOUCH_PAD_IN_CMD_1 = (byte) 0x03;
    public static final byte HEAD_TOUCH_PAD_IN_CMD_2 = (byte) 0x02;
    public static final byte HEAD_TOUCH_PAD_OUT_CMD_1 = (byte) 0xA2;
    public static final byte HEAD_TOUCH_PAD_OUT_CMD_2 = (byte) 0x06;
    /*触控控制固件端*/
    public static final byte HEAD_TOUCH_FIRMWARE_IN_CMD_1 = (byte) 0xF0;
    public static final byte HEAD_TOUCH_FIRMWARE_IN_CMD_2 = (byte) 0x60;
    public static final byte HEAD_TOUCH_FIRMWARE_OUT_CMD_1 = (byte) 0xA3;
    public static final byte HEAD_TOUCH_FIRMWARE_OUT_CMD_2 = (byte) 0x6C;
    /*M获取位移*/
    public static final byte WHEEL_ACTUAL_DISPLACEMENT_OUT_CMD_1 = (byte) 0xA3;
    public static final byte WHEEL_ACTUAL_DISPLACEMENT_OUT_CMD_2 = (byte) 0x3E;
    public static final byte WHEEL_ACTUAL_DISPLACEMENT_IN_CMD_1 = (byte) 0xF0;
    public static final byte WHEEL_ACTUAL_DISPLACEMENT_IN_CMD_2 = (byte) 0x33;
    /*H系列获取舵机相关*/
    public static final byte ENGINE_ANGLE_PAD_IN_CMD_1 = (byte) 0x04;
    public static final byte ENGINE_ANGLE_PAD_IN_CMD_2 = (byte) 0x03;
    public static final byte ENGINE_ANGLE_PAD_OUT_CMD_1 = (byte) 0xA2;
    public static final byte ENGINE_ANGLE_PAD_OUT_CMD_2 = (byte) 0x07;

    public static final byte ENGINE_FIRMWARE_OUT_CMD_1 = (byte) 0xA3;
    public static final byte ENGINE_FIRMWARE_OUT_CMD_1_OLD = (byte) 0xFE;
    public static final byte ENGINE_ANGLE_FIRMWARE_OUT_CMD_2 = (byte) 0x61;
    public static final byte ENGINE_CURRENT_FIMRWARE_OUT_CMD_2 = (byte) 0x63;
    public static final byte ENGINE_TEMPERATURE_FIMRWARE_OUT_CMD_2 = (byte) 0x64;
    public static final byte ENGINE_STATE_CHANGE_OUT_CMD_2 = (byte) 0x65;
    public static final byte ENGINE_GET_ZERO_OFFSET_OUT_CMD_2 = (byte) 0x66;
    public static final byte ENGINE_SET_ZERO_OFFSET_OUT_CMD_2 = (byte) 0x67;
    public static final byte ENGINE_GET_GYRO_OUT_CMD_2 = (byte) 0x70;
    public static final byte ENGINE_GET_GYRO_OUT_CMD_2_OLD = (byte) 0x68;
    public static final byte ENGINE_SET_SINGLE_ANGLE_OUT_CMD_2 = (byte) 0xA1;
    public static final byte ENGINE_SET_ANGLE_OUT_CMD_2 = (byte) 0xA2;
    public static final byte ENGINE_GET_ULTRASONIC_OUT_CMD_2 = (byte) 0xA3;
    public static final byte MACHINE_CALIBRATION_CMD_2 = (byte) 0x78;
    public static final byte ENGINE_SET_H34_LIGHT_CMD_2 = (byte) 0x74;
    public static final byte ENGINE_GET_ENGINE_INFO_OUT_CMD_2 = (byte) 0xC3;

    public static final byte ENGINE_FIRMWARE_IN_CMD_1 = (byte) 0xF0;
    public static final byte ENGINE_ANGLE_FIRMWARE_IN_CMD_2 = (byte) 0x61;
    public static final byte ENGINE_CURRENT_FIMRWARE_IN_CMD_2 = (byte) 0x63;
    public static final byte ENGINE_TEMPERATURE_FIMRWARE_IN_CMD_2 = (byte) 0x64;
    public static final byte ENGINE_GET_ZERO_OFFSET_IN_CMD_2 = (byte) 0x65;
    public static final byte ENGINE_GET_ENGINE_INFO_CMD_IN_CMD_2 = (byte)0x6F;
    public static final byte ENGINE_GET_GYRO_IN_CMD_2 = (byte) 0x70;
    public static final byte ENGINE_GET_ULTRASONIC_IN_CMD_2 = (byte) 0xA3;
    /*M系列眼部灯光命令*/
    public static final byte M_EYE_LIHGT_CMD_1 = (byte) 0xA3;
    public static final byte M_EYE_LIHGT_CMD_2 = (byte) 0x31;
    /*M系列轮子速度设置新协议*/
    public static final byte M_WHEEL_SPEED_OUT_CMD_1 = (byte) 0xA3;
    public static final byte M_WHEEL_SPEED_OUT_CMD_2 = (byte) 0x30;
    /*M系列stm32命令反馈*/
    public static final byte M_FEEDBACK_IN_CMD_1 = (byte) 0xF0;
    public static final byte M_FEEDBACK_IN_CMD_2 = (byte) 0x20;
    public static final byte M_FEEDBACK_OK = (byte) 0x00;
    public static final byte M_FEEDBACK_WRONG_HEAD = (byte) 0x01;
    public static final byte M_FEEDBACK_WRONG_CHECK = (byte) 0x02;
    /*M系列stm32心跳*/
    public static final byte M_STM32_HEART_BEAT_OUT_CMD_1 = (byte) 0xA3;
    public static final byte M_STM32_HEART_BEAT_OUT_CMD_2 = (byte) 0x42;
    public static final byte M_STM32_RELIEVE_PROTECT_OUT_CMD_1 = (byte) 0xA3;
    public static final byte M_STM32_RELIEVE_PROTECT_OUT_CMD_2 = (byte) 0x43;
    public static final byte M_STM32_HEART_BEAT_IN_CMD_1 = (byte) 0xF0;
    public static final byte M_STM32_HEART_BEAT_IN_CMD_2 = (byte) 0x34;
    /*M5获取传感器*/
    public static final byte M_SENSOR_OUT_CMD_1 = (byte) 0xA3;
    public static final byte M_SENSOR_OUT_CMD_2 = (byte) 0x37;
    public static final byte M_SENSOR_IN_CMD_1 = (byte) 0xF0;
    public static final byte M_SENSOR_M5_IN_CMD_2 = (byte) 0x30;

    /*H5 群控*/
    public static final byte H_GROUP_CMD_1 = (byte) 0xA3;
    public static final byte H_GROUP_CMD_RELEASE = (byte) 0x65;
    public static final byte H_GROUP_CMD_CAD = (byte) 0x7D;

    /*M1系列stm32心跳*/
    public static final byte M1_STM32_HEART_BEAT_OUT_CMD_1 = (byte) 0xA6;
    public static final byte M1_STM32_HEART_BEAT_OUT_CMD_2 = (byte) 0x3C;
    public static final byte M1_STM32_RELIEVE_PROTECT_OUT_CMD_1 = (byte) 0xA6;
    public static final byte M1_STM32_RELIEVE_PROTECT_OUT_CMD_2 = (byte) 0x3D;
    public static final byte M1_STM32_HEART_BEAT_IN_CMD_1 = (byte) 0xF3;
    public static final byte M1_STM32_HEART_BEAT_IN_CMD_2 = (byte) 0x33;

    /*C系列设置闭环电机*/
    public static final byte C_CLOSED_LOOP_MOTOR_OUT_CMD_1 = (byte) 0xA3;
    public static final byte C_CLOSED_LOOP_MOTOR_OUT_CMD_2 = (byte) 0x03;

    public  static final byte[] C_OPEN_LOOP_MOTOR_CMD={(byte)0xA3,(byte)0x13};

    /*多媒体播放命令字 2.3.15*/
    public static final byte MULTI_MEDIA_IN_CMD_1 = (byte) 0x22;
    public static final byte MULTI_MEDIA_IN_CMD_2_PLAY = (byte) 0x01;
    public static final byte MULTI_MEDIA_IN_CMD_2_PAUSE = (byte) 0x02;
    public static final byte MULTI_MEDIA_IN_CMD_2_STOP = (byte) 0x03;
    public static final byte MULTI_MEDIA_IN_CMD_2_RESUME = (byte) 0x04;

    public static final byte MULTI_MEDIA_OUT_CMD_1 = (byte) 0xA6;
    public static final byte MULTI_MEDIA_OUT_CMD_2_PLAY = (byte) 0x01;
    public static final byte MULTI_MEDIA_OUT_CMD_2_PAUSE = (byte) 0x02;
    public static final byte MULTI_MEDIA_OUT_CMD_2_STOP = (byte) 0x03;
    public static final byte MULTI_MEDIA_OUT_CMD_2_RESUME = (byte) 0x04;
    public static final byte MULTI_MEDIA_OUT_CMD_2_PLAY_COMPLETE = (byte) 0x05;

    /*项目编程命令字*/
    public static final byte S_PROGRAM_PROJECT_IN_CMD_1 = (byte) 0x08;
    public static final byte S_PROGRAM_PROJECT_IN_CMD_2_PLAY_LIST = (byte) 0x06;
    public static final byte S_PROGRAM_PROJECT_IN_CMD_2_STOP_LIST = (byte) 0x07;
    public static final byte S_PROGRAM_PROJECT_OUT_CMD_1 = (byte) 0xA1;
    public static final byte S_PROGRAM_PROJECT_OUT_CMD_2_PLAY_LIST_COMPLETE = (byte) 0x03;

    /*认识机器人命令字*/
    public static final byte KNOW_ROBOT_IN_CMD_1 = (byte) 0x20;
    public static final byte KNOW_ROBOT_IN_CMD_2_GYRO = (byte) 0x03;
    public static final byte KNOW_ROBOT_IN_CMD_2_PLAY_SOUND = (byte) 0x04;
    public static final byte KNOW_ROBOT_IN_CMD_2_H5_GAIT_MOVE = (byte) 0x10;
    public static final byte KNOW_ROBOT_IN_CMD_2_H5_GAIT_DERECTION = (byte) 0x11;
    public static final byte KNOW_ROBOT_IN_CMD_2_H5_GAIT_SETTING = (byte) 0x12;

    public static final byte KNOW_ROBOT_OUT_CMD_1 = (byte) 0xA4;
    public static final byte KNOW_ROBOT_OUT_CMD_2_GYRO = (byte) 0x01;
    public static final byte KNOW_ROBOT_OUT_CMD_2_H34_ROBOT_STATE = (byte) 0x05;
    public static final byte KNOW_ROBOT_OUT_CMD_2_TREAD_CONTROL = (byte) 0x08;
    public static final byte KNOW_ROBOT_OUT_CMD_2_MOVE_BALANCE = (byte) 0x0C;

    /*H自检程序命令字*/
    public static final byte SELF_CHECK_IN_CMD_1 = (byte) 0x25;
    public static final byte SELF_CHECK_IN_CMD_2_JOINT = (byte) 0x03;
    public static final byte SELF_CHECK_OUT_CMD_1 = (byte) 0xA7;
    public static final byte SELF_CHECK_OUT_CMD_2_JOINT = (byte) 0x02;
    /*S舵机检测命令字*/
    public static final byte ENGINE_TEST_S_OUT_CMD_1 = (byte)0xA3;
    public static final byte ENGINE_TEST_S_OUT_CMD_2_OPEN_WHEEL_MODE = (byte)0xA9;
    public static final byte ENGINE_TEST_S_OUT_CMD_2_SET_WHEEL_MODE = (byte)0xAA;
    public static final byte ENGINE_TEST_S_OUT_CMD_2_CHECK_ENGINE = (byte)0xAB;
    public static final byte ENGINE_TEST_S_OUT_CMD_2_SET_LIGHT = (byte)0xAC;

    public static final byte ENGINE_TEST_S_IN_CMD_1 = (byte)0xF0;
    public static final byte ENGINE_TEST_S_IN_CMD_2_CHECK_ENGINE = (byte)0xA3;

    /*模型控制命令*/
    public static final byte MODEL_C_201_IN_CMD_1 = (byte)0x24;
    public static final byte MODEL_C_201_IN_CMD_2_MODEL_TYPE = (byte)0x01;
    public static final byte MODEL_C_201_IN_CMD_2_MOVE = (byte)0x02;
    public static final byte MODEL_C_201_IN_CMD_2_FUNCTION = (byte)0x03;
    public static final byte MODEL_C_201_IN_CMD_2_ACTION = (byte)0x06;
}
