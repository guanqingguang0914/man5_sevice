package com.abilix.control.uav;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.ardupilotmega.CRC;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_optical_flow;
import com.MAVLink.common.msg_rc_channels_override;
import com.MAVLink.common.msg_request_data_stream;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yangq on 2017/6/30.
 */

public abstract class MAVLinkDataProcess extends DataProcess{
    public static MAVLinkObj mavlinkObj = MAVLinkObj.GetManger();
    public static int mavlink_sysid = 255;
    public static int mavlink_compid = 0;
    public static byte[] heartbeat;
    public static List<Integer> msgIDList = new ArrayList<Integer>(Arrays.asList(0,1,27,30,32,33,105,106,173,181));//需要解析的msgID
    public static int mavlink_seq = 0;//
    public static boolean showGetMavlink = false;//是否打印接收到的mavlink数据分类log
    public static boolean showGetMobile = false;//是否打印接收到的pad数据分类log
    public static boolean showSendMavlink = false;//是否打印发送的mavlink数据分类log
    public static boolean IfReqMavlink = false;//是否请求飞控反馈数据
    public static boolean IfHasUnlock = false;//是否已经解锁
    public int custom_mode=1;

    public void sendHeartMavlink() {//向飞控发送心跳
        if (System.currentTimeMillis() - heartbeatTime >= 1000) {
            sendHeartbeatData();//向APM发送心跳
            heartbeatTime = System.currentTimeMillis();
        }
    }
    public void reqMavlinkData() {//请求飞控反馈数据
        if (!IfReqMavlink) {
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x02);
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x06);
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x02);
            packageMAVLinkRequestData66((short) 0x0400, (byte) 0x0A);
            packageMAVLinkRequestData66((short) 0x0400, (byte) 0x0B);
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x0C);
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x01);
            packageMAVLinkRequestData66((short) 0x0200, (byte) 0x03);
        }
    }

    @Override
    public void ParseSerialData(byte[] data){//处理接收到的串口数据
        ParseMavlinkData(data);
    }
    public void ParseMavlinkData(byte[] readData) {//解析mavlink
        int readlength = 0;
        if(isShowLog[0])LogMgr.i("ParseMavlinkData 收到串口数据 "+showDataHex(readData));

        if (readData == null || (readlength = readData.length) < 8 || readlength>250) return;
        if (readData[0] == (byte) 0xfe) {
            int readlen = readData[1] & 0xff;
            if (readlength >= readlen + 8) {
                 IfReqMavlink = true;//说明已经接收到mavlink反馈数据了
                if (msgIDList.contains((readData[5] & 0xff))) {//只有特定的数据才解析
                    byte[] readData0 = new byte[readlen];
                    System.arraycopy(readData, 6, readData0, 0, readlen);//直接复制有用的data数据
                    if(isShowLog[0]) LogMgr.i("ParseMavlinkData", "ParseMavlinkData报文  mavlink_sysid = "+(readData[3] & 0xff)+"  mavlink_compid = "+(readData[4] & 0xff)+"  mavlink_msgid= "+(readData[5] & 0xff));
                    MAVLinkPacket mavlinkPacket = new MAVLinkPacket();
                    mavlinkPacket.len = readData[1] & 0xff;
                    mavlinkPacket.seq = readData[2] & 0xff;
                    mavlinkPacket.sysid = readData[3] & 0xff;
                    mavlinkPacket.compid = readData[4] & 0xff;
                    mavlinkPacket.msgid = readData[5] & 0xff;
                    for (int i = 0; i < readlen; i++) {
                        mavlinkPacket.payload.putByte(readData0[i]);
                    }
                    mavlinkPacket.generateCRC();
                    CRC crcMavlink = mavlinkPacket.crc;
//					if(crcMavlink.getLSB()==readData[readlen+8-2] && crcMavlink.getMSB()==readData[readlen+8-1]){	//校验通过
                    mavlinkPacket.unpack();
//					}else LogMgr.e("ParseMavlinkData 解析的mavlink协议校验不通过");
                    if ((readData[5] & 0xff) == 0)
//                        IfHasUnlock = (((mavlinkObj.get_heartbeat().base_mode >> 7) & 0x01) == 0x01);
                        custom_mode=mavlinkObj.get_heartbeat().custom_mode;
                }
            }
            if (readlength > readlen + 8 + 8) {//粘包
                byte[] readData2 = new byte[readlength - (readlen + 8)];
                System.arraycopy(readData, readlen + 8, readData2, 0, readlength - (readlen + 8));
                ParseMavlinkData(readData2);
            }
        }
    }
    public byte[] getPayload(byte[] readDatam) {////将自定义协议报文转化为mavlink协议报文
        MAVLinkPacket mavlinkPacket = new MAVLinkPacket();
        mavlinkPacket.len = (readDatam[1] & 0xff) - 4;//9;//
        mavlinkPacket.sysid = mavlink_sysid;
        mavlinkPacket.compid = mavlink_compid;
        mavlinkPacket.msgid = readDatam[5] & 0xff;
        for (int i = 6; i <= readDatam.length - 3; i++) {
            mavlinkPacket.payload.putByte(readDatam[i]);
        }
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        return sendbyte0;
    }

    public synchronized static void packageMAVLinkHeartbeat0() {//发送心跳
        msg_heartbeat mavlinkMessage = new msg_heartbeat();
        mavlinkMessage.custom_mode = 0;
        mavlinkMessage.type = 0x02;
        mavlinkMessage.autopilot = 0x03;
        mavlinkMessage.system_status = 0x03;
        mavlinkMessage.base_mode = 0x51;
        mavlinkMessage.mavlink_version = 0x03;
        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        heartbeat = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
    }

    public synchronized void sendHeartbeatData() {//心跳
        packageMAVLinkHeartbeat0();
        sendSerialData(heartbeat);
//		if(DataBuffer.showSendMavlink)LogMgr.i("MAVLinkDataProcess 发送  心跳"+dataProcess.showDataHex(heartbeat));
    }

    public synchronized void packageMAVLinkModel11(int model, byte[] dataByte) {//将模式数据转换成mavlink字节数组并存发送缓冲区
        if (dataByte.length < 2) {
            LogMgr.e("MAVLinkDataProcess 发送  模式长度不够");
            return;
        }
        msg_set_mode mavlinkMessage = new msg_set_mode();
        mavlinkMessage.custom_mode = model;
        mavlinkMessage.target_system = dataByte[0];
        mavlinkMessage.base_mode = dataByte[1];

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        sendSerialData(sendbyte0);
        if (showSendMavlink) LogMgr.i("MAVLinkDataProcess 发送  模式设定");
    }

    public synchronized void packageMAVLinkRequestData66(short messageRate, byte streamId) {//请求反馈数据
        msg_request_data_stream mavlinkMessage = new msg_request_data_stream();
        mavlinkMessage.req_message_rate = messageRate;
        mavlinkMessage.target_system = 0x01;
        mavlinkMessage.target_component = 0x01;
        mavlinkMessage.req_stream_id = streamId;
        mavlinkMessage.start_stop = 0x01;

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        sendSerialData(sendbyte0);
        if (showSendMavlink) LogMgr.i("MAVLinkDataProcess 发送  飞控数据反馈请求");
    }

    public synchronized void packageMAVLinkOverride70(short[] dataShort, byte[] dataByte) {//将长协议数据转换成mavlink字节数组并存发送缓冲区
        if (dataShort.length < 8 || dataByte.length < 2) {
            LogMgr.e("MAVLinkDataProcess 发送  遥控长度不够");
            return;
        }
        msg_rc_channels_override mavlinkMessage = new msg_rc_channels_override();
        mavlinkMessage.chan1_raw = dataShort[0];
        mavlinkMessage.chan2_raw = dataShort[1];
        mavlinkMessage.chan3_raw = dataShort[2];
        mavlinkMessage.chan4_raw = dataShort[3];
        mavlinkMessage.chan5_raw = dataShort[4];
        mavlinkMessage.chan6_raw = dataShort[5];
        mavlinkMessage.chan7_raw = dataShort[6];
        mavlinkMessage.chan8_raw = dataShort[7];
        mavlinkMessage.target_system = dataByte[0];
        mavlinkMessage.target_component = dataByte[1];

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        sendSerialData(sendbyte0);
        if (showSendMavlink && (isShowLog[0]))LogMgr.i("MAVLinkDataProcess 发送  遥控" + showDataHex(sendbyte0));
    }

    public synchronized void packageMAVLinkLong76(int command, float[] dataFloat, byte[] dataByte) {//将长协议数据转换成mavlink字节数组并存发送缓冲区
        if (dataFloat.length < 7 || dataByte.length < 3) {
            LogMgr.e("MAVLinkDataProcess 发送  长指令长度不够");
            return;
        }
        msg_command_long mavlinkMessage = new msg_command_long();
        mavlinkMessage.param1 = dataFloat[0];
        mavlinkMessage.param2 = dataFloat[1];
        mavlinkMessage.param3 = dataFloat[2];
        mavlinkMessage.param4 = dataFloat[3];
        mavlinkMessage.param5 = dataFloat[4];
        mavlinkMessage.param6 = dataFloat[5];
        mavlinkMessage.param7 = dataFloat[6];
        mavlinkMessage.command = (short) command;
        mavlinkMessage.target_system = dataByte[0];
        mavlinkMessage.target_component = dataByte[1];
        mavlinkMessage.confirmation = dataByte[2];

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        sendSerialData(sendbyte0);
        if (showSendMavlink) {
            String strr = "";
            switch (command) {
                case 400:
                    strr = "  解锁/锁定";
                    break;
                case 22:
                    strr = "  起飞";
                    break;
                case 21:
                    strr = "  降落";
                    break;
                case 179:
                    strr = "  设定起始位置";
                    break;
                case 178:
                    strr = "  设定最大速度";
                    break;
                case 113:
                    strr = "  改变高度";
                    break;
                case 82:
                    strr = "  移动到目标点";
                    break;
                case 115:
                    strr = "  回旋";
                    break;
                case 209:
                    strr = "  舵机测试";
                    break;
                case 176:
                    strr = "  模式设定";
                    break;
                case 77:
                    strr = "  ACK";
                    break;
            }
            if(isShowLog[0])LogMgr.i("MAVLinkDataProcess 发送  " + strr + showDataHex(sendbyte0));
        }
    }
    public synchronized void packageMAVLinkSetPosition84(float[] dataFloat, byte[] dataByte) {//将长协议数据转换成mavlink字节数组并存发送缓冲区
        if (dataFloat.length < 4|| dataByte.length < 3) {
            LogMgr.e("MAVLinkDataProcess 发送  长指令长度不够");
            return;
        }
        msg_set_position_target_local_ned mavlinkMessage = new msg_set_position_target_local_ned();
        mavlinkMessage.x = dataFloat[0];
        mavlinkMessage.y = dataFloat[1];
        mavlinkMessage.z = dataFloat[2];
        mavlinkMessage.yaw = dataFloat[3];
        mavlinkMessage.type_mask = 0x3DF8;
        mavlinkMessage.target_system = dataByte[0];
        mavlinkMessage.target_component = dataByte[1];
        mavlinkMessage.coordinate_frame = dataByte[2];//1
        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        sendSerialData(sendbyte0);
        if(isShowLog[0])LogMgr.i("MAVLinkDataProcess 发送84  " + "位置设定" + showDataHex(sendbyte0));
    }

    public synchronized byte[] packageMAVLinkFlow100(long[] dataLong, float[] dataFloat, short[] dataShort, byte[] dataByte) {//将光流数据转换成mavlink字节数组并存发送缓冲区
        if (dataFloat.length < 3 || dataByte.length < 2) return null;
        msg_optical_flow mavlinkMessage = new msg_optical_flow();
        mavlinkMessage.time_usec = dataLong[0];
        mavlinkMessage.flow_comp_m_x = dataFloat[0];//光流数据X
        mavlinkMessage.flow_comp_m_y = dataFloat[1];//光流数据Y
        mavlinkMessage.ground_distance = dataFloat[2];
        mavlinkMessage.flow_x = dataShort[0];
        mavlinkMessage.flow_y = dataShort[1];
        mavlinkMessage.sensor_id = dataByte[0];
        mavlinkMessage.quality = dataByte[1];

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组

//		dataProcess.addSendMavlinkList(sendbyte0);//传感器信息不放入缓冲区，单独发送

        if (showSendMavlink) LogMgr.i("MAVLinkDataProcess 发送  光流");
        return sendbyte0;
    }
    public static int FloatToInt(float fdata) {
        return Float.floatToIntBits(fdata);
    }

    public static byte[] FloatTobyte(float fdata) {// float转换为字节     高位在前低位在后, data数据，allNum转化成的字节个数，byteNum第几个字节
        // 把float转换为byte[]
        int fbit = FloatToInt(fdata);
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }
        return b;
    }

    public int putByte(byte dataD, byte[] data, int index) {//调用之前先将 index调到其下标
        data[index] = dataD;
        index++;
        return index;
    }

    public int putShort(short dataD, byte[] data, int index) {
        for (int i = 0; i < 2; i++) {
            data[index + i] = (byte) (dataD >> (8 * (1 - i)));
        }
        index += 2;
        return index;
    }

    public int putInt(int dataD, byte[] data, int index) {//向缓冲区添加int数据，是拆分成四个字节添加的
        for (int i = 0; i < 4; i++) {
            data[index + i] = (byte) (dataD >> (8 * (3 - i)));
        }
        index += 4;
        return index;
    }

    public int putLong(long dataD, byte[] data, int index) {//高位在前低位在后
        for (int i = 0; i < 8; i++) {
            data[index + i] = (byte) (dataD >> (8 * (7 - i)));
        }
        index += 8;
        return index;
    }

    public int putFloat(float dataD, byte[] data, int index) {
        int dataD0 = Float.floatToIntBits(dataD);
        return putInt(dataD0, data, index);
    }
}
