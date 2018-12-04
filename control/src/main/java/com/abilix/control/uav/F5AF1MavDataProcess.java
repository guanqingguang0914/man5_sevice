package com.abilix.control.uav;

import android.util.Log;

import com.abilix.control.utils.LogMgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yangq on 2017/7/3.
 * F/AF飞控数据处理
 */

public class F5AF1MavDataProcess extends MAVLinkDataProcess{
    private final float StopNum = DataTransform.bytesToFloat4(new byte[]{0x46, (byte) 0xa5, (byte) 0x98, 0x00}, 0);
    private final float SetUpNum1 = DataTransform.bytesToFloat4(new byte[]{(byte) 0xbf, (byte) 0x80, (byte) 0x00, 0x00}, 0);
    private final float SetUpNum2 = DataTransform.bytesToFloat4(new byte[]{(byte) 0xff, (byte) 0xc0, (byte) 0x00, 0x00}, 0);
    public static MAVLinkObj mavlinkObj = MAVLinkObj.GetManger();
    public static boolean[] senddata = new boolean[9];//是否发送传感器信息
    public static int[] sendtime = new int[9];//发送传感器信息时间间隔
    public static long[] sendLasttime = new long[9];//最后一次发送传感器信息时间
    private static F5AF1MavDataProcess dataProcess = null;
    public static F5AF1MavDataProcess GetManger() {
        // 单例
        if (dataProcess == null) {
            dataProcess = new F5AF1MavDataProcess();
            robotType=0x04;
            msgIDList = new ArrayList<Integer>(Arrays.asList(0,1, 27, 30, 32,33, 173, 181));//需要解析的msgID
        }
        return dataProcess;
    }
    @Override
    public void specificParseData(byte[] data){
        switch (DataBuffer.UAVType) {
            case 1://F
                switch (data[5] & 0xff) {//命令字1
                    case 0x23:// 控制指令
                        switch (data[6] & 0xff) {//命令字2
                            case 0x01://（解锁/锁定）
                                packageMAVLinkLong76(400, new float[]{(data[11] & 0xff), 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x02:// 起飞
                                if (!IfHasUnlock) {
                                    LogMgr.e("ParseData 没有解锁不能起飞 ");
                                    break;
                                }
                                packageMAVLinkModel11(0x03040000, new byte[]{1, (byte) 0xd1});
                                packageMAVLinkLong76(22, new float[]{SetUpNum1, 0, 0, SetUpNum2, SetUpNum2, SetUpNum2, DataTransform.bytesToFloat4h(data, 11)}, new byte[]{1, 1, 0});
                                break;
                            case 0x03:// 降落
                                if (!IfHasUnlock) {
                                    LogMgr.e("ParseData 没有解锁不能降落 ");
                                    break;
                                }
                                packageMAVLinkModel11(0x06040000, new byte[]{1, (byte) 0x9d});
                                break;
                            case 0x04:// 移动

                                break;
                            case 0x05:// 遥控
//                                if (!IfHasUnlock) {
//                                    LogMgr.e("ParseData 没有解锁不能遥控 ");
//                                    break;
//                                }
                                auvAFRemote[0] = (short) (DataTransform.bytesToShort2h(data, 11));
                                auvAFRemote[1] = (short) (DataTransform.bytesToShort2h(data, 13));
                                auvAFRemote[2] = (short) (DataTransform.bytesToShort2h(data, 15));
                                auvAFRemote[3] = (short) (DataTransform.bytesToShort2h(data, 17));
//                                auvAFRemote[1]=(short)(3000-auvAFRemote[1]);//F5 APP ios二通道反了，零时转换一下
                                packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                                break;
                            case 0x06:// 舵机测试

                                break;
                            case 0x07:// 模式设定
                                if (!IfHasUnlock) {
                                    LogMgr.e("ParseData 没有解锁不能模式设定 ");
                                    break;
                                }
//                                packageMAVLinkModel11(((data[11] & 0x0000ff) << 16), new byte[]{1, (byte) 0x81});
                                break;
                            case 0x08:// 是否发送传感器信息
                                int bitData = DataTransform.bytesToInt1(data, 11);
                                boolean ifopen = ((data[12] & 0x01) == 1);
                                if ((data[13] & 0xff) == 0) {//是否整体发送
                                    if (ifopen) {
                                        senddata[0] = true;
                                        sendtime[0] = DataTransform.bytesToInt2h(data, 14);
                                    } else senddata[0] = false;
                                    LogMgr.i("ParseData 请求所有传感器数据  " + senddata[0]);
                                } else {
                                    for (int i = 0; i <= 7; i++) {
                                        if (((bitData >> i) & 0x01) == 1) {
                                            switch (7 - i) {
                                                case 0:// 请求姿态传感器
                                                    if (ifopen) {
                                                        senddata[3] = true;
                                                        sendtime[3] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[3] = false;
                                                    LogMgr.i("ParseData 请求姿态传感器  " + senddata[3]);
                                                    break;
                                                case 1:// 请求方位传感器
                                                    if (ifopen) {
                                                        senddata[4] = true;
                                                        sendtime[4] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[4] = false;
                                                    LogMgr.i("ParseData 请求方位传感器  " + senddata[4]);
                                                    break;
                                                case 2:// 请求光流传感器
                                                    if (ifopen) {
                                                        senddata[5] = true;
                                                        sendtime[5] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[5] = false;
                                                    LogMgr.i("ParseData 请求光流传感器  " + senddata[5]);
                                                    break;
                                                case 3:// 请求超声传感器
                                                    if (ifopen) {
                                                        senddata[6] = true;
                                                        sendtime[6] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[6] = false;
                                                    LogMgr.i("ParseData 请求超声传感器  " +senddata[6]);
                                                    break;
                                                case 4:// 请求红外传感器
                                                    if (ifopen) {
                                                        senddata[7] = true;
                                                        sendtime[7] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[7] = false;
                                                    LogMgr.i("ParseData 请求红外传感器  " + senddata[7]);
                                                    break;
                                                case 7:// 请求系统状态
                                                    if (ifopen) {
                                                        senddata[8] = true;
                                                        sendtime[8] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[8] = false;
                                                    LogMgr.i("ParseData 请求系统状态  " + senddata[8]);
                                                    break;
                                            }
                                        }
                                    }
                                }
                                break;
                            case 0x09:// 紧急停止
                                if (!IfHasUnlock) {
                                    LogMgr.e("ParseData 没有解锁不能紧急停止 ");
                                    break;
                                }
                                packageMAVLinkLong76(400, new float[]{0, StopNum, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x51:// 设置边灯模式

                                break;
                            case 0x52:// 设置底灯模式

                                break;
                            case 0x53:// 设置手抓状态

                                break;
                        }
                        break;
                }
                break;
            case 2://AF1
                switch (data[5] & 0xff) {//命令字1
                    case 0x23:// 控制指令
                        switch (data[6] & 0xff) {//命令字2
                            case 0x01://（解锁/锁定）
                                packageMAVLinkLong76(400, new float[]{(data[11] & 0xff), 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                IfReqMavlink = false;
                                break;
                            case 0x02:// 起飞
                                packageMAVLinkLong76(22, new float[]{0, 0, 0, 0, 0, 0, DataTransform.bytesToFloat4h(data, 11)}, new byte[]{1, 1, 0});
                                break;
                            case 0x03:// 降落
                                packageMAVLinkLong76(21, new float[]{0, 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x04:// 移动

                                break;
                            case 0x05:// 遥控
                                auvAFRemote[0] = (short) (DataTransform.bytesToShort2h(data, 11));
                                auvAFRemote[1] = (short) (DataTransform.bytesToShort2h(data, 13));
                                auvAFRemote[2] = (short) (DataTransform.bytesToShort2h(data, 15));
                                auvAFRemote[3] = (short) (DataTransform.bytesToShort2h(data, 17));
                                packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], auvAFRemote[4], auvAFRemote[4], auvAFRemote[4], auvAFRemote[4]}, new byte[]{(byte) 1, 1});
                                break;
                            case 0x06:// 舵机测试

                                break;
                            case 0x07:// 模式设定
                                switch (data[11] & 0xff) {
                                    case 1:
                                        auvAFRemote[4] = 1100;
                                        break;
                                    case 2:
                                        auvAFRemote[4] = 1500;
                                        break;
                                    case 3:
                                        auvAFRemote[4] = 1900;
                                        break;
                                }
                                packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], auvAFRemote[4], auvAFRemote[4], auvAFRemote[4], auvAFRemote[4]}, new byte[]{(byte) 1, 1});
                                break;
                            case 0x08:// 是否发送传感器信息
                                int bitData = DataTransform.bytesToInt1(data, 11);
                                boolean ifopen = ((data[12] & 0x01) == 1);
                                if ((data[13] & 0xff) == 0) {//是否整体发送
                                    if (ifopen) {
                                        senddata[0] = true;
                                        sendtime[0] = DataTransform.bytesToInt2h(data, 14);
                                    } else senddata[0] = false;
                                    LogMgr.i("ParseData 请求所有传感器数据  " + senddata[0]);
                                } else {
                                    for (int i = 0; i <= 7; i++) {
                                        if (((bitData >> i) & 0x01) == 1) {
                                            switch (7 - i) {
                                                case 0:// 请求姿态传感器
                                                    if (ifopen) {
                                                        senddata[3] = true;
                                                        sendtime[3] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[3] = false;
                                                    LogMgr.i("ParseData 请求姿态传感器  " + senddata[3]);
                                                    break;
                                                case 1:// 请求方位传感器
                                                    if (ifopen) {
                                                        senddata[4] = true;
                                                        sendtime[4] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[4] = false;
                                                    LogMgr.i("ParseData 请求方位传感器  " + senddata[4]);
                                                    break;
                                                case 2:// 请求光流传感器
                                                    if (ifopen) {
                                                        senddata[5] = true;
                                                        sendtime[5] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[5] = false;
                                                    LogMgr.i("ParseData 请求光流传感器  " + senddata[5]);
                                                    break;
                                                case 3:// 请求超声传感器
                                                    if (ifopen) {
                                                        senddata[6] = true;
                                                        sendtime[6] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[6] = false;
                                                    LogMgr.i("ParseData 请求超声传感器  " + senddata[6]);
                                                    break;
                                                case 4:// 请求红外传感器
                                                    if (ifopen) {
                                                        senddata[7] = true;
                                                        sendtime[7] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[7] = false;
                                                    LogMgr.i("ParseData 请求红外传感器  " + senddata[7]);
                                                    break;
                                                case 7:// 请求系统状态
                                                    if (ifopen) {
                                                        senddata[8] = true;
                                                        sendtime[8] = DataTransform.bytesToInt2h(data, 14);
                                                    } else
                                                        senddata[8] = false;
                                                    LogMgr.i("ParseData 请求系统状态  " + senddata[8]);
                                                    break;
                                            }
                                        }
                                    }
                                }
                                break;
                            case 0x09:// 紧急停止
                                packageMAVLinkLong76(400, new float[]{0, StopNum, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x51:// 设置边灯模式

                                break;
                            case 0x52:// 设置底灯模式

                                break;
                            case 0x53:// 设置手抓状态

                                break;
                        }
                        break;
                }
                break;
        }
    }
    public void sendDataMobile() {//给pad发送数据
        if (System.currentTimeMillis() - mheartbeatTime >= 1000) {//向mobile发送心跳
            mheartbeatTime = System.currentTimeMillis();
            if(isShowLog[0]) Log.i("ParseData()", "sendDataMobile 向mobile发送心跳");
            sendData(0xA3, 0x00, DataTransform.LongTobyte(mheartbeatTime, (MAVLinkObj.GetManger().get_heartbeat().custom_mode >> 8) & 0x0f));
        }
//		Log.i("sendDataMobile","sendDataMobile出错了  "+11);
        if (senddata[0] && (System.currentTimeMillis() - sendLasttime[0]) >= sendtime[0]) {//所有基本传感器信息
//			  Log.i("RefreshUIThread","sendBasicData 发送mobile数据了  -- 0");
            sendBasicData();
            sendLasttime[0] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+22);
        if (senddata[1] && (System.currentTimeMillis() - sendLasttime[1]) >= sendtime[1]) {//扩展传感器类型1
            sendLasttime[1] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+33);
        if (senddata[2] && (System.currentTimeMillis() - sendLasttime[2]) >= sendtime[2]) {//扩展传感器类型2
            sendLasttime[2] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+44);
        if (senddata[3] && (System.currentTimeMillis() - sendLasttime[3]) >= sendtime[3]) {//姿态传感器
            sendAttitudeData();
            sendLasttime[3] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+55);
        if (senddata[4] && (System.currentTimeMillis() - sendLasttime[4]) >= sendtime[4]) {//方位传感器
            sendPositionData();
            sendLasttime[4] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+66);
        if (senddata[5] && (System.currentTimeMillis() - sendLasttime[5]) >= sendtime[5]) {//光流传感器
            sendFlowData();
//			  Log.i("sendDataMobile","sendDataMobile出错了  "+88);
            sendLasttime[5] = System.currentTimeMillis();
        }
//		  Log.i("sendDataMobile","sendDataMobile出错了  "+99);
        if (senddata[6] && (System.currentTimeMillis() - sendLasttime[6]) >= sendtime[6]) {//超声传感器
            sendRangeFinderData();
            sendLasttime[6] = System.currentTimeMillis();
        }
        if (senddata[7] && (System.currentTimeMillis() - sendLasttime[7]) >= sendtime[7]) {//红外传感器
            sendInfraredData();
            sendLasttime[7] = System.currentTimeMillis();
        }
        if (senddata[8] && (System.currentTimeMillis() - sendLasttime[8]) >= sendtime[8]) {//系统状态
            sendSystemData();
            sendLasttime[8] = System.currentTimeMillis();
        }
    }
    private int reqNum = 0;
    @Override
    public void readDataRun(byte[] mavData){
//        if ((mavData = getSerialDataArr()) != null) {
//            ParseMavlinkData(mavData);//接收并解析mavlink数据
//        } else if (!IfReqMavlink) {
//            reqNum++;
//            if (reqNum >= 50) {
//                reqMavlinkData();//请求飞控反馈数据
//                reqNum = 0;
//            }
//        } else reqNum = 0;
        sendDataMobile();//给pad反馈信息
        sendHeartMavlink();//给飞控发送mavlink心跳
        try {
            Thread.sleep(6);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public synchronized void sendBasicData() {//所有基本传感器信息
        byte[] data = new byte[52];
        int index = 0;
        if (DataBuffer.UAVType == 1) {
            index = putFloat(mavlinkObj.get_sys_status().voltage_battery, data, index);//Voltage :float 4Byte//电压
            index = putFloat(mavlinkObj.get_sys_status().current_battery, data, index);//Current:float 4Byte//电流
        } else if (DataBuffer.UAVType == 2) {
            index = putFloat(mavlinkObj.get_battery2().voltage, data, index);//Voltage :float 4Byte//电压
            index = putFloat(mavlinkObj.get_battery2().current_battery, data, index);//Current:float 4Byte//电流
        } else {
            index = putFloat(0, data, index);//Voltage :float 4Byte//电压
            index = putFloat(0, data, index);//Current:float 4Byte//电流
        }
        index = putFloat(mavlinkObj.get_attitude().pitch, data, index);//Pitch:float 4byte//姿态  	//俯仰角
        index = putFloat(mavlinkObj.get_attitude().roll, data, index);//Roll:float 4Byte 	//横滚
        index = putFloat(mavlinkObj.get_attitude().yaw, data, index);//yaw:float 4Byte 	//偏航角
        index = putInt(mavlinkObj.get_global_position_int().lat, data, index);//lat:int32 4Byte	//GPS
        index = putInt(mavlinkObj.get_global_position_int().lon, data, index);//lon:int32 4Byte	//GPS
        index = putFloat(mavlinkObj.get_global_position_int().alt, data, index);//alt:float 	//高度
        index = putFloat(mavlinkObj.get_global_position_int().hdg, data, index);//Orient: float 4Byte//指南针
        index = putFloat(0, data, index);//FlowX:float  4Byte//光流
        index = putFloat(0, data, index);//FlowY:float  4Byte
        index = putFloat(mavlinkObj.get_rangefinder().distance, data, index);//RangeFinder:float 4Byte//超声
        index = putShort((short) 0, data, index);//IR:int 2Byte//红外避障

//		index=putFloat(1.1f,data,index);//Voltage :float 4Byte//电压
//		index=putFloat(2.2f,data,index);//Current:float 4Byte//电流
//		index=putFloat(3.3f,data,index);//Pitch:float 4byte//姿态
//		index=putFloat(4.4f,data,index);//Roll:float 4Byte
//		index=putFloat(5.5f,data,index);//yaw:float 4Byte
//		index=putInt(66,data,index);//lat:int32 4Byte//GPS
//		index=putInt(77,data,index);//lon:int32 4Byte
//		index=putFloat(8.8f,data,index);//alt:float
//		index=putFloat(9.9f,data,index);//Orient: float 4Byte//指南针
//		index=putFloat(10.10f,data,index);//FlowX:float  4Byte//光流
//		index=putFloat(11.11f,data,index);//FlowY:float  4Byte
//		index=putFloat(12.12f,data,index);//RangeFinder:float 4Byte//超声
//		index=putShort((byte)13,data,index);//IR:int 1Byte//红外避障

        int state = 9;//各种状态与
        index = putShort((short) state, data, index);//Status：2Byte     ????目前还没有
        sendData(0xA3, 0x01, data);
    }

    public synchronized void sendAttitudeData() {//姿态传感器信息
        byte[] data = new byte[12];
        int index = 0;
        index = putFloat(mavlinkObj.get_attitude().pitch, data, index);//Pitch:float 4byte//姿态
        index = putFloat(mavlinkObj.get_attitude().roll, data, index);//Roll:float 4Byte
        index = putFloat(mavlinkObj.get_attitude().yaw, data, index);//yaw:float 4Byte、

//		index=putFloat(993.3f,data,index);//Pitch:float 4byte//姿态
//		index=putFloat(994.4f,data,index);//Roll:float 4Byte
//		index=putFloat(995.5f,data,index);//yaw:float 4Byte

        sendData(0xA3, 0x02, data);
    }

    public synchronized void sendPositionData() {//方位传感器信息
        byte[] data = new byte[16];
        int index = 0;
        index = putInt(mavlinkObj.get_global_position_int().lat, data, index);//lat:int32 4Byte//GPS
        index = putInt(mavlinkObj.get_global_position_int().lon, data, index);//lon:int32 4Byte
        index = putFloat(mavlinkObj.get_global_position_int().alt, data, index);//alt:float 4Byte
        index = putFloat(mavlinkObj.get_global_position_int().hdg, data, index);//Orient: float 4Byte//指南针


//		index=putInt(9966,data,index);//lat:int32 4Byte//GPS
//		index=putInt(9977,data,index);//lon:int32 4Byte
//		index=putFloat(998.8f,data,index);//alt: float
//		index=putFloat(999.9f,data,index);//Orient: float 4Byte//指南针

        sendData(0xA3, 0x03, data);
    }

    public synchronized void sendFlowData() {//光流传感器信息
        byte[] data = new byte[8];
        int index = 0;
//		index=putFloat((float)PositionDataBuffer.posData[1],data,index);//FlowX:float  4Byte//光流
//		index=putFloat((float)PositionDataBuffer.posData[2],data,index);//FlowY:float  4Byte

//		index=putFloat(9910.10f,data,index);//FlowX:float  4Byte//光流
//		index=putFloat(9911.11f,data,index);//FlowY:float  4Byte

        sendData(0xA3, 0x04, data);
    }

    public synchronized void sendRangeFinderData() {//超声传感器信息
        byte[] data = new byte[4];
        int index = 0;
        index = putFloat(mavlinkObj.get_rangefinder().distance, data, index);//RangeFinder:float 4Byte//超声

//		index=putFloat(9912.12f,data,index);//RangeFinder:float 4Byte//超声

        sendData(0xA3, 0x05, data);
    }

    public synchronized void sendInfraredData() {//红外传感器信息
        byte[] data = new byte[2];
        int index = 0;
//		index=putShort(DataBuffer.Infrared,data,index);//IR:int 2Byte//红外避障
//		index=putShort((byte)93,data,index);//IR:int 1Byte//红外避障

        sendData(0xA3, 0x06, data);
    }

    public synchronized void sendSystemData() {//系统状态信息
        byte[] data = new byte[10];
        int index = 0;
        if (DataBuffer.UAVType == 1) {
            index = putFloat(mavlinkObj.get_sys_status().voltage_battery, data, index);//Voltage :float 4Byte//电压
            index = putFloat(mavlinkObj.get_sys_status().current_battery, data, index);//Current:float 4Byte//电流
        } else if (DataBuffer.UAVType == 2) {
            index = putFloat(mavlinkObj.get_battery2().voltage, data, index);//Voltage :float 4Byte//电压
            index = putFloat(mavlinkObj.get_battery2().current_battery, data, index);//Current:float 4Byte//电流
        } else {
            index = putFloat(0, data, index);//Voltage :float 4Byte//电压
            index = putFloat(0, data, index);//Current:float 4Byte//电流
        }

//		index=putFloat(991.1f,data,index);//Voltage :float 4Byte//电压
//		index=putFloat(992.2f,data,index);//Current:float 4Byte//电流

        int state = 9;//各种状态与
        index = putShort((short) state, data, index);//Status：2Byte
        sendData(0xA3, 0x07, data);
    }

    public synchronized void sendIMU() {//imu信息
        byte[] data = new byte[12];
        int index = 0;
        index = putShort(mavlinkObj.get_raw_imu().xacc, data, index);//
        index = putShort(mavlinkObj.get_raw_imu().yacc, data, index);//
        index = putShort(mavlinkObj.get_raw_imu().zacc, data, index);//
        index = putShort(mavlinkObj.get_raw_imu().xgyro, data, index);//
        index = putShort(mavlinkObj.get_raw_imu().xgyro, data, index);//
        index = putShort(mavlinkObj.get_raw_imu().xgyro, data, index);//
        sendData(0xA3, 0x08, data);
    }

    public synchronized void sendPositionNed() {//高度以及速速
        byte[] data = new byte[12];
        int index = 0;
        index = putFloat(mavlinkObj.get_local_position_ned().z, data, index);//
        index = putFloat(mavlinkObj.get_local_position_ned().vz, data, index);//
        float v = (float) Math.sqrt(mavlinkObj.get_local_position_ned().vx * mavlinkObj.get_local_position_ned().vx + mavlinkObj.get_local_position_ned().vy * mavlinkObj.get_local_position_ned().vy);
        index = putFloat(v, data, index);//
        sendData(0xA3, 0x09, data);
    }

    //	public static byte IntTobyte(int dataD,int allNum, int byteNum){// int转换为字节      高位在前低位在后 , data数据，allNum转化成的字节个数，byteNum第几个字节
//		if(allNum<1 || byteNum<1)return (byte)0x00;
//		int bytedata = 0;
//		switch(allNum){
//		case 4:
//			return (byte)(dataD >>(8*(byteNum-1)));
//		case 3:
//			return (byte)(dataD >>(8*(byteNum-1)));
//		case 2:
//			return (byte)(dataD >>(8*(byteNum-1)));
//		case 1:
//			return (byte)(dataD >>(8*(byteNum-1)));
//		default: return (byte)0x00;
//		}
//	}
}
