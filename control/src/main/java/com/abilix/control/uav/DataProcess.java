package com.abilix.control.uav;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import com.abilix.control.ControlInfo;
import com.abilix.control.patch.STM32Mgr;
import com.abilix.control.sp.PushMsg;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class DataProcess {
    public static boolean[] isShowLog=new boolean[]{false,false};//是否显示Log，收发的数据显示默认不打开，调试时打开
    public static int robotType=ControlInfo.getChild_robot_type();
    private static boolean ReadDataThread = false;//是否已开启线程
    public static short[] auvAFRemote = new short[]{1500, 1500, 1100, 1500, 1100};
    public static long heartbeatTime;
    public static long mheartbeatTime;
    public static long lastTime = 0;
    public static ByteArrayOutputStream dataBuf = new ByteArrayOutputStream();
    public static Handler mHandler = null;
    public abstract void ParseSerialData(byte[] data);
    private PushMsg mPushmsg=new PushMsg(){
        @Override
        public void onPush(byte[] pushData) {
            ParseSerialData(pushData);
        }
    };
    public DataProcess(){
        if (!ReadDataThread) {
            ReadDataThread = true;
            new Thread(new ReadDataRunnable()).start();
            SP.registerPushEvent(mPushmsg);//
        }
    }
    public void initHandler(Handler handler) {
        mHandler = handler;
    }

    public abstract void specificParseData(byte[] data);

    public void ParseData(byte[] data) {//解析udp指令
        int dataLen = 0;
        if (data != null) dataLen = data.length;
		if(isShowLog[0])LogMgr.i("ParseDataUDP收到数据 "+showDataHex(data));
        if (dataLen < 12 || DataTransform.bytesToInt2h(data, 2) != (dataLen - 4)) {//长度不对
            LogMgr.e("ParseDataUDP收到数据长度不对");
            return;
        }
        if (DataTransform.XORcheckAdd(data)) {//校验位通过
            switch ((data[4] & 0xff)) {//校验机器人类型
                case 0x04:// F5/AF1

                case 0x06:// AF2

                case 0x2D:// F3

                case 0x2E:// F4
                    specificParseData(data);
                    break;
            }
        } else LogMgr.e("ParseDataUDP收到的数据校验不通过");
    }

    public synchronized byte[] packageData(int cmd1, int cmd2, byte[] data) {//将数据封装成完整报文
        int len = 0;
        if (data == null) len = 8;
        else len = data.length + 8;
        byte[] send = new byte[len + 4];
        send[0] = (byte) 0xaa;// 报头
        send[1] = (byte) 0x55;// 报头
        send[2] = (byte) ((len & 0xff00) >> 8);//帧长
        send[3] = (byte) (len & 0xff);//帧长
        send[4] = (byte) robotType;// 机器人类型
        send[5] = (byte) cmd1;// 命令字1
        send[6] = (byte) cmd2;// 命令字2
        if (len > 8) System.arraycopy(data, 0, send, 11, len - 8);
        int crc = DataTransform.XORcheckSend(send);//校验
        send[len + 3] = (byte) (crc & 0xff);// 校验
        return send;
    }

    public synchronized void sendData(int cmd1, int cmd2, byte[] data) {// 数据发送命令
        byte[] send = packageData(cmd1, cmd2, data);
        sendData(send);
    }

    public synchronized void sendData(byte[] data) {
        if (data == null || mHandler == null) {
            LogMgr.e("sendData 发送数据出错了" + (data == null) + "  " + (mHandler == null));
            return;
        }
        if ((data[0] & 0xff) == 0xaa && (data[1] & 0xff) == 0x55 && ((data.length - 4) == DataTransform.bytesToInt2h(data, 2)) && DataTransform.XORcheckAdd(data)) {//&& DataTransform.XORcheckAdd(data)
            Message sensorMsg = mHandler.obtainMessage();
            sensorMsg.what = 0;
            sensorMsg.obj = data;
            mHandler.sendMessage(sensorMsg);
            if(isShowLog[0])LogMgr.i("sendData 发送数据了"+(data.length)+"  数据  "+showDataHex(data));
        } else LogMgr.e("sendData 数据校验不通过不对    长度" + (data.length) + "  数据:  " + showDataHex(data));

    }

    //读串口和发pad信息一个线程，解析pad数据和发串口一个线程
    public void sendSerialData(byte[] data) {//发送串口mavlink
        LogMgr.i("sendSerialData 发送串口mavlink");
        if (data != null) SP.write(data);
        else LogMgr.i("sendSerialData 发送数据为null");
    }

    public void sendSerialMotorData(byte[] data) {//发送串口2
        if (data != null) SP.writeVice(data);
    }

//    public byte[] getSerialDataArr() {//接收串口mavlink
//        int datalen = SP.getAvailable();
////		if(datalen>0)LogMgr.i("getMavlink 收到串口数据长度  "+datalen);
//        if (datalen >= 8) {//mavlink最短8字节
//            byte[] data = new byte[datalen];
//            SP.read(data, 0);
////			LogMgr.i("getMavlink收到数据       "+(data[5] & 0xff)+"     "+showDataHex(data));
//            if(datalen<200)return data;
//            else return null;
//        } else return null;
//    }
//
//    public void getSerialData() {//接收串口数据
//        int datalen = SP.getAvailable();
////		if(datalen>0)LogMgr.i("getSerialData 收到串口数据长度  "+datalen);
//        if (datalen > 23) {//AF最短23字节
//            byte[] data = new byte[datalen];
//            SP.read(data, 0);
////			LogMgr.i("getSerialData收到数据       "+(data[5] & 0xff)+"     "+showDataHex(data));
//            DataBuffer.writeData(data);//写入缓冲区
//        }
//    }
//    public void getSerialMotorData() {//接收电源板串口数据
//        int datalen = SP.getMotorAvailable();
//		if(datalen>0)LogMgr.i("getSerialMotorData 收到串口数据长度  "+datalen);
//        if (datalen >= 12) {
//            byte[] data = new byte[datalen];
//            SP.readMotor(data, 0);
//			LogMgr.i("getSerialMotorData收到数据       "+(data[5] & 0xff)+"     "+showDataHex(data));
////            DataBuffer.writeData(data);
//            sendData(data);//直接发送出去
//        }
//    }

    public synchronized String showDataHex(byte[] data) {//将接收或者发送的数据转换为16进制String
        String str0 = "";
        int v;
        String hv = "";
        for (int i = 0; i < data.length; i++) {
            v = data[i] & 0xFF;
            if (v <= 0x0f) hv = hv + " 0" + Integer.toHexString(v);
            else hv = hv + " " + Integer.toHexString(v);
        }
        return str0 + hv + " \n ";
    }
    public abstract void readDataRun(byte[] data);
    public class ReadDataRunnable implements Runnable {
        private byte[] mavData;
        @Override
        public void run() {
            while (ReadDataThread) {
                try {
                    readDataRun(mavData);
                } catch (Exception e) {
                    LogMgr.e("ReadDataRunnable线程出错  " + e);
                }
            }
            ReadDataThread = false;
        }
    }
}
