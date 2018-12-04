package com.abilix.control;


import android.os.Build;

import com.abilix.control.aidl.Control;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.patch.HPatchDisposer;
import com.abilix.control.patch.MPatchDisposer;
import com.abilix.control.patch.PatchTracker;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import com.abilix.control.sp.SP;


/**
 * @author jingh
 * @Descripton:该类负责Control的初始化，包括读取机器人主类型、平台类型、Log日志、传感器，初始化串口，读取 机器人子类型
 * @date2017-3-28下午5:02:31
 */

public class ControlInitiator {
//    public static int MAIN_ROBOT_TYPE_BRAIN = 0X00;
//    public static int CHILD_ROBOT_TYPE = 0X00;
    /* 系统版本号 */
    private final static String ROBOT_BUILD_TYPE = Build.DISPLAY;

    /* 硬件平台名称 */
    public static final String HARDWARE_TYPE_RK = "rk30board";
    public static final String HARDWARE_TYPE_MT = "mt6580";
    public static final int HW_TYPE_RK = 0X02;
    public static final int HW_TYPE_MT = 0X01;
    public static final int HW_TYPE_UNKNOWN = 0x00;

    /* 机器人类型 */
    public static final byte ROBOT_TYPE_COMMON = (byte) 0x00;
    public static final byte ROBOT_TYPE_C = (byte) 0x01;
    public static final byte ROBOT_TYPE_M = (byte) 0x02;
    public static final byte ROBOT_TYPE_H = (byte) 0x03;
    public static final byte ROBOT_TYPE_F = (byte) 0x04;
    public static final int ROBOT_TYPE_S = 0x05;
    public static final int ROBOT_TYPE_AF = 0x06;
    public static final int ROBOT_TYPE_U = 0x07;

    public static final int ROBOT_TYPE_BRIANC = 0x09;
    public static final int ROBOT_TYPE_M0 = 0x0C;
    public static final int ROBOT_TYPE_M1 = 0x0B;
    public static final int ROBOT_TYPE_M2 = 0x0D;
    public static final int ROBOT_TYPE_M3 = 0x0E;
    public static final int ROBOT_TYPE_M4 = 0x0F;
    public static final int ROBOT_TYPE_M3S = 0x4E;
    public static final int ROBOT_TYPE_M4S = 0x4F;
    public static final int ROBOT_TYPE_M5 = 0x10;
    public static final int ROBOT_TYPE_M6 = 0x12;
    public static final int ROBOT_TYPE_M7 = 0x13;
    public static final int ROBOT_TYPE_C1_2 = 0x49;
    public static final int ROBOT_TYPE_C9= 0x0A;
    public static final int ROBOT_TYPE_CU = 0x50;
    public static final int ROBOT_TYPE_H3 = 0x23;
    public static final int ROBOT_TYPE_H5 = 0x25;
    public static final int ROBOT_TYPE_SE901 = 0x6C;
    //add S3-S8
    public static final int ROBOT_TYPE_S3 = 0x37;
    public static final int ROBOT_TYPE_S4 = 0x38;
    public static final int ROBOT_TYPE_S7 = 0x3A;
    public static final int ROBOT_TYPE_S8 = 0x3B;

    // U系列
    public static final int ROBOT_TYPE_U5 = 0x51;

    /* 串口类型 */
    public static final int SERIALPORT_TYPE_UNKNOWN = 0x00;
    /**C1 U STM32串口*/
    public static final int SERIALPORT_TYPE_MT0_115200 = 0X01;
    /**H STM32串口(未被使用)*/
    public static final int SERIALPORT_TYPE_MT0_230400 = 0X02;
    /**C和M，F STM32串口*/
    public static final int SERIALPORT_TYPE_MT1_230400 = 0X03;
    /**H STM8串口、S STM32串口 F STM32串口2*/
    public static final int SERIALPORT_TYPE_MT1_500000 = 0X04;
    /**AF STM32串口*/
    public static final int SERIALPORT_TYPE_MT0_56700 = 0X05;
    /**AF STM32串口2*/
    public static final int SERIALPORT_TYPE_MT1_56700 = 0X06;
    /**U 串口2*/
    public static final int SERIALPORT_TYPE_MT1_115200 = 0X07;
    /** U5 STM32串口*/
    public static final int SERIALPORT_TYPE_MT0_500000 = 0X08;

//     获取机器人主类型、平台类型，并据此得出串口类型
//    static {
//        if (ROBOT_BUILD_TYPE.length() >= 1) {
//            LogMgr.i("ROBOT_BUILD_TYPE.charAt(0) = " + ROBOT_BUILD_TYPE.charAt(0));
//            LogMgr.i("hardware type::" + Build.HARDWARE);
//            switch (ROBOT_BUILD_TYPE.charAt(0)) {
//                case 'C':
//                    if(ROBOT_BUILD_TYPE.charAt(1) == 9){
//                        ControlInfo.setMain_robot_type(ROBOT_TYPE_C9);
//                    }else{
//                        ControlInfo.setMain_robot_type(ROBOT_TYPE_C);
//                    }
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_115200);
//                        ControlInfo.setMain_robot_type(ROBOT_TYPE_BRIANC);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//
//                case 'M':
//                    if (ROBOT_BUILD_TYPE.charAt(1) == 'C') {
//                        ControlInfo.setMain_robot_type(ROBOT_TYPE_M1);
//                    } else {
//                        ControlInfo.setMain_robot_type(ROBOT_TYPE_M);
//                    }
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//
//                case 'S':
//                    ControlInfo.setMain_robot_type(ROBOT_TYPE_S);
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//
//                case 'H':
//                    ControlInfo.setMain_robot_type(ROBOT_TYPE_H);
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
////                        ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT0_230400);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//                case 'F':
//                    ControlInfo.setMain_robot_type(ROBOT_TYPE_F);
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_115200);
//                        ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_500000);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//                // 代表AF
//                case 'A':
//                    ControlInfo.setMain_robot_type(ROBOT_TYPE_AF);
//                    if (Build.HARDWARE.equals(HARDWARE_TYPE_RK)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_RK);
//                    } else if (Build.HARDWARE.equals(HARDWARE_TYPE_MT)) {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                        ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_56700);
//                        ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_56700);
//                    } else {
//                        ControlInfo.setPlatform_type(HW_TYPE_MT);
//                    }
//                    break;
//            }
//        }
//    }


    private static final int  CORE_MTK6580 = 1;
    private static final int  CORE_RK3128 = 2;

    private static final int FUNCTION_DEFAULT = 0;
    private static final int FUNCTION_C_201 = 1;
    private static final int FUNCTION_C9 = 2;
    private static final int FUNCTION_H3_H4 = 1;
    private static final int FUNCTION_SE901 = 3;
    static {
        switch (Utils.getProductSerial(ROBOT_BUILD_TYPE)){
            case "C":
                if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    if(Utils.getFunctionVersionNumber(ROBOT_BUILD_TYPE) == FUNCTION_C_201){
                        //表示目前用于C项目平衡车（201）项目
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_C);
                        ControlInfo.setChild_robot_type(ROBOT_TYPE_CU);
                    }else if(Utils.getFunctionVersionNumber(ROBOT_BUILD_TYPE) == FUNCTION_C9){
                        //表示目前用于C项目C9项目
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_C9);
                        ControlInfo.setChild_robot_type(ROBOT_TYPE_C9);
                    }else{
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_C);
                    }
                }else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128){
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_115200);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_BRIANC);
                }else{
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_C);
                }
                break;
            case "M":
                if (Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128) {
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_M1);
                } else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_M);
                } else{
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_M);
                }
                break;
            case "H":
                if (Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128) {
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                } else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
                    if(Utils.getFunctionVersionNumber(ROBOT_BUILD_TYPE) == FUNCTION_H3_H4){
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_H3);
                        ControlInfo.setChild_robot_type(ROBOT_TYPE_H3);
                       // ControlInfo.setChild_robot_type(ControlInfo.getChild_robot_type());
                    }else if(Utils.getFunctionVersionNumber(ROBOT_BUILD_TYPE) == FUNCTION_SE901){
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_H);
//                        ControlInfo.setChild_robot_type(ROBOT_TYPE_SE901);
                    }else{
                        ControlInfo.setMain_robot_type(ROBOT_TYPE_H);
                    }
                } else{
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
                    ControlInfo.setMain_robot_type(ROBOT_TYPE_H);
                }
                break;
            case "F":
                ControlInfo.setMain_robot_type(ROBOT_TYPE_F);
                if (Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128) {
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                } else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_500000);
                } else {
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_230400);
                    ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_500000);
                }
                break;
            case "S":
                ControlInfo.setMain_robot_type(ROBOT_TYPE_S);
                if (Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128) {
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                } else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
                } else {
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT1_500000);
                }
                break;
            case "AF":
                ControlInfo.setMain_robot_type(ROBOT_TYPE_AF);
                if (Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_RK3128) {
                    //rk3128平台
                    ControlInfo.setPlatform_type(HW_TYPE_RK);
                } else if(Utils.getCoreVersionNumber(ROBOT_BUILD_TYPE) == CORE_MTK6580){
                    //mtk6580平台
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_56700);
                    ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_56700);
                } else{
                    ControlInfo.setPlatform_type(HW_TYPE_MT);
                    ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_56700);
                    ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_56700);
                }
                break;
            case "U":
                ControlInfo.setMain_robot_type(ROBOT_TYPE_U);
                ControlInfo.setChild_robot_type(ROBOT_TYPE_U5);
                ControlInfo.setPlatform_type(HW_TYPE_MT);
                ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_115200);
                ControlInfo.setMotor_serialport_type(SERIALPORT_TYPE_MT1_115200);
                //TODO yhd 项目组说修改波特率
//                ControlInfo.setMain_serialport_type(SERIALPORT_TYPE_MT0_500000);
                break;
            default:
                ControlInfo.setMain_robot_type(ROBOT_TYPE_C);
                break;
        }
    }

    /**
     * Control相关初始化操作都在这里进行,且初始化顺序不能随意改变
     */
    public static void init() {
        initLog();
        initSP();
        initChildRobotType();
        initSensor();
        initControlFactory();
        startMotorProtect();
        startFingerProtect();
        getBatteryState();
    }

    /**
     * 初始化串口
     */
    private static void initSP() {
        SP.initSerial(ControlInfo.getMain_serialport_type());
    }

    /**
     * 初始化机器人子类型
     */
    private static void initChildRobotType() {
        int robotType = getChildRobotType();
        saveRobotType(robotType);
        if (robotType > 0) {
            ControlInfo.setChild_robot_type(robotType);
        }
    }

    /**
     * 初始化传感器
     */
    private static void initSensor() {
        MySensor.obtainMySensor(ControlApplication.instance).openSensorEventListener();
    }

    /**
     * 初始化Control工厂
     */
    private static void initControlFactory() {
        ControlFactory.buildFactory(ControlInfo.getChild_robot_type());
    }

    /**
     * 初始化Log日志
     */
    private static void initLog() {
        LogMgr.startExportLog();
    }

    /**
     * 开启M轮子电机保护
     */
    private static void startMotorProtect() {
        Control motorProtectControl = new Control(15, null);
        motorProtectControl.setModeState(MPatchDisposer.MOTRO_HEART_BEAT);
        ControlKernel.getInstance().dispatchCmd(motorProtectControl);
    }

    /**
     * 开启H手指保护
     */
    private static void startFingerProtect() {
        Control fingerProtectControl = new Control(15, null);
        fingerProtectControl.setModeState(HPatchDisposer.H_FINGER_PROTECT);
        ControlKernel.getInstance().dispatchCmd(fingerProtectControl);
    }

    /**
     * 开启从STM32获取电池电量
     */
    private static void getBatteryState() {
        Control getBatteryControl = new Control(PatchTracker.GET_BATTERY, null);
        ControlKernel.getInstance().dispatchCmd(getBatteryControl);
    }


    /**
     * 获取机器人子类型
     */
    private static int getChildRobotType() {
        byte[] getChildRobotType_byte = ProtocolBuilder.buildProtocol(ROBOT_TYPE_COMMON, ProtocolBuilder.CMD_GET_CHILD_ROBOT_TYPE, null);

        /*        SP.write(getChildRobotType_byte);
        byte[] byte_readRobotType = new byte[40];
        try {
            SP.write(byte_readRobotType);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        byte[] byte_readRobotType=SP.request(getChildRobotType_byte);
        if(byte_readRobotType==null){
            return -1;
        }
        LogMgr.d("robot type bytes:" + Utils.bytesToString(byte_readRobotType));
        if (byte_readRobotType[5] == (byte) 0xF0 && byte_readRobotType[6] == (byte) 0x07) {
            int type = byte_readRobotType[11];
            if (byte_readRobotType[11] == (byte) 0xff) {
                LogMgr.e("未设置机器人类型");
                return -1;
            }
            LogMgr.d("机器人类型：" + type);
            return type;
        }
        LogMgr.e("获取机器人类型失败");
        return -1;
    }

    private static void saveRobotType(int robotType) {
        FileUtils.saveFile(robotType + "", FileUtils.ROBOTY_TYPE);
    }
}
