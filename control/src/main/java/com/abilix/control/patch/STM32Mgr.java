package com.abilix.control.patch;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


import com.abilix.control.ControlInfo;
import com.abilix.control.GlobalConfig;
import com.abilix.control.aidl.Control;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class STM32Mgr {
    public static final String BATTERY_SAVE_PAT = "/sys/class/power_supply/battery/partnerx_set_voltage";
    private static Timer getBatteryStateTimer, isHeadTouchTimer, isHeadTouchTimer50;
    private static TimerTask getBatteryTimerTask, isHeadTouchTimerTask, isHeadTouchTimerTask50;
    private static int isHeadTouchTimes = 0;
    private static boolean isHadRest = false;

    public static void sleepOrWeak(Control control, IPatchDisposer mPatchDisposer) {
        if (control != null) {
            if (control.getModeState() == 1) {//休息
                LogMgr.d("receive sleap cmd");
//                setSTM32Sleep(true);
                isHadRest = true;
                stopIsHeadTouch();
                stopIsHeadTouch50();
                setRobotXiadun();
            } else if (control.getModeState() == 0) {//停止休息
                LogMgr.d("receive weak cmd");
                isHadRest = false;
                setSTM32Sleep(false);
                isHeadTouch(control, mPatchDisposer);
            }else if(control.getModeState() == 2){//复位；停止头部触摸检测
                LogMgr.d("头部触摸停止");
                stopIsHeadTouch();
                stopIsHeadTouch50();
            }else if(control.getModeState() == 3){//编程断开手脚灯亮;低电量充电时亮起手脚灯
                ProtocolUtils.handAndFeetLight(true);
            } else {//低电量没充电时关闭手脚灯
                ProtocolUtils.handAndFeetLight(false);
            }
        }
    }

    private static void setRobotXiadun() {
        boolean isStateXiaDun = ProtocolUtils.isStateXiadun();
//        boolean isStateXiaDun = false;
        LogMgr.d("isStateXiaDun = " + isStateXiaDun);
        try {
            Thread.sleep(100);
            if(isStateXiaDun){
                setSTM32Sleep(true);
                Thread.sleep(50);
                byte[] color = new byte[14];//这里显示的额头的灯
                color[0] = 2;
                color[1] = 3;
                color[2] = 1;
                color[6] = 2;
                color[10] = 3;
                color[3] = (byte)0xFF;
                color[4] = (byte)0;
                color[5] = (byte)0xFF;
                SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));

                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LogMgr.d("H5下蹲动作保护");
        PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
        String movePath2 = GlobalConfig.MOVE_BIN_PATH + File.separator + "H5_xiadun.bin";
        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath2, null, false, false, 0, false,
                PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onResume() {

                    }

                    @Override
                    public void onStop() {
                        LogMgr.i("onStop");
                        //下蹲之后，舵机断电，并显示紫灯
                        try {
                            if(isHadRest){
                                setSTM32Sleep(true);
                            }
                            Thread.sleep(50);
                            byte[] color = new byte[14];//这里显示的额头的灯,并关闭眼睛灯
                            color[0] = 2;
                            color[1] = 3;
                            color[2] = 1;
                            color[6] = 2;
                            color[10] = 3;
                            color[3] = (byte)0xFF;
                            color[4] = (byte)0;
                            color[5] = (byte)0xFF;
                            SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onSingleMoveStopWhileLoop() {

                    }

                    @Override
                    public void onNormalStop() {

                    }

                    @Override
                    public void onForceStop() {

                    }
                });
    }

    private static void stopIsHeadTouch() {
        LogMgr.d("stopIsHeadTouch");
        if (isHeadTouchTimer != null) {
            isHeadTouchTimer.cancel();
            isHeadTouchTimer = null;
        }
        if (isHeadTouchTimerTask != null) {
            isHeadTouchTimerTask.cancel();
            isHeadTouchTimerTask = null;
        }
    }

    private static void stopIsHeadTouch50() {
        LogMgr.d("stopIsHeadTouch50");
        isHeadTouchTimes = 0;
        if (isHeadTouchTimer50 != null) {
            isHeadTouchTimer50.cancel();
            isHeadTouchTimer50 = null;
        }
        if (isHeadTouchTimerTask50 != null) {
            isHeadTouchTimerTask50.cancel();
            isHeadTouchTimerTask50 = null;
        }
    }

    private static void isHeadTouch(final Control control, final IPatchDisposer mPatchDisposer) {
        stopIsHeadTouch();
        byte[] color = new byte[6];//这里显示的额头的灯
        color[0] = 2;
        color[1] = 1;
        color[2] = 1;
        color[3] = (byte) 0;
        color[4] = (byte) 0xff;
        color[5] = (byte) 0;
        SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
        isHeadTouchTimer = new Timer();
        isHeadTouchTimerTask = new TimerTask() {
            @Override
            public void run() {
                isHeadTouch50(control, mPatchDisposer);
//                if(isHeadTouchTimes >= 3){
//                    stopIsHeadTouch();
//                    stopIsHeadTouch50();
//                    //这里走播放bin文件的逻辑
//                    mPatchDisposer.DisposeProtocol(control);
//                }
            }
        };
        isHeadTouchTimer.schedule(isHeadTouchTimerTask, 50, 1500);
    }

    private static void isHeadTouch50(final Control control, final IPatchDisposer mPatchDisposer) {
        stopIsHeadTouch50();
        isHeadTouchTimer50 = new Timer();
        isHeadTouchTimerTask50 = new TimerTask() {
            @Override
            public void run() {
                byte[] headtouch_write = ProtocolBuilder.buildProtocol((byte) ControlInfo.getMain_robot_type(), ProtocolBuilder.CMD_HEADTOUCH, null);
                byte[] request = SP.request(headtouch_write, 80);
                if (request == null) {
                    return;
                }
                LogMgr.d("request = " + Utils.bytesToString(request));
                LogMgr.d("request ===== " + ((request[5] & 0xff) == 0xf0));
                if ((request[5] & 0xFF) == 0xf0 && (request[6] & 0xFF) == 0x69 && request[12] == 1) {
                    isHeadTouchTimes++;
                    LogMgr.d("isHeadTouchTimes======= = " + isHeadTouchTimes);
                }
                LogMgr.d("isHeadTouchTimes = " + isHeadTouchTimes);
                if (isHeadTouchTimes >= 3) {
                    stopIsHeadTouch();
                    stopIsHeadTouch50();
                    //这里走播放bin文件的逻辑
                    mPatchDisposer.DisposeProtocol(control);
                }
            }
        };
        isHeadTouchTimer50.schedule(isHeadTouchTimerTask50, 50, 100);
    }

    public static void turnOff(Control control) {
        LogMgr.e("STM32关机");
        byte[] turnoff_robot = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x10, 0x00, 0x11, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20};
        try {
            for (int i = 0; i < 5; i++) {
                SP.request(turnoff_robot);
                Thread.sleep(50);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void setSTM32Sleep(boolean isSleep) {
        if (isSleep) {
            LogMgr.d("set stm32 sleep");
            byte[] sleep = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x10, 0x00, 0x11, 0x08, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x29};
            for (int i = 0; i < 1; i++) {
                try {
                    SP.request(sleep);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            LogMgr.d("set stm32 weak");
            byte[] weak = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x10, 0x00, 0x11, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x28};
            for (int i = 0; i < 1; i++) {
                try {
                    SP.write(weak);
                    Thread.sleep(200);
                    ProtocolUtils.relAndFix(1, (byte) 0x18);
                    Thread.sleep(200);
                    //手部脚步灯亮
                    ProtocolUtils.handAndFeetLight(true);
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void getBatteryState() {
        if (getBatteryStateTimer != null) {
            getBatteryStateTimer.cancel();
            getBatteryStateTimer = null;
        }

        if (getBatteryTimerTask != null) {
            getBatteryTimerTask.cancel();
            getBatteryTimerTask = null;
        }

        getBatteryStateTimer = new Timer();
        getBatteryTimerTask = new TimerTask() {

            @Override
            public void run() {
                byte[] battery_write = ProtocolBuilder.buildProtocol((byte) ControlInfo.getMain_robot_type(), ProtocolBuilder.CMD_BATTERY, null);

                byte[] battery_read = SP.request(battery_write, 10);
                if (battery_read == null) {
                    return;
                }
                byte[] battery_voltage = new byte[2];
                System.arraycopy(battery_read, 11, battery_voltage, 0, 2);
                //读到的电压值是实际电压×100
                int battery_voltage_int = Utils.byte2int_2byteHL(battery_voltage, 0);
                LogMgr.d("STM32读取值:" + battery_voltage_int);
                //实际电压值
                //	float battery_voltage_double=battery_voltage_int/100;
                //以mv为单位电压
                float battery_voltage_mv = battery_voltage_int * 10;
                LogMgr.d("将电压值写入文件:" + battery_voltage_mv);
                FileUtils.saveFile(battery_voltage_mv + "", BATTERY_SAVE_PAT);
            }
        };

        getBatteryStateTimer.schedule(getBatteryTimerTask, 0, 5000);
    }
}
