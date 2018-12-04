package com.abilix.moco.gaitx.kernal.execute;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.GaitAlgorithm;

import java.util.Arrays;

/**
 * Created by yangz on 2018/1/4.
 */

public class GaitAlgorithmForH5 {

    private static byte iCount;
    private static byte[] pID;
    private static int mServoCount = 23;

    private static GaitAlgorithmForH5 instance;
    private GaitAlgorithmForH5(){
    }

    public static GaitAlgorithmForH5 getInstance(){
        if(instance == null){
            synchronized (GaitAlgorithmForH5.class){
                if (instance == null){
                    instance = new GaitAlgorithmForH5();
                    iCount = (byte) mServoCount; // 23
                    pID = new byte[mServoCount];
                    for (int n = 0; n < mServoCount; n++) {
                        pID[n] = (byte) (n); // 0~22
                    }
                    Executor.getInstance().setOnAngleUpdateListener(new Executor.OnAngleUpdateListener() {
                        @Override
                        public void angleUpdate(int[] angles) {
                            SrvSetPosAll(angles);
                        }
                    });
                }
            }
        }
        return instance;
    }

    /**
     * 停止步态
     */
    public void stopGait(){
        SensorImuServiceForH5.stopSensorImuServiceForH5();
        Executor.getInstance().stop();
    }

    /**
     * 停止动作
     */
    public void stopMove(){
        GaitAlgorithmForH5.getInstance().move(0 ,0);
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        stopGait();
//        Executor.getInstance().setbExecutable(false);
    }

    /**
     * 复位
     */
    public void reset(){
        Executor.getInstance().reset();
    }

    /**
     * 移动
     */
    public void move(double angle, double stepLength){
        init();
//        if (data.matches("^[-+]?\\d*\\.\\d*,[-+]?\\d*\\.\\d*")) {
//            String[] recvDA = data.split(",");
//
//        }
        Executor.getInstance().updateAD(angle, stepLength);
    }

    /**
     * 设置前进
     */
    public void setForward(){
        Executor.getInstance().motionManager.setParam(WALK_PARAM.FORWARD.getIndex(), true);
        Executor.getInstance().motionManager.setParam(WALK_PARAM.BACKWARD.getIndex(), false);
    }

    /**
     * 设置后退
     */
    public void setBackward(){
        Executor.getInstance().motionManager.setParam(WALK_PARAM.FORWARD.getIndex(), false);
        Executor.getInstance().motionManager.setParam(WALK_PARAM.BACKWARD.getIndex(), true);
    }

    /**
     * 设置速度限制
     * @param speedLimit
     */
    public void setSpeedLimit(int speedLimit){
        Executor.getInstance().motionManager.setParam(WALK_PARAM.SPEEDLIMITVAL.getIndex(), speedLimit);
    }

    /**
     * 走转跑
     * @param data
     */
    public void walk2Run(int data){
        Executor.getInstance().motionManager.setParam(WALK_PARAM.WALK2RUNVAL.getIndex(), data);
    }

    byte[] newAngles;
    byte[] newResult;
    public byte[] adjustMove(byte[] angles){
        SensorImuServiceForH5.startSensorImuServiceForH5();
        boolean isNewVersionBinMoveFile = false;
        LogMgr.i("修正前角度angles = "+ Utils.bytesToString(angles));
        newAngles = angles;
        if(angles.length == 44){
            isNewVersionBinMoveFile = true;
            newAngles = new byte[46];
            System.arraycopy(angles,0,newAngles,2,angles.length);
        }else if(angles.length == 46){

        }else{
            LogMgr.e("参数个数不正确");
            return angles;
        }
        byte[] result = Executor.getInstance().modAngle(newAngles);
        newResult = result;
        if(isNewVersionBinMoveFile){
            newResult = new byte[44];
            System.arraycopy(result,2,newResult,0,newResult.length);
        }
        LogMgr.i("修正后角度angles = "+ Utils.bytesToString(newResult));
//        if(angles.equals()){
//
//        }
//        return  angles;
        return newResult;
    }

    private void init(){
        LogMgr.i("init()");
        SensorImuServiceForH5.startSensorImuServiceForH5();
        Executor.getInstance().start();
        Executor.getInstance().setbExecutable(true);
    }

    private static void SrvSetPosAll(int[] angles){

        int data[] = new int[]{angles[11], angles[2], angles[10], angles[1], angles[12], angles[3], angles[13], angles[4], angles[14], angles[5], angles[9], angles[0], angles[15], angles[6], angles[16], angles[7], angles[17], angles[8], 512, 512};
        int[] data2 = new int[23];
        for (int i = 0; i < data2.length; i++) {
            if (i == 0) {
                data2[i] = 0;
            } else if (i > 0 && i <= 18) {
                data2[i] = data[i - 1];
            } else if (i == 19 || i == 20) {
                data2[i] = 512;
            } else if (i == 21 || i == 22) {
                data2[i] = data[i - 3];
            }
        }
        byte[] data3 = new byte[46];
        for (int i = 0; i < data2.length; i++) {
            data3[i * 2] = (byte) (data2[i] & 0xFF);
            data3[i * 2 + 1] = (byte) ((data2[i] >> 8) & 0xFF);
        }
        try {
            GaitAlgorithm.Servo_SetPosAll(iCount, pID, data3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
