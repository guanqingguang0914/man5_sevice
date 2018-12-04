package com.abilix.control.uav;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yangq on 2017/7/3.
 * F3/F4飞控数据处理
 */

public class F3MavDataProcess extends MAVLinkDataProcess {
    private final float StopNum = DataTransform.bytesToFloat4(new byte[]{0x46, (byte) 0xa5, (byte) 0x98, 0x00}, 0);
    private final float SetUpNum1 = DataTransform.bytesToFloat4(new byte[]{(byte) 0xbf, (byte) 0x80, (byte) 0x00, 0x00}, 0);
    private final float SetUpNum2 = DataTransform.bytesToFloat4(new byte[]{(byte) 0xff, (byte) 0xc0, (byte) 0x00, 0x00}, 0);
    public  final float Accuracy=0.1f/2;//定位精度
    private boolean[] informationType=new boolean[2];
    private long[] lastSendInfTime=new long[2];
    private static int dataSeq=1;//项目编程模块序号
    private static int runDirection=1;//项目编程模块运动方向，进度计算方法不一样
    private boolean dataSeqCh=false;//是否换一个项目编程模块了
    private boolean dataExeStart=false;//一个项目编程模块是否开始
    private boolean stopExe=false;//是否停止执行项目编程模块
    private float[] dataPar=new float[3];//项目编程参数
    private float[] dataProgress=new float[2];//项目编程进度
    public float[] executionTime=new float[2];//一个项目编程模块执行的开始结束数据
    public long[] exLastTime=new long[1];//时间
    private static F3MavDataProcess dataProcess = null;
    public static F3MavDataProcess GetManger() {
        // 单例
        if (dataProcess == null) {
            dataProcess = new F3MavDataProcess();
            msgIDList = new ArrayList<Integer>(Arrays.asList(0, 27, 30, 32,105,106, 173, 181));//需要解析的msgID
        }
        return dataProcess;
    }

    @Override
    public void specificParseData(byte[] data){
        switch (data[5] & 0xff) {//命令字1
            case 0x23:// 控制指令
                switch (data[6] & 0xff) {//命令字2
                    case 0x80://（心跳
                        switch (data[11] & 0xff) {
                            case 1://飞行器状态信息
                                informationType[0]=true;
                                informationType[1]=false;
                                break;
                            case 2://传感器信息
                                informationType[0]=false;
                                informationType[1]=true;
                                break;
                            case 3://飞行器状态信息和传感器信息都发送
                                informationType[0]=true;
                                informationType[1]=true;
                                break;
                        }
                        break;
                    case 0x81://（解锁/锁定）
                        packageMAVLinkLong76(400, new float[]{(data[11] & 0xff), 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                        if((data[11] & 0xff)==1)IfHasUnlock=true;
                        else IfHasUnlock=false;
                        break;
                    case 0x83:// 起飞
                        if (!IfHasUnlock) {
                            LogMgr.e("ParseData 没有解锁不能起飞 ");
                            break;
                        }
                        packageMAVLinkModel11(0x03040000, new byte[]{1, (byte) 0xd1});
                        packageMAVLinkLong76(22, new float[]{SetUpNum1, 0, 0, SetUpNum2, SetUpNum2, SetUpNum2, DataTransform.bytesToFloat4h(data, 11)}, new byte[]{1, 1, 0});
                        break;
                    case 0x84:// 降落
                        if (!IfHasUnlock) {
                            LogMgr.e("ParseData 没有解锁不能降落 ");
                            break;
                        }
                        packageMAVLinkModel11(0x06040000, new byte[]{1, (byte) 0x9d});
                        break;
                    case 0x85:// 校准

                        break;
                    case 0x86:// 模式设定
                        if (!IfHasUnlock) {
                            LogMgr.e("ParseData 没有解锁不能模式设定 ");
                            break;
                        }
                        packageMAVLinkModel11(((data[11] & 0x0000ff) << 16), new byte[]{1, (byte) 0x81});
                        break;
                    case 0x90:// 遥控
                        if (!IfHasUnlock) {
                            LogMgr.e("ParseData 没有解锁不能遥控 ");
                            break;
                        }
                        auvAFRemote[0] = (short) (DataTransform.bytesToFloat4h(data, 11));
                        auvAFRemote[1] = (short) (DataTransform.bytesToFloat4h(data, 15));
                        auvAFRemote[2] = (short) (DataTransform.bytesToFloat4h(data, 19));
                        auvAFRemote[3] = (short) (DataTransform.bytesToFloat4h(data, 23));
                        for(int i=0;i<4;i++) auvAFRemote[i]=(short)(1100+auvAFRemote[i]*800f/1000f);
                        if(isShowLog[0])LogMgr.i("ParseData 遥控 "+ auvAFRemote[0]+"   "+ auvAFRemote[1]+"   "+ auvAFRemote[2]+"   "+ auvAFRemote[3]);
                        packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                        break;
                    case 0x91:// 动作设定
                        if(dataSeq!=(data[20] & 0xff)){
                            dataSeqCh=true;
                            dataSeq=(data[20] & 0xff);
                            exLastTime[0]=System.currentTimeMillis();
                        }else dataSeqCh=false;
                        runDirection=(data[11] & 0xff);
                        switch (runDirection) {
                            case 0x21://解锁
                                packageMAVLinkLong76(400, new float[]{1, 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x22://锁定
                                packageMAVLinkLong76(400, new float[]{0, 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0});
                                break;
                            case 0x01://起飞
                                packageMAVLinkModel11(0x03040000, new byte[]{1, (byte) 0xd1});
                                packageMAVLinkLong76(22, new float[]{SetUpNum1, 0, 0, SetUpNum2, SetUpNum2, SetUpNum2, DataTransform.bytesToFloat4h(data, 12)}, new byte[]{1, 1, 0});
                                break;
                            case 0x02://降落
                                packageMAVLinkModel11(0x06040000, new byte[]{1, (byte) 0x9d});
                                break;
                            case 0x03://前进 -y
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().y;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().y)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]-dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{0, executionTime[1], 0}, new byte[]{ 1, 1,1});
                                }

//                                if(dataSeqCh)executionTime[0]=System.currentTimeMillis();
//                                if(dataSeqCh || (!dataSeqCh && (executionTime[1]-System.currentTimeMillis())>100)){//结束前100ms就不发了，防止执行完后再执行一次
//                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
//                                    dataPar[1]=DataTransform.bytesToFloat4h(data, 16);
//                                    if(dataPar[0]!=0 && dataPar[1]!=0 ){
//                                        dataPar[2]=dataPar[0]/dataPar[1];//执行时长
//                                        executionTime[1]= (long) (executionTime[0]+dataPar[2]);//执行的目标时间
//                                        packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
//                                    }
//                                }
                                break;
                            case 0x04://后退 +y
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().y;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().y)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]+dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{0, executionTime[1], 0}, new byte[]{ 1, 1,1});
                                }
                                break;
                            case 0x05://左移 -x
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().x;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().x)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]-dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{ executionTime[1],0, 0}, new byte[]{ 1, 1,1});
                                }
                                break;
                            case 0x06://右移 +x
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().x;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().x)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]+dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{ executionTime[1],0, 0}, new byte[]{ 1, 1,1});
                                }
                                break;
                            case 0x07://上升 -z
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().z;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().z)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]-dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{0, 0,executionTime[1]}, new byte[]{ 1, 1,1});
                                }
//                                if(dataSeqCh)executionTime[0]=(long)(mavlinkObj.get_global_position_int().alt);
//                                if(dataSeqCh || (!dataSeqCh && (executionTime[1]-(long)(mavlinkObj.get_global_position_int().alt))>0)){
//                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
//                                    executionTime[1]= executionTime[0]+(long)dataPar[0];//执行的目标
////                                    packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
//                                }
                                break;
                            case 0x08://下降 +z
                                if(dataSeqCh)executionTime[0]=mavlinkObj.get_local_position_ned().z;
                                if(dataSeqCh || (!dataSeqCh && Math.abs(executionTime[1]-mavlinkObj.get_local_position_ned().z)>=Accuracy)){
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    executionTime[1]= executionTime[0]+dataPar[0];//执行的目标
                                    packageMAVLinkSetPosition84(new float[]{0, 0,executionTime[1]}, new byte[]{ 1, 1,1});
                                }
                                break;
                            case 0x09://旋转
                                if(dataSeqCh){
                                    executionTime[0]=(long) (mavlinkObj.get_attitude().yaw);
                                    dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                    if(dataPar[0]>=0)runDirection=0x09;//正转
                                    else runDirection=0x0A;//反转
                                }
                                if(runDirection==0x09){//正转
                                    if(dataSeqCh || (!dataSeqCh && (executionTime[1]-(long)(mavlinkObj.get_attitude().yaw))>0)){
                                        dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                        if(dataPar[0]>=0)runDirection=0x09;//正转
                                        else runDirection=0x0A;//反转
                                        executionTime[1]= executionTime[0]+(long)dataPar[0];//执行的目标
//                                    packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                                    }
                                }else{//反转
                                    if(dataSeqCh || (!dataSeqCh && (executionTime[1]-(long)(mavlinkObj.get_attitude().yaw))<0)){
                                        dataPar[0]=DataTransform.bytesToFloat4h(data, 12);
                                        if(dataPar[0]>=0)runDirection=0x09;//正转
                                        else runDirection=0x0A;//反转
                                        executionTime[1]= executionTime[0]-(long)dataPar[0];//执行的目标
//                                    packageMAVLinkOverride70(new short[]{auvAFRemote[0], auvAFRemote[1], auvAFRemote[2], auvAFRemote[3], 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                                    }
                                }
                                break;
                            case 0x0A://手动

                                break;
                            case 0x0B://定高

                                break;
                            case 0x0C://定点

                                break;
                        }
                        break;
                    case 0x92:// 停止项目编程
                        stopExe=true;
                        //也可在结束后添加其他动作
                        break;
                }
                break;
        }
    }

    @Override
    public void readDataRun(byte[] mavData){
        sendDataMobile();//给pad反馈信息
        sendHeartMavlink();//给飞控发送mavlink心跳
        try {
            Thread.sleep(5);
        } catch (Exception e) {
            e.printStackTrace();
        }
        doExeListen();
    }
    //中心坐标系转空间坐标系
    public void doExeListen(){//项目编程模块执行过程监听进度
        switch(runDirection){
            case 0x03:// 前进
            case 0x04:// 后退
                if(!stopExe && (executionTime[1]-mavlinkObj.get_local_position_ned().x)>0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)Math.abs(100*(mavlinkObj.get_local_position_ned().x-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
            case 0x05:// 左移
            case 0x06:// 右移
                if(!stopExe && (executionTime[1]-System.currentTimeMillis())>0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100*(System.currentTimeMillis()-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
            case 0x07:// 上升
                if(!stopExe && (executionTime[1]-(long)(mavlinkObj.get_global_position_int().alt))>0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100*((long)(mavlinkObj.get_global_position_int().alt)-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
            case 0x08:// 下降
                if(!stopExe && (executionTime[1]-(long)(mavlinkObj.get_global_position_int().alt))<0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100*((long)(mavlinkObj.get_global_position_int().alt)-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
            case 0x09:// 正旋转
                if(!stopExe && (executionTime[1]-(long)(mavlinkObj.get_attitude().yaw))>0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100*((long)(mavlinkObj.get_attitude().yaw)-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
            case 0x0A:// 反旋转
                if(!stopExe && (executionTime[1]-(long)(mavlinkObj.get_attitude().yaw))<0){
                    dataExeStart=true;
                    dataProgress[0]=(System.currentTimeMillis()-exLastTime[0])/200;
                    if(dataProgress[0] != dataProgress[1] && !stopExe){//每200ms发送一次进度
                        dataProgress[1] = dataProgress[0];
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100*((long)(mavlinkObj.get_attitude().yaw)-executionTime[0])/(executionTime[1]-executionTime[0]))});
                    }
                }else if(dataExeStart){//一个项目编程模块开始了
                    packageMAVLinkOverride70(new short[]{1500, 1500, 1500, 1500, 0, 0, 0, 0}, new byte[]{(byte) 1, 1});
                    dataExeStart=false;
                    if(stopExe){//紧急停止了
                        //也可在结束后在此添加其他动作
                        stopExe=false;
                    }else{
                        sendData(0xA3, 0x83, new byte[]{(byte)dataSeq ,(byte)(100)});
                    }
                }
                break;
        }
    }
    public void sendDataMobile() {//给pad发送数据
        if (informationType[0] && (System.currentTimeMillis() - lastSendInfTime[0]) >= 100) {//飞行器状态信息
//			  Log.i("RefreshUIThread","sendBasicData 发送飞行器状态信息数据了  -- 0");
            sendStatuData();
            lastSendInfTime[0] = System.currentTimeMillis();
        }
        if (informationType[1] && (System.currentTimeMillis() - lastSendInfTime[1]) >= 100 && (System.currentTimeMillis() - lastSendInfTime[0]) >= 40) {//传感器信息
//			  Log.i("RefreshUIThread","sendBasicData 发送传感器信息数据了  -- 1");
            sendSensorinfData();
            lastSendInfTime[1] = System.currentTimeMillis();
        }
    }
    public synchronized void sendStatuData() {//飞行器状态信息
        byte[] data = new byte[21];
        int index = 0;
        int flyState=1;
        switch(custom_mode){
            case 0x08:
                flyState=1;
                break;
            case 0x09:
                flyState=1;
                break;
        }
        index = putByte((byte)flyState, data, index);//模式
        index = putFloat(mavlinkObj.get_attitude().pitch, data, index);//Pitch:float 4byte//姿态  	//俯仰角
        index = putFloat(mavlinkObj.get_attitude().roll, data, index);//Roll:float 4Byte 	//横滚
        index = putFloat(mavlinkObj.get_attitude().yaw, data, index);//yaw:float 4Byte 	//偏航角
        index = putFloat(mavlinkObj.get_optical_flow_rad().distance, data, index);//alt:float 	//高度
        index = putFloat(mavlinkObj.get_sys_status().voltage_battery, data, index);//Voltage :float 4Byte//电压
        sendData(0xA3, 0x80, data);
    }
    public synchronized void sendSensorinfData() {//传感器信息
        byte[] data = new byte[64];
        int index = 0;
        index = putByte((byte) 3, data, index);//FlowY:float  1Byte//光流状态
        index = putByte((byte) 3, data, index);//FlowY:float  1Byte//陀螺仪状态
        index = putByte((byte) 3, data, index);//FlowY:float  1Byte//测高计状态
        index = putFloat(mavlinkObj.get_optical_flow_rad().integrated_x, data, index);//FlowX:float  4Byte//光流
        index = putFloat(mavlinkObj.get_optical_flow_rad().integrated_y, data, index);//FlowY:float  4Byte
        index = putByte(mavlinkObj.get_optical_flow_rad().quality, data, index);//FlowY:float  1Byte//光流质量
        index = putFloat(mavlinkObj.get_optical_flow_rad().distance, data, index);//RangeFinder:float 4Byte//超声
        index = putFloat(mavlinkObj.get_optical_flow_rad().distance, data, index);//alt:float 	//高度
        index = putFloat(mavlinkObj.get_attitude().yaw, data, index);//Orient: float 4Byte//指南针
        index = putFloat(mavlinkObj.get_highres_imu().xacc, data, index);//float  4Byte//加速度计
        index = putFloat(mavlinkObj.get_highres_imu().yacc, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_highres_imu().zacc, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_highres_imu().xgyro, data, index);//float  4Byte//陀螺仪
        index = putFloat(mavlinkObj.get_highres_imu().ygyro, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_highres_imu().zgyro, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_highres_imu().xmag, data, index);//float  4Byte//磁罗盘
        index = putFloat(mavlinkObj.get_highres_imu().ymag, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_highres_imu().zmag, data, index);//float  4Byte
        index = putFloat(mavlinkObj.get_sys_status().voltage_battery, data, index);//Voltage :float 4Byte//电压
        sendData(0xA3, 0x81, data);
    }
}
