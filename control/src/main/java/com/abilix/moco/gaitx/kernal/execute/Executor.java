package com.abilix.moco.gaitx.kernal.execute;


import android.util.Log;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class Executor {

    private boolean balanceB;
    private boolean bExecutable;

    private int[] m_mtval;
    private int[] m_tmpmtval;
    private int[] mAngles;
    private int[] mtal_last;
    private int motionType;
    private int gcount;
    private int gcountn;
    private int timeDelay;

    private long timeElapse;

    private double m_time;
    private double m_time2;
    private double m_timeAll;
    private double gyro_x;
    private double gyro_y;
    private double gyro_z;
    private double accel_x;
    private double accel_y;
    private double accel_z;
    private double pose_roll;
    private double pose_pitch;
    private double pose_yaw;
    private double m_step;
    private double mDistance;
    private double mAngle;
    private double[] m_agls;
    private double[] m_tmpagls;

    private Timer mTimer;
    private TimerTask mTimerTask;
    public MotionManager motionManager;
    private OnAngleUpdateListener mOnAngleUpdateListener;

    /**
     *
     */
    private Executor() {
        motionManager = new MotionManager();
        // initialize
        init();
        reset();
        motionManager.setParam(WALK_PARAM.SPEEDLIMITVAL.getIndex(), 20);
    }

    private static Executor instance;

    public static Executor getInstance(){
        if(instance == null){
            synchronized (Executor.class){
                if (instance == null){
                    instance = new Executor();
                }
            }
        }
        return instance;
    }

    /**
     *
     */
    public void init() {
        this.mAngles = new int[18];
        this.mtal_last = new int[18];
        this.gcount = 0;
        this.gcountn = 1;
        this.timeDelay = 100;
        this.timeElapse = 5;
        this.m_time = 0.01;
        this.m_time2 = 0.01;
        this.motionType = Robot_Motion.WALK.getIndex();
        this.m_timeAll = motionManager.getTimeAll();
        this.m_step = 0.01;
        this.m_agls = new double[18];
        this.m_mtval = new int[18];
        this.m_tmpagls = new double[18];
        this.m_tmpmtval = new int[18];
        this.bExecutable = false;
        this.mDistance = 0;
        this.mAngle = 0;
        setBalanceB(true);
        this.gcountn = 1;
        this.timeElapse = 5;
    }

    /**
     * @return
     */
    public int[] stepForward() {
        if(motionType != Robot_Motion.WALK.getIndex()){
            motionType = Robot_Motion.WALK.getIndex();
        }
        if (mDistance > 1.0) {
            m_agls = motionManager.walk(m_time2, motionType);
            m_time2 = m_time2 + m_step;
        } else if (mDistance <= 1.0) {
            m_agls = motionManager.walk(m_time2, motionType);
            m_time2 = 0.01;
        }
        m_time = m_time + m_step;

        for (int i = 0; i < 18; ++i) {
            m_mtval[i] = (int) (m_agls[i]);
        }
        return m_mtval;
    }

    /**
     * @param t
     * @return
     */
    public int[] getForward(double t) {
        m_tmpagls = motionManager.walk(t, motionType);
        for (int i = 0; i < 18; ++i) {
            m_tmpmtval[i] = (int) (m_tmpagls[i]);
        }
        return m_tmpmtval;
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public void updateGyro(double x, double y, double z) {
        this.gyro_x = x;
        this.gyro_y = y;
        this.gyro_z = z;
        motionManager.setParam(WALK_PARAM.GYROX.getIndex(), x);
        motionManager.setParam(WALK_PARAM.GYROY.getIndex(), y);
        motionManager.setParam(WALK_PARAM.GYROZ.getIndex(), z);
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public void updateAccele(double x, double y, double z) {
        this.accel_x = x;
        this.accel_y = y;
        this.accel_z = z;
        motionManager.setParam(WALK_PARAM.ACCX.getIndex(), x);
        motionManager.setParam(WALK_PARAM.ACCY.getIndex(), y);
        motionManager.setParam(WALK_PARAM.ACCZ.getIndex(), z);
    }

    /**
     * @param roll
     * @param pitch
     * @param yaw
     */
    public void updateAngle(double roll, double pitch, double yaw) {
        this.pose_roll = roll;
        this.pose_pitch = pitch;
        this.pose_yaw = yaw;
//        LogMgr.e("==============>"+pose_pitch);
        motionManager.setParam(WALK_PARAM.ROLL.getIndex(), pose_roll);
        motionManager.setParam(WALK_PARAM.PITCH.getIndex(), pose_pitch);
        motionManager.setParam(WALK_PARAM.YAW.getIndex(), pose_yaw);
    }

    /**
     * @param angle
     * @param distance
     */
    public void updateAD(double angle, double distance) {
//        LogMgr.i("updateAD angle = "+angle+" distance = "+distance);
//        if(    (angle==0&&distance==0)    ){
//
//        }
        this.mAngle = angle;
        this.mDistance = distance;
        motionManager.setParam(WALK_PARAM.ANGLE.getIndex(), angle);
        motionManager.setParam(WALK_PARAM.DISTANCE.getIndex(), distance);
    }

//    long time1;
    /**
     *
     */
    public synchronized void start() {
        if(mTimer!=null){
            return;
        }
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
//                LogMgr.i("得到一次步态数据 与上次间隔 = " + (System.currentTimeMillis() - time1) + " gcountn = "+gcountn+" gcount = "+gcount+" isbExecutable() = "+isbExecutable());
//                time1 = System.currentTimeMillis();
                if (gcountn < gcount) {
                    if (isbExecutable()) {
                        mAngles = stepForward();
                        mtal_last = mAngles.clone();
                    }
                    gcount = 0;
                } else {
                    mAngles = mtal_last.clone();
                }
//                LogMgr.d("mAngles = "+ Utils.intsToString(mAngles));
                mOnAngleUpdateListener.angleUpdate(mAngles);

                gcount++;
//                LogMgr.d("一次时间消耗 = "+ (System.currentTimeMillis() - time1));
            }
        };
        mTimer.schedule(mTimerTask, timeDelay, timeElapse);
    }

    /**
     * 停止Timer
     */
    public synchronized void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    /**
     * 是否打开平衡
     *
     * @param value
     */
    public void setBalanceB(boolean value) {
        balanceB = value;
        motionManager.setParam(WALK_PARAM.BALANCEB.getIndex(), value);
    }

    /**
     *
     */
    public void reset() {
        mtal_last = getForward(m_step);
    }

    /**
     * @return
     */
    public boolean isbExecutable() {
        return bExecutable;
    }

    /**
     * @param bExecutable
     */
    public void setbExecutable(boolean bExecutable) {
        this.bExecutable = bExecutable;
    }

    /**
     * @return
     */
    public double getGyro_x() {
        return gyro_x;
    }

    /**
     * @return
     */
    public double getGyro_y() {
        return gyro_y;
    }

    /**
     * @return
     */
    public double getGyro_z() {
        return gyro_z;
    }

    /**
     * @return
     */
    public double getAccel_x() {
        return accel_x;
    }

    /**
     * @return
     */
    public double getAccel_y() {
        return accel_y;
    }

    /**
     * @return
     */
    public double getAccel_z() {
        return accel_z;
    }

    /**
     * @return
     */
    public double getPose_roll() {
        return pose_roll;
    }

    /**
     * @return
     */
    public double getPose_pitch() {
        return pose_pitch;
    }

    /**
     * @return
     */
    public double getPose_yaw() {
        return pose_yaw;
    }

    /**
     * @param motionType
     */
    public void setMotionType(int motionType) {
        this.motionType = motionType;
    }

    /**
     * @return
     */
    public double getM_time() {
        return m_time;
    }

    /**
     * @return
     */
    public double getM_step() {
        return m_step;
    }

    /**
     * @param m_step
     */
    public void setM_step(double m_step) {
        this.m_step = m_step;
    }

    /**
     * @param listener 回调接口
     */
    public void setOnAngleUpdateListener(OnAngleUpdateListener listener) {
        mOnAngleUpdateListener = listener;
    }

    /**
     * 关节角度更新监听
     */
    public interface OnAngleUpdateListener {
        /**
         * @param angles 角度 int[]
         */
        void angleUpdate(int[] angles);
    }


//    public byte[] modAngle(byte angles[]) {
//        int i = 2;
//        double Angle[] = new double[18];
//        int AngleCount = 0;
//        while (1 < i && i < 38) {
//            for (int j = 0; j < 18; j++) {
//                //低位在前，高位在后
//                int temp1;
//                temp1 = angles[i];
//                temp1 &= 0xFF;
//                temp1 |= ((long) angles[i + 1] << 8);
//                temp1 &= 0xffff;
//                Angle[AngleCount] = temp1;
//                AngleCount++;
//                i += 2;
//            }
//        }
//        motionManager.setAngle(Angle, 18);
//        motionType = Robot_Motion.READBIN.getIndex();
//        m_agls = motionManager.walk(m_time, motionType);
//        for (int k = 0; k < 18; ++k) {
//            m_mtval[k] = (int) (m_agls[k]);
//        }
////
//        int data[] = new int[]{m_mtval[11], m_mtval[2], m_mtval[10], m_mtval[1], m_mtval[12], m_mtval[3], m_mtval[13], m_mtval[4], m_mtval[14], m_mtval[5], m_mtval[9], m_mtval[0], m_mtval[15], m_mtval[6], m_mtval[16], m_mtval[7], m_mtval[17], m_mtval[8]};
//        int[] data2 = new int[19];
//        for (int k = 0; k < data2.length; k++) {
//            if (k == 0) {
//                data2[k] = 0;
//            } else if (k > 0 && k <= 18) {
//                data2[k] = data[k - 1];
//            }
//        }
//        byte[] data3 = new byte[46];
//        for (int k = 0; k < data2.length; k++) {
//            data3[k * 2] = (byte) (data2[k] & 0xFF);
//            data3[k * 2 + 1] = (byte) ((data2[k] >> 8) & 0xFF);
//        }
//        for (int p = 38;p < 46;p ++) {
//            data3[p] = angles[p];
//        }
//        return data3;
//    }

    public byte[] modAngle(byte angles[]) {
        int i = 2;
        double Angle[] = new double[18];
        int AngleCount = 0;
        while (1 < i && i < 38) {
            for (int j = 0; j < 18; j++) {
                //低位在前，高位在后
                int temp1;
                temp1 = angles[i];
                temp1 &= 0xFF;
                temp1 |= ((long) angles[i + 1] << 8);
                temp1 &= 0xffff;
                Angle[AngleCount] = temp1;
                AngleCount++;
                i += 2;
            }
        }
        double[] angle = new double[18];
        angle[0] = Angle[ 11]; angle[1] = Angle[ 3]; angle[2] = Angle[ 1]; angle[3] = Angle[ 5];
        angle[4] = Angle[ 7]; angle[5] = Angle[ 9]; angle[6] = Angle[ 13]; angle[7] = Angle[ 15];
        angle[8] = Angle[ 17]; angle[9] = Angle[ 10]; angle[10] = Angle[ 2]; angle[11] = Angle[ 0];
        angle[12] = Angle[ 4]; angle[13] = Angle[ 6]; angle[14] = Angle[ 8]; angle[15] = Angle[12];
        angle[16] = Angle[ 14]; angle[17] = Angle[ 16];

        motionManager.setAngle(angle, 18);
        motionType = Robot_Motion.READBIN.getIndex();
        m_agls = motionManager.walk(m_time, motionType);
        for (int k = 0; k < 18; ++k) {
            m_mtval[k] = (int) (m_agls[k]);
        }

        int data[] = new int[]{m_mtval[11], m_mtval[2], m_mtval[10], m_mtval[1], m_mtval[12], m_mtval[3], m_mtval[13], m_mtval[4], m_mtval[14], m_mtval[5], m_mtval[9], m_mtval[0], m_mtval[15], m_mtval[6], m_mtval[16], m_mtval[7], m_mtval[17], m_mtval[8]};
        int[] data2 = new int[19];
        for (int k = 0; k < data2.length; k++) {
            if (k == 0) {
                data2[k] = 0;
            } else if (k > 0 && k <= 18) {
                data2[k] = data[k - 1];
            }
        }
        byte[] data3 = new byte[46];
        for (int k = 0; k < data2.length; k++) {
            data3[k * 2] = (byte) (data2[k] & 0xFF);
            data3[k * 2 + 1] = (byte) ((data2[k] >> 8) & 0xFF);
        }
        for (int p = 38;p < 46;p ++) {
            data3[p] = angles[p];
        }
        return data3;
    }
}
