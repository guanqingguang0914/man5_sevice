package com.abilix.control;

import com.abilix.control.utils.LogMgr;

public class ControlInfo {
    private static int main_robot_type = ControlInitiator.ROBOT_TYPE_S;
    private static int child_robot_type = ControlInitiator.ROBOT_TYPE_COMMON;
    private static int platform_type = ControlInitiator.HW_TYPE_UNKNOWN;
    private static int main_serialport_type = ControlInitiator.SERIALPORT_TYPE_MT1_230400;
    private static int motor_serialport_type = ControlInitiator.SERIALPORT_TYPE_UNKNOWN;

    public static int getMain_robot_type() {
        return main_robot_type;
    }

    public static void setMain_robot_type(int main_robot_type) {
        LogMgr.d("设置主类型为 main_robot_type:" + main_robot_type);
        ControlInfo.main_robot_type = main_robot_type;
    }

    /**
     * 获取机器子类型 如果子类型<1(未能从stm32获取子类型)时，返回主类型
     * @return
     */
    public static int getChild_robot_type() {
        if (child_robot_type < 1) {
            return main_robot_type;
        }
        return child_robot_type;
    }

    /**
     * 设置机器子类型，一次启动中只能设置一次，多次设置时并不产生效果
     * @param child_robot_type
     */
    public static void setChild_robot_type(int child_robot_type) {
        LogMgr.d("设置子类型为 child_robot_type:" + child_robot_type);
        if(ControlInfo.child_robot_type != 0x00){
            LogMgr.w("已经设置过子类型，不再重复设置。当前子类型为 = "+ControlInfo.child_robot_type);
            return;
        }
        ControlInfo.child_robot_type = child_robot_type;
    }

    public static int getPlatform_type() {
        return platform_type;
    }

    /**
     * 设置Android类型平台
     * @param platform_type
     */
    public static void setPlatform_type(int platform_type) {
        LogMgr.d("设置平台类型 platform_type:" + platform_type);
        ControlInfo.platform_type = platform_type;
    }

    public static int getMain_serialport_type() {
        return main_serialport_type;
    }

    public static void setMain_serialport_type(int main_serialport_type) {
        LogMgr.d("设置串口类型 main_serialport_type:" + main_serialport_type);
        ControlInfo.main_serialport_type = main_serialport_type;
    }

    public static int getMotor_serialport_type() {
        return motor_serialport_type;
    }

    public static void setMotor_serialport_type(int motor_serialport_type) {
        LogMgr.d("设置次串口类型motor_serialport_type:" + motor_serialport_type);
        ControlInfo.motor_serialport_type = motor_serialport_type;
    }
}
