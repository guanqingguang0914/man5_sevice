package com.abilix.control.balancecar;


import android.os.Handler;
import android.os.Message;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlKernel;
import com.abilix.control.aidl.Brain;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;


public class BalanceCarData {
    public static BalanceCarData balancecarData = null;
    public static Handler handler = null;
    public static Handler handler1 = null;
    public Message sensorMsg = null;
    private static final Object sServiceSync = new Object();
    public static boolean startRun = false;
    public static MySensor mSensor;
    public static Message msg01 = null;
    public static int appNum = 0;
    public static boolean ifblance = true;

    public static int balanceState = 0;
    public static int stopTimes = 0;
    private float[] values;
    private static float[] angle = new float[3];
    /* P偏差比例，I积分，D微分 */
    private float P = 6.4f, I = 1.2f, D = 10, N = 0.98f; // 一轮
    // private float P = 10.2f, I = 1.3f, D = 15,N=0.98f; //两轮
    private static float last_angle = 0;
    private float ag = 0;
    private float sudu;
    // 这里要引入加速度而不能仅仅线性关系。这里要引入加速度。所以这里取平方值。
    private static boolean flag = false;
    private static boolean zero_flag = false;
    // 这里取三组数据。取方差最小的一组作为系统偏差。
    private float[] zero_array = new float[300];// 200个值是4秒钟。取后一百个值。
    private int zero_num = 0;

    private final static int ILen = 100;
    public int zeroTimes = 0;

    double iDelta = 0;
    double imu_1 = 0;
    static double iDeltaSum = 0;
    double iDeltaLast = 0;
    long iSpeed = 0;

    static int iCount = 0;
    int iSpeedCount = 0;
    long lSpeedSum = 0;

    float iOffset = 0;
    int iSpeedAvg;
    static int iSpeedArray[] = new int[ILen];
    float fSpeedAvgSum = 0;
    int iSpeedAvgLast;
    long lDutySum = 0;

    double f1, f2, f3;
    float a;
    private float runfast = 0;

    // private float PP = 12f, II = 0.08f, DD = 300.0f; //两轮
    // public float GAIN=1/100f;
    private float PP = 21f, II = 10f, DD = 90f; // 一轮
    public final float GAIN = -1 / 100f;
    public final float DTIME = 300f;
    private final double todegree = 180 / Math.PI;

    private float[] lastValueAB = new float[2];
    public byte[] readbuffer = new byte[28];
    public int readlength = 0;
    public static float[] runlongAB1 = new float[2];// 存储本次获取位移
    public static float[] runlongAB2 = new float[2];// 保存上一次位移
    public static float[] DrunlongAB = new float[2];// 和上一次位移差
    public static float[] DrunlongAB2 = new float[2];//
    public static float[] integral = new float[2];// 偏移积分
    public static float[] differential = new float[2];// 偏移微分
    public static long getABTime = System.currentTimeMillis();
    public static long DgetABTime = 0;
    public static long runTime = 0;

    // int count=0;
    // float valuess=0;
    // float values22=0;

    private final static int STM_VERSION_NUTTX_BASE = 0x02020028;

    private BalanceCarData() {
        if (Utils.getStmVersion() >= STM_VERSION_NUTTX_BASE) {
            //nuttx版本平衡系数调整
            N = N * 0.82f;
            LogMgr.e("BalanceCarData: N = " + N);
        }
    }

    public static BalanceCarData GetManger(Handler mHandler, int appNum0) {
        synchronized (sServiceSync) {
            if (balancecarData == null) {
                balancecarData = new BalanceCarData();
            }
            appNum = appNum0;
            switch (appNum0) {
                case 1://
                    handler = mHandler;
                    break;
                case 2:// vjc
                    handler1 = mHandler;
                    break;
                default:
                    handler = mHandler;
                    break;
            }
            if (mSensor == null) {
                mSensor = MySensor.obtainMySensor(ControlApplication.instance);
                mSensor.openSensorEventListener();
                mSensor.startBalanceCar = true;
                mSensor.zero_value = 0;
            }
        }
        return balancecarData;
    }

    public void InitBalanceCar(int param1, int param2) {// 进入平衡车
        switch (param1) {
            case 0:// 进入平衡车模式
                zero();
                break;
            case 1:// 退出平衡车模式
                getOut();
                break;
        }
    }

    public void SetBalanceCar(int param1, int param2, int param3) {// 控制平衡车
        switch (param1) {
            case 0:// 停止
                start();
                break;
            case 1:// 前进

                break;
            case 2:// 后退

                break;
            case 3:// 左转

                break;
            case 4:// 右转

                break;
        }
        runState(param1);
    }

    public static int bytesToInt4(byte[] bytes, int begin) {// 四字节转换为int
        // 低位在前高位在后
        return (int) ((int) 0xff & bytes[begin + 3]) | (((int) 0xff & bytes[begin + 2]) << 8) | (((int) 0xff & bytes[begin + 1]) << 16) | (((int) 0xff & bytes[begin]) << 24);
    }

    public static float bytesToFloat4(byte[] bytes, int begin) {// 四字节转换为float
        // 低位在前高位在后
        int floatInt = bytesToInt4(bytes, begin);// 先转换为int型，再转换为float
        return Float.intBitsToFloat(floatInt);
    }

    public static byte[] dealwheel_big_new(int value) {
        byte[] motor_send = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x0C, 0x01, (byte) 0xA3, 0x02, 0x00, 0x00, 0x00, 0x00, 0x64, 0x64, 0x64, 0x64, 0x00};
        motor_send[11] = (byte) (100 + value);
        motor_send[12] = (byte) (100 + value);
        for (int i = 0; i < 15; i++) {
            motor_send[15] = (byte) (motor_send[15] + motor_send[i]);
        }
        try {
           return SP.request(motor_send);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void dealwheel_init() {
        byte[] motor_send = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x08, 0x01, (byte) 0xA3, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00};
        for (int i = 0; i < 11; i++) {
            motor_send[11] = (byte) (motor_send[11] + motor_send[i]);
        }
        try {
            SP.write(motor_send);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPadBalance() {
        try {
            switch (appNum) {
                case 1://
                    byte[] senddate = new byte[]{(byte) 0xAA, 0x55, 0x00, 0x09, 0x0, (byte) 0xA1, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                    for (int i = 0; i < 13; i++) {
                        senddate[12] = (byte) (senddate[12] + senddate[i]);
                    }
                    sensorMsg = handler.obtainMessage();
                    sensorMsg.obj = senddate;
                    handler.sendMessage(sensorMsg);
                    break;
                case 2:// vjc
                    /*msg01 = handler1.obtainMessage();
                    msg01.what = 5;
                    msg01.obj = "平衡车已经平衡";
                    handler1.sendMessage(msg01);*/
                    Brain balanceCar = new Brain(2, new byte[2]);
                    balanceCar.setModeState(9);
                    ControlKernel.getInstance().responseCmdToBrain(balanceCar);
                    ifblance = true;
                    LogMgr.d("11111balanceCar 0000000000 === ");
                    break;
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void zero() {// 归零
        try {
            switch (appNum) {
                case 1://
                    break;
                case 2:// vjc
                    resumeInit();// 初始化之前先给之前的退出
                    Thread.sleep(2);
                    if (!ifblance) {
                        startRun = false;
                        /*msg01 = handler1.obtainMessage();
                        msg01.what = 5;
                        msg01.obj = "平衡车已经平衡";
                        handler1.sendMessage(msg01);*/
                        Brain balanceCar = new Brain(2, new byte[2]);
                        balanceCar.setModeState(9);
                        ControlKernel.getInstance().responseCmdToBrain(balanceCar);
                    }
                    /*msg01 = handler1.obtainMessage();
                    msg01.what = 4;
                    msg01.obj = "平衡车正在初始化";
                    handler1.sendMessage(msg01);*/
                    Brain initCar = new Brain(2, new byte[2]);
                    initCar.setModeState(8);
                    ControlKernel.getInstance().responseCmdToBrain(initCar);
                    ifblance = false;
                    LogMgr.d("11111balanceCar 111111111 ==== ");
                    break;
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (!zero_flag) {
            mSensor.getBalanceDate = false;
            mSensor.dtadd = 0;
            zero_flag = true;
            dealwheel_big_new(0);
            dealwheel_init();
            if (!startRun) {
                startRun = true;
                new Thread(new balanceCar()).start();
            }
        } else if (!flag) {
            start();
        }
    }

    public void start() {// 开始平衡，进入平衡车模式
        angle[0] = 0;
        iCount = 0;
        iSpeedArray = new int[ILen];

        iDelta = 0;
        imu_1 = 0;
        iDeltaSum = 0;
        last_angle = 0;
        iDeltaLast = 0;
        iSpeed = 0;

        iCount = 0;
        iSpeedCount = 0;
        lSpeedSum = 0;

        iOffset = 0;
        iSpeedAvg = 0;
        fSpeedAvgSum = 0;
        iSpeedAvgLast = 0;
        lDutySum = 0;

        // count=0;

        lastValueAB = new float[2];
        readlength = 0;
        runlongAB1 = new float[2];// 存储本次获取位移
        runlongAB2 = new float[2];// 保存上一次位移
        DrunlongAB = new float[2];// 和上一次位移差
        getABTime = 0;
        DgetABTime = 0;
        getABTime = System.currentTimeMillis();
        balanceState = 0;
        dealwheel_init();
        mSensor.getBalanceDate = false;
        mSensor.dtadd = 0;
        flag = true;
    }

    public void runState(int state) {
        balanceState = state;
        dealwheel_init();
        integral[0] = 0;// 积分
        differential[0] = 0;// 微分
        // count=0;
        switch (state) {
            case 0:// 平衡
                runfast = 0;
                break;
            case 1:// 前进
                runfast = 0.001f;
                break;
            case 2:// 后退
                runfast = -0.0007f;
                break;
            case 3:// 左转
                break;
            case 4:// 右转
                break;
        }
    }

    public static void getOut() {// 退出
        dealwheel_big_new(0);
        iCount = 0;
        iDeltaSum = 0;
        last_angle = 0;
        iSpeedArray = new int[ILen];
        angle[0] = 0;
        // count=0;

        flag = false;
        balanceState = 0;
        destory();
        Brain stopBalance = new Brain(2, new byte[]{0x01, 0x00});
        stopBalance.setModeState(9);
        ControlKernel.getInstance().responseCmdToBrain(stopBalance);
    }

    public static void destory() {// 退出
        zero_flag = false;
        flag = false;
        startRun = false;
        balancecarData = null;
        mSensor = null;
    }

    public void resumeInit() {// 再次初始化
        startRun = false;
        zero_flag = false;
        flag = false;
        iCount = 0;
        iDeltaSum = 0;
        last_angle = 0;
        iSpeedArray = new int[ILen];
        angle[0] = 0;
        balanceState = 0;
    }

    class balanceCar implements Runnable {
        @Override
        public void run() {
            integral = new float[2];// 偏移积分
            differential = new float[2];// 偏移微分
            dealwheel_init();
            while (startRun) {
                try {
                    if (System.currentTimeMillis() - runTime >= 8 && mSensor.getBalanceDate) {
                        runTime = System.currentTimeMillis();
                        if (zero_flag || flag) {
                            angle[0] += mSensor.dtadd;
                            mSensor.getBalanceDate = false;
                            mSensor.dtadd = 0;
                        } else {
                            mSensor.dtadd = 0;
                        }
                        if (zero_flag) {
                            values = mSensor.getmG();
                            zero_array[zero_num % 300] = values[0];
                            zero_num++;
                            if (zero_num > 300) {
                                zero_num = 0;
                                zero_flag = false;
                                // 这里处理那个稳定偏差。
                                float zero_sum;
                                zero_sum = 0;
                                for (int j = 0; j < zero_array.length; j++) {
                                    zero_sum += zero_array[j];
                                }
                                mSensor.zero_value = zero_sum / zero_array.length;
                                angle[0] = 0;
                                dealwheel_init();
                                start();
                                sendPadBalance();
                            }
                        }

                        if (flag) {
                            ag = (float) (angle[0] * todegree);
                            ag += lastValueAB[0]; // 外层pid
                            f1 = (ag) * P;// P
                            // I
                            iDeltaSum += ag;
                            f2 = (double) (iDeltaSum * I);
                            // D
                            sudu = ag - last_angle;// D -- 误差
                            f3 = sudu * D;
                            last_angle = ag;
                            a = (float) ((f1 + f2 + f3));
                            if (a > 100) {
                                a = 100;
                                stopTimes++;
                            } else if (a < -100) {
                                a = -100;
                                stopTimes++;
                            } else {
                                stopTimes = 0;
                            }
                            if (stopTimes > 50) {
                                flag = false;
                                dealwheel_big_new(0);
                            }
                            // count++;
                            // if(count<1200)valuess=0;
                            // else valuess=(count-1200)/8;
                            //
                            // if(valuess==1)a+=10;
                            // else if(valuess>1 && (valuess)%2==0){
                            // if(a>0) a-=5;
                            // else a+=5;
                            // }
                            switch (balanceState) {
                                case 0:// 平衡
                                    break;
                                case 1:// 前进
                                    // if(a>0)a-=0.2;
                                    // else if(a<0)a+=0.2;
                                    break;
                                case 2:// 后退
                                    break;
                                case 3:// 左转
                                    break;
                                case 4:// 右转
                                    // if(a>0)a+=10;
                                    // else if(a<0)a-=10;
                                    break;
                            }
                            readbuffer=dealwheel_big_new((int) (a * N));
                            if (flag && readbuffer != null && readbuffer.length>=28) {
                                if (System.currentTimeMillis() - getABTime >= DTIME) {
                                    if (readbuffer[ 0] == (byte) 0xAA && readbuffer[1] == (byte) 0x55 && readbuffer[5] == (byte) 0xF0
                                            && readbuffer[6] == (byte) 0x24) {
                                        DgetABTime = System.currentTimeMillis() - getABTime;
                                        getABTime = System.currentTimeMillis();
                                        runlongAB1[0] = bytesToInt4(readbuffer, 11) * GAIN;
                                        runlongAB1[1] = bytesToInt4(readbuffer, 15) * GAIN;
                                        DrunlongAB[0] = runlongAB1[0] - runlongAB2[0];
                                        DrunlongAB[1] = runlongAB1[1] - runlongAB2[1];
                                        integral[0] += (DrunlongAB[0] / DTIME - runfast);// 积分
                                        differential[0] = (DrunlongAB[0] - DrunlongAB2[0]) / (DTIME * DTIME);// 微分
                                        runlongAB2[0] = runlongAB1[0];
                                        runlongAB2[1] = runlongAB1[1];
                                        DrunlongAB2[0] = DrunlongAB[0];
                                        DrunlongAB2[1] = DrunlongAB[1];

                                        switch (balanceState) {
                                            case 0:// 平衡
                                                lastValueAB[0] = PP * (DrunlongAB[0] / DTIME - runfast) + II * (integral[0]) + DD * (differential[0]);
                                                break;
                                            case 1:// 前进
                                                lastValueAB[0] = PP * (DrunlongAB[0] / DTIME - runfast) + II * (integral[0]) + DD * (differential[0]);

                                                // count++;
                                                // if(count/25==0){
                                                // valuess=0.5f;
                                                // values22=0.5f;
                                                // lastValueAB[0]=PP*(DrunlongAB[0]/DTIME-values22)+II*(runlongAB1[0]-valuess)+DD*((DrunlongAB[0]-DrunlongAB2[0])/(DTIME*DTIME));
                                                // }else{
                                                // lastValueAB[0]=100*lastValueAB[0]/100;
                                                // }
                                                break;
                                            case 2:// 后退
                                                break;
                                            case 3:// 左转
                                                break;
                                            case 4:// 右转
                                                break;
                                        }
                                    }
                                }
                            }
                            zeroTimes = 0;
                        } else if (zeroTimes < 2) {
                            zeroTimes++;
                            dealwheel_big_new(0);
                        }
                    } else {
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
