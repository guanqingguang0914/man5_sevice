package com.abilix.control.uav;

import com.MAVLink.ardupilotmega.msg_ahrs;
import com.MAVLink.ardupilotmega.msg_ahrs2;
import com.MAVLink.ardupilotmega.msg_battery2;
import com.MAVLink.ardupilotmega.msg_rangefinder;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_highres_imu;
import com.MAVLink.common.msg_nav_controller_output;
import com.MAVLink.common.msg_optical_flow_rad;
import com.MAVLink.common.msg_raw_imu;
import com.MAVLink.common.msg_raw_pressure;
import com.MAVLink.common.msg_scaled_pressure;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_local_position_ned;
import com.MAVLink.common.msg_system_time;

import android.util.Log;

public class MAVLinkObj {
    private static MAVLinkObj mavlinkObj = null;

    public static MAVLinkObj GetManger() {
        // 单例
        if (mavlinkObj == null) {
            mavlinkObj = new MAVLinkObj();
        }
        return mavlinkObj;
    }

    public static byte[] remoteControl = new byte[26];// 遥控指令缓冲区,解析之后的
    public static byte[] InfraredData = new byte[41];// 红外避障缓冲区,解析之后的

    public static msg_heartbeat heartbeat = null;// 0
    public static msg_sys_status sys_status = null;// 1
    public static msg_system_time system_time = null;// 2
    public static msg_gps_raw_int gps_raw_int = null;// 24
    public static msg_raw_imu raw_imu = null;// 27
    public static msg_raw_pressure raw_pressure = null;// 28
    public static msg_scaled_pressure scaled_pressure = null;// 29
    public static msg_attitude attitude = null;// 30
    public static msg_local_position_ned msg_local_position_ned = null;// 32
    public static msg_global_position_int global_position_int = null;// 33
    public static msg_nav_controller_output nav_controller_output = null;// 62
    public static msg_command_ack command_ack = null;// 77
    public static msg_highres_imu highres_imu = null;// 105
    public static msg_optical_flow_rad optical_flow_rad = null;// 106
    public static msg_ahrs ahrs = null;// 163
    public static msg_rangefinder rangefinder = null;// 173
    public static msg_ahrs2 ahrs2 = null;// 178
    public static msg_battery2 msg_battery2 = null;// 181

    public void set_heartbeat(msg_heartbeat obj) {//1
        heartbeat = obj;
    }

    public void set_sys_status(msg_sys_status obj) {//1
        sys_status = obj;
    }

    public void set_system_time(msg_system_time obj) {//2
        system_time = obj;
    }

    public void set_gps_raw_int(msg_gps_raw_int obj) {//24
        gps_raw_int = obj;
    }

    public void set_raw_imu(msg_raw_imu obj) {//27
        raw_imu = obj;
    }

    public void set_raw_pressure(msg_raw_pressure obj) {//28
        raw_pressure = obj;
    }

    public void set_scaled_pressure(msg_scaled_pressure obj) {//29
        scaled_pressure = obj;
    }

    public void set_attitude(msg_attitude obj) {//30
        attitude = obj;
    }

    public void set_global_position_int(msg_global_position_int obj) {//33
        global_position_int = obj;
    }

    public void set_nav_controller_output(msg_nav_controller_output obj) {//62
        nav_controller_output = obj;
    }
    public void set_command_ack(msg_command_ack obj) {//77
        command_ack = obj;
    }

    public void set_highres_imu(msg_highres_imu obj) {//105
        highres_imu = obj;
    }
    public void set_optical_flow_rad(msg_optical_flow_rad obj) {//106
        optical_flow_rad = obj;
    }

    public void set_ahrs(msg_ahrs obj) {//163
        ahrs = obj;
    }

    public void set_rangefinder(msg_rangefinder obj) {//173
        rangefinder = obj;
    }

    public void set_ahrs2(msg_ahrs2 obj) {//178
        ahrs2 = obj;
    }

    public msg_heartbeat get_heartbeat() {//0
        if (heartbeat == null) {
            heartbeat = new msg_heartbeat();
        }
        return heartbeat;
    }

    public msg_sys_status get_sys_status() {//1
        if (sys_status == null) {
            sys_status = new msg_sys_status();
        }
        return sys_status;
    }

    public msg_system_time get_system_time() {//2
        if (system_time == null) {
            system_time = new msg_system_time();
        }
        return system_time;
    }

    public msg_gps_raw_int get_gps_raw_int() {//24
        if (gps_raw_int == null) {
            gps_raw_int = new msg_gps_raw_int();
        }
        return gps_raw_int;
    }

    public msg_raw_imu get_raw_imu() {//27
        if (raw_imu == null) {
            raw_imu = new msg_raw_imu();
        }
        return raw_imu;
    }

    public msg_raw_pressure get_raw_pressure() {//28
        if (raw_pressure == null) {
            raw_pressure = new msg_raw_pressure();
        }
        return raw_pressure;
    }

    public msg_scaled_pressure get_scaled_pressure() {//29
        if (scaled_pressure == null) {
            scaled_pressure = new msg_scaled_pressure();
        }
        return scaled_pressure;
    }

    public msg_attitude get_attitude() {//30
        if (attitude == null) {
            Log.i("RefreshUIThread", "sendBasicData attitude   》》》》》》》》》》》》》》》》》");
            attitude = new msg_attitude();
        }
        return attitude;
    }

    public msg_local_position_ned get_local_position_ned() {//32
        if (msg_local_position_ned == null) {
            msg_local_position_ned = new msg_local_position_ned();
        }
        return msg_local_position_ned;
    }

    public msg_global_position_int get_global_position_int() {//33
        if (global_position_int == null) {
            global_position_int = new msg_global_position_int();
        }
        return global_position_int;
    }

    public msg_nav_controller_output get_nav_controller_output() {//62
        if (nav_controller_output == null) {
            nav_controller_output = new msg_nav_controller_output();
        }
        return nav_controller_output;
    }

    public msg_command_ack get_command_ack() {//77
        if (command_ack == null) {
            command_ack = new msg_command_ack();
        }
        return command_ack;
    }
    public msg_highres_imu get_highres_imu() {//105
        if (highres_imu == null) {
            highres_imu = new msg_highres_imu();
        }
        return highres_imu;
    }
    public msg_optical_flow_rad get_optical_flow_rad() {//106
        if (optical_flow_rad == null) {
            optical_flow_rad = new msg_optical_flow_rad();
        }
        return optical_flow_rad;
    }

    public msg_ahrs get_ahrs() {//163
        if (ahrs == null) {
            ahrs = new msg_ahrs();
        }
        return ahrs;
    }

    public msg_rangefinder get_rangefinder() {//173
        if (rangefinder == null) {
            rangefinder = new msg_rangefinder();
        }
        return rangefinder;
    }

    public msg_ahrs2 get_ahrs2() {//178
        if (ahrs2 == null) {
            ahrs2 = new msg_ahrs2();
        }
        return ahrs2;
    }

    public msg_battery2 get_battery2() {//181
        if (msg_battery2 == null) {
            msg_battery2 = new msg_battery2();
        }
        return msg_battery2;
    }
}
