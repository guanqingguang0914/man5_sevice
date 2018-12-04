package com.abilix.robot.walktunner;

import android.content.Intent;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yangq on 2017/8/28.
 */

public class GaitAlgorithm implements Hwalk{
    private static GaitAlgorithm instance=null;
    private static int FORWARD_SPEED = 10;
    private static int BACKWARD_SPEED = 10;
    private static int LEFT_SPEED = 10;
    private static int RIGHT_SPEED = 10;

    private static int WalkType = 0;

    private static byte iCount;
    private static byte[] pID;
    private static int mServoCount = 23;
    private static long stopWalkTime = 0;
    public static boolean isStopWalk = false;
    public static boolean isDestoryWalk = true;

    public static Walk walk = Walk.getSingleton();
    private static Timer mTimer=null;
    private static TimerTask mTimerTask=null;
    public static GaitAlgorithm getInstance(){
        walk = Walk.getSingleton();
        if(instance==null){
            instance=new GaitAlgorithm();
            SensorImuService.startSensorImuService();
        }
        return instance;
    }
    @Override
    public void startForwardWalk(){//前进
        init();
        walk.startForwardWalk(FORWARD_SPEED);
        WalkType = 0x01;
    }

    @Override
    public void startBackwardWalk(){//后退
        init();
        walk.startBackwardWalk(BACKWARD_SPEED);
        WalkType = 0x02;
    }
    @Override
    public void startLeftWalk(){//左走
        init();
        walk.startLeftWalk(LEFT_SPEED);
        WalkType = 0x03;
    }
    @Override
    public void startLeftForwardWalk(){//左前走
        init();
        walk.startLeftForwardWalk(LEFT_SPEED, FORWARD_SPEED);
        WalkType = 0x05;
    }
    @Override
    public void startLeftBackwardWalk(){//左后走
        init();
        walk.startLeftBackwardWalk(LEFT_SPEED, BACKWARD_SPEED);
        WalkType = 0x07;
    }
    @Override
    public void startRightWalk(){//右走
        init();
        walk.startRightWalk(RIGHT_SPEED);
        WalkType = 0x04;
    }
    @Override
    public void startRightForwardWalk(){//右前走
        init();
        walk.startRightForwardWalk(RIGHT_SPEED, FORWARD_SPEED);
        WalkType = 0x06;
    }
    @Override
    public void startRightBackwardWalk(){//右后走
        init();
        walk.startRightBackwardWalk(RIGHT_SPEED, BACKWARD_SPEED);
        WalkType = 0x08;
    }
    @Override
    public void startTurnLeftWalk(){//左转
        init();
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
            walk.startTurnWalk(0, 20, 1);
        } else {
            walk.startTurnWalk(FORWARD_SPEED, 1);
        }
        WalkType = 0x0A;
    }
    @Override
    public void startTurnRightWalk(){//右转
        init();
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
            walk.startTurnWalk(0, 20, 2);
        } else {
            walk.startTurnWalk(FORWARD_SPEED, 2);
        }
        WalkType = 0x0B;
    }
    @Override
    public void setWalkSpeed(int speed[]){//右后走
        FORWARD_SPEED = speed[0];
        BACKWARD_SPEED = speed[1];
        LEFT_SPEED = speed[2];
        RIGHT_SPEED = speed[3];
    }
    @Override
    public void stopWalk(){//停止
        stopWalkTime=System.currentTimeMillis();
        walk.stopwalk();
        isStopWalk = true;
        WalkType = 0x0;
    }
    @Override
    public void destoryWalk(){//停止步态算法
        isStopWalk = true;
        isDestoryWalk = true;
        LogMgr.e("主动停止步态服务");
        instance.destroy();
        WalkType = 0x0;
    }
    private void init(){//初始化timer和server
        isStopWalk = false;
        stopWalkTime=System.currentTimeMillis();
        try{
            if(instance==null)instance=new GaitAlgorithm();
            walk = Walk.getSingleton();
            if(mTimer==null){
                iCount = (byte) mServoCount; // 23
                pID = new byte[mServoCount];
                for (int n = 0; n < mServoCount; n++) {
                    pID[n] = (byte) (n); // 0~22
                }
                startTimer();
            }else isDestoryWalk = false;
        }catch(Exception e){
            LogMgr.i("GaitAlgorithm init 出错了 "+e);
        }
    }
    private void destroy(){
        try{
            stopTimer();
            SensorImuService.stopSensorImuService();
        }catch(Exception e){
            LogMgr.i("GaitAlgorithm destroy 出错  "+e);
        }
        walk=null;
        instance=null;
    }

    private void startTimer() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if(isStopWalk){//停止
                    if(System.currentTimeMillis()-stopWalkTime>1000*60){
                        LogMgr.e("一分钟后自动停止服务");
                        destroy();//停止一分钟后还没有销毁，自动销毁timer
                    }else  if(System.currentTimeMillis()-stopWalkTime<1000*1){
                        packetData();
                    }
                }else{
                    packetData();
                }
            }
        };
        mTimer.schedule(mTimerTask, 20, 16);
    }
    private void packetData(){
        int[] data = walk.getMotorValue();
        int[] data2 = new int[23];
        for (int i = 0; i < data2.length; i++) {
            if (i == 0) {
                data2[i] = 0;
            } else if (i > 0 && i <= 18) {
                data2[i] = data[i - 1];
            } else if (i == 19 || i == 20) {
                data2[i] = 512;
            } else if (i == 21 || i == 22) {
                data2[i] = 512;
            }
        }
        byte[] data3 = new byte[46];
        for (int i = 0; i < data2.length; i++) {
            data3[i * 2] = (byte) (data2[i] & 0xFF);
            data3[i * 2 + 1] = (byte) ((data2[i] >> 8) & 0xFF);
        }
//                LogMgr.i( "下发数据 = " + Arrays.toString(data));
//                LogMgr.e( "下发数据 = " + Utils.bytesToString(data3,data3.length));
        try {
            Servo_SetPosAll(iCount, pID, data3);
        } catch (Exception e) {
            LogMgr.e( "下发数据出错了= " + e);
        }
        //                switch(WalkType){
//                            case 0x01:
//                                startForwardWalk();
//                                break;
//                            case 0x02:
//                                startBackwardWalk();
//                                break;
//                            case 0x03:
//                                startLeftWalk();
//                                break;
//                            case 0x04:
//                                startRightWalk();
//                                break;
//                            case 0x05:
//                                startLeftForwardWalk();
//                                break;
//                            case 0x06:
//                                startRightForwardWalk();
//                                break;
//                            case 0x07:
//                                startLeftBackwardWalk();
//                                break;
//                            case 0x08:
//                                startRightBackwardWalk();
//                                break;
//                        }
    }
    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
        }
        mTimer=null;
        mTimerTask=null;
    }

    /**
     * 往串口发送一次数据
     *
     * @param iCount
     * @param pID
     * @param pPos
     */
    public static void Servo_SetPosAll(byte iCount, byte[] pID, byte[] pPos) {
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
        gSendBuff[iLength - 1] = bChecksum;
        bChecksum = (byte) (bChecksum & 0xFF);
        DoWriteFrame(gSendBuff, iLength);
    }

    public void Servo_SetPosSpeedAll(byte iCount, byte[] pID, byte[] pPos, int speed) {
        // FF FF FE 07 83 1E 02 id FF 03 D4
        // byte[] Buffer = new byte[16];
        int iLength = 0;
        byte bChecksum = 0;
        int i = 0;

        if (iCount < 1 || iCount > 30)
            return;
        iLength = 8 + iCount * 5;
        byte[] gSendBuff = new byte[iLength];

        gSendBuff[0] = (byte) 0xFF;
        gSendBuff[1] = (byte) 0xFF;
        gSendBuff[2] = (byte) 0xFE;
        gSendBuff[3] = (byte) (4 + iCount * 5);
        gSendBuff[4] = (byte) 0x83;
        gSendBuff[5] = (byte) 0x1E;
        gSendBuff[6] = 0x04;

        for (i = 0; i < iCount; i++) // 22次
        {
            if (pID[i] < 254) {
                gSendBuff[7 + i * 5] = pID[i];
                // gSendBuff[7+i*3+2] = (byte)((pPos[i]>>8) & 0x00FF);
                // gSendBuff[7+i*3+1] = (byte)((pPos[i] & 0x00FF));

                gSendBuff[7 + i * 5 + 1] = (byte) (pPos[i * 2]);
                gSendBuff[7 + i * 5 + 2] = (byte) (pPos[i * 2 + 1]);
                gSendBuff[7 + i * 5 + 3] = (byte) (speed & 0xFF); // (pPos[i*2]);
                gSendBuff[7 + i * 5 + 4] = (byte) 0x00; // (pPos[i*2+1]);
            } else {
                gSendBuff[7 + i * 5] = 0x00;
                gSendBuff[7 + i * 5 + 2] = 0x00;
                gSendBuff[7 + i * 5 + 1] = 0x00;
                gSendBuff[7 + i * 5 + 3] = 0x00;
                gSendBuff[7 + i * 5 + 4] = 0x00;
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
        int iLength = dwLen + 3;
        //高位在前，低位在后
        byte[] sendBuff = new byte[iLength];
//        sendBuff[0] = (byte) 0xFE;
//        sendBuff[1] = (byte) 0x68;
//        sendBuff[2] = (byte) 'Z';
//        sendBuff[3] = 0x00;
//        sendBuff[4] = (byte) ((dwLen >> 8) & 0xFF);
//        sendBuff[5] = (byte) (dwLen & 0xFF);
//        sendBuff[iLength - 2] = (byte) 0xAA;
//        sendBuff[iLength - 1] = (byte) 0x16;
//        System.arraycopy(pBuf, 0, sendBuff, 6, dwLen);
        sendBuff[0] = (byte) 0x02;
        sendBuff[1] = (byte) ((dwLen >> 8) & 0xFF);
        sendBuff[2] = (byte) (dwLen & 0xFF);
        System.arraycopy(pBuf, 0, sendBuff, 3, dwLen);
        byte[] gSendBuffer = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H,(byte) 0x11,(byte) 0x15,sendBuff);
        try {
            SP.write(gSendBuffer);
        } catch (Exception e) {
            LogMgr.e( "下发数据出错了= " + e);
        }
    }

    public static void Servo_SetSpeedAll(byte iCount, byte[] pID, byte[] pPos, int speed) {

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
}
