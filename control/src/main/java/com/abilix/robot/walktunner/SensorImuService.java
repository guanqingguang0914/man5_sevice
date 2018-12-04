package com.abilix.robot.walktunner;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.utils.LogMgr;
/**读取传感器器，判断当前机器人的站立，摔倒状态并发送相关广播*/
public class SensorImuService extends Service implements SensorEventListener {

    private final static String TAG = "SensorImuService";
    public static final String SENSOR_RESULT = "com.abilix.control.sensor_result";
    public static final String SENSOR_BACK = "SENSOR_BACK";
    public static final int SENSOR_DOWN = 1;
    public static final int SENSOR_UP = 2;
    public static final int FRONT_UP = 3;
    public static final int BACK_UP = 4;
    private Intent mIntent;

    //Walk
    public Walk walk;

    //Sensor
    private SensorManager sensorManager;
    private Sensor gyroSsensor;
    private Sensor accSensor;

    //==============================Kalman Filter==============================
    public static float[] accValues;
    public static float[] gyroValues;
    static float gyroX, gyroY, gyroZ;
    float accelX, accelY, accelZ;
    public static float Angle_Pitch, Gyro_Balance, Gyro_Turn, Angle_roll, Angle_yaw;
    float Acceleration_Z;
    double K1 = 0.02;
    float angleR, angle_dot;
    double Q_angle = 0.001;
    double Q_gyro = 0.003;
    double R_angle = 0.5;
    //double dt=0.010;
    double dt = 0.1;   //0.0025
    char C_0 = 1;
    float Q_bias, Angle_err;
    float PCt_0, PCt_1, E;
    float K_0, K_1, t_0, t_1;
    public float[] Pdot = {0, 0, 0, 0};
    public float[][] PP_new = {{1, 0}, {0, 1}};
    RollFilter mRollFilter;
    /**当前机器人状态，摔倒时为true，站立时为false*/
//    private boolean isRobotFall;
    /**倒下的初始时间*/
    private long timeInit;
    /**倒下后记录的时间，固定秒数后发送起身广播*/
    private long timeend;
    private long UPtimeInit;
    private long UPtimeend;
    //=================================Yaw=========================================
    private float timestamp = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float angle[] = new float[3];
    private static float zeroDrift = 0.011f;

    private enum INIT_STATUS {
        NOT_INIT,
        ON_INITING,
        ON_INITING2,
        INITED,
    }

    private INIT_STATUS initStatus = INIT_STATUS.NOT_INIT;
    private float zeroDriftTatle = 0.0f;
    private int zeroDriftCount = 0;

    private boolean beginYaw = false;
    private float beginYawTime = 0;
    private float beginYawValue = 0.0f;


    private static int ZERO_DRIFT_MAX_COUNT = 1000;   //计算零漂第一阶测试次数
    private int TestYawTime_s = 15;                   //计算零漂第二阶测试时长，单位：秒
    private float yawDiffPreMinute = 0.1f;            //除零漂后计算的yaw值，每分钟最大偏yawDiffPreMinute度
    private float preYawMaxDiff = TestYawTime_s * yawDiffPreMinute / 60.0f;
    private int ignoreCountWhenCalcuZeroDrift = 100;
    //==============================================================================================================================

    /**平衡服务是否启动*/
    private static boolean instanceb = false;

    public static void startSensorImuService() {
        if (!instanceb) {
            instanceb = true;
            LogMgr.e("startSensorImuService 启动步态服务");
            ControlApplication.instance.startService(new Intent(ControlApplication.instance, SensorImuService.class));
        }
    }

    public static void stopSensorImuService() {
        LogMgr.e("stopSensorImuService 步态服务停止");
        if (instanceb) {
            ControlApplication.instance.stopService(new Intent(ControlApplication.instance, SensorImuService.class));
            instanceb = false;
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        walk = Walk.getSingleton();
        onInit();
        LogMgr.i("onCreate registerListener");
        mRollFilter = new RollFilter();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroSsensor, SensorManager.SENSOR_DELAY_FASTEST);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroValues = event.values;
            //H34
            gyroX = -gyroValues[0];//pitch
            gyroY = -gyroValues[2];//roll
            gyroZ = gyroValues[1];//yaw
            if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                gyroX = -gyroValues[0];//pitch
                gyroY = gyroValues[2];//roll
                gyroZ = gyroValues[1];//yaw
            }
//            Log.d(TAG, "call updateGyro()" + "===>(" + gyroX + "," + gyroY + "," + gyroZ + ")");

            timestamp = event.timestamp;
            walk.updateGyro(gyroX, gyroY, gyroZ);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accValues = event.values;
            //新版本陀螺仪方向h34
            accelX = -accValues[0];//pitch
            accelY = -accValues[2];//roll
            accelZ = accValues[1];//yaw
            if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                accelX = -accValues[0];//pitch
                accelY = accValues[2];//roll
                accelZ = accValues[1];//yaw
            }
//            Log.d(TAG, "call updateAccele()" + "===>(" + accelX + "," + accelY + "," + accelZ + ")");
            walk.updateAccele(accelX, accelY, accelZ);
        }

        if (gyroValues != null && accValues != null) {
            Angle_Pitch = Get_Angle(2, gyroX, gyroZ, accelY, accelZ);
            Angle_roll = mRollFilter.Get_Angle(2, gyroY, gyroZ, accelX, accelZ);
//            Log.e("SensorAngle", "Angle_Pitch====>" + Angle_Pitch + " ,Angle_roll====>" + Angle_roll);
            walk.updateAngle(Angle_roll, Angle_Pitch, 0);
//            LogMgr.v("获取到角度值 Angle_Pitch = "+Angle_Pitch);
            if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
                if ((float) Angle_Pitch > 70 || (float) Angle_Pitch < -70) {//倒下状态。
                    LogMgr.w("机器倒下状态 Angle_Pitch = " + Angle_Pitch);
                    if (!ControlApplication.IsRobotFall) {
                        // PlayMoveOrSoundUtils.getInstance().forceStop(false);
                        mIntent = new Intent(SENSOR_RESULT);
                        mIntent.putExtra(SENSOR_BACK, SENSOR_DOWN);
                        sendBroadcast(mIntent);
                        LogMgr.e("发送机器倒下广播 Angle_Pitch = " + Angle_Pitch);
                        ControlApplication.IsRobotFall = true;
                        timeInit = 0;
                        timeend = 0;
                        UPtimeInit = 0;
                        UPtimeend = 0;
                    }

                    if (timeInit == 0) {//刚检测到倒下要记录时间。
                        LogMgr.d("刚检测到倒下要记录时间");
                        timeInit = System.currentTimeMillis();
                    } else {  //已经不是刚倒下了开始计算倒下时间。
                        timeend = System.currentTimeMillis();
                        //倒下2秒钟后发送自动爬起的广播
                        if (timeend - timeInit > 2 * 1000) {//大于5秒钟。
                            timeend = 0;
                            timeInit = 0;//爬起来给10秒钟时间。
                            // PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                            //这个是倒的方向，
                            if ((float) Angle_Pitch > 0) {


//                            qiandao();
                                mIntent = new Intent(SENSOR_RESULT);
                                mIntent.putExtra(SENSOR_BACK, FRONT_UP);
                                sendBroadcast(mIntent);
                                LogMgr.e("发送机器前起广播 Angle_Pitch = " + Angle_Pitch);
                            } else {
//                            houqi();
                                mIntent = new Intent(SENSOR_RESULT);
                                mIntent.putExtra(SENSOR_BACK, BACK_UP);
                                sendBroadcast(mIntent);
                                LogMgr.e("发送机器后起广播 Angle_Pitch = " + Angle_Pitch);
                            }
                        }

                    }
                } else if ((float) Angle_Pitch > -30 && (float) Angle_Pitch < 30 && ControlApplication.IsRobotFall) {//站立状态监测。
                    LogMgr.i("检测到机器站立状态");
                    if (UPtimeInit == 0) {
                        LogMgr.d("刚检测到站立状态监测要记录时间");
                        UPtimeInit = System.currentTimeMillis();
                    } else {
                        UPtimeend = System.currentTimeMillis();
                        if (UPtimeend - UPtimeInit > 5 * 1000 && ControlApplication.IsRobotFall) {//大于5秒钟。
                            UPtimeInit = 0;
                            UPtimeend = 0;
                            ControlApplication.IsRobotFall = false;
                            LogMgr.e("发送机器站立状态广播");
                            mIntent = new Intent(SENSOR_RESULT);
                            mIntent.putExtra(SENSOR_BACK, SENSOR_UP);
                            sendBroadcast(mIntent);
//                                String path0 = Environment.getExternalStorageDirectory().getPath() + File.separator
// + "Download" + File.separator + "Walk.bin";
//                                String soundFile = Environment.getExternalStorageDirectory().getPath() + File
// .separator + "Download" + File.separator + "move5.wav";
//                                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(path0, soundFile, true, false, 0,
// false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL);
                        }

                    }


                } else {
                    LogMgr.i("中间状态，摔倒时间和站立时间清零");
                    timeInit = 0; //站立时间清零。
                    UPtimeInit = 0;
                }
            } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                if ((float) Angle_Pitch > -120 && (float) Angle_Pitch < 120) {//倒下状态。
                    if (!ControlApplication.IsRobotFall) {
                        // PlayMoveOrSoundUtils.getInstance().forceStop(false);
                        mIntent = new Intent(SENSOR_RESULT);
                        mIntent.putExtra(SENSOR_BACK, SENSOR_DOWN);
                        sendBroadcast(mIntent);
                        Log.e(TAG, "倒了" + Angle_Pitch);
                        ControlApplication.IsRobotFall = true;
                        timeInit = 0;
                        timeend = 0;
                        UPtimeInit = 0;
                        UPtimeend = 0;
                    }

                    if (timeInit == 0) {//刚检测到倒下要记录时间。
                        timeInit = System.currentTimeMillis();
                    } else {  //已经不是刚倒下了开始计算倒下时间。
                        timeend = System.currentTimeMillis();
                        if (timeend - timeInit > 5 * 1000) {//大于5秒钟。
                            timeend = 0;
                            timeInit = 0;//爬起来给10秒钟时间。
                            // PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                            //这个是倒的方向，
                            if ((float) Angle_Pitch > 0) {


//                            qiandao();
                                mIntent = new Intent(SENSOR_RESULT);
                                mIntent.putExtra(SENSOR_BACK, FRONT_UP);
                                sendBroadcast(mIntent);
                                Log.e(TAG, "前起" + Angle_Pitch);
                            } else {
//                            houqi();
                                mIntent = new Intent(SENSOR_RESULT);
                                mIntent.putExtra(SENSOR_BACK, BACK_UP);
                                sendBroadcast(mIntent);
                                Log.e(TAG, "后起" + Angle_Pitch);
                            }
                        }
                    }
                } else if ((float) Angle_Pitch < -160 || (float) Angle_Pitch < 160 && ControlApplication.IsRobotFall) {//站立状态监测。
                    if (UPtimeInit == 0) {

                        UPtimeInit = System.currentTimeMillis();
                    } else {
                        UPtimeend = System.currentTimeMillis();
                        if (UPtimeend - UPtimeInit > 5 * 1000 && ControlApplication.IsRobotFall) {//大于5秒钟。
                            UPtimeInit = 0;
                            UPtimeend = 0;
                            ControlApplication.IsRobotFall = false;
                            Log.e(TAG, "走");
                            mIntent = new Intent(SENSOR_RESULT);
                            mIntent.putExtra(SENSOR_BACK, SENSOR_UP);
                            sendBroadcast(mIntent);
//                                String path0 = Environment.getExternalStorageDirectory().getPath() + File.separator
// + "Download" + File.separator + "Walk.bin";
//                                String soundFile = Environment.getExternalStorageDirectory().getPath() + File
// .separator + "Download" + File.separator + "move5.wav";
//                                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(path0, soundFile, true, false, 0,
// false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL);
                        }

                    }

                } else {
                    timeInit = 0; //站立时间清零。
                    UPtimeInit = 0;
                }
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onInit() {
        zeroDriftTatle = 0.0f;
        zeroDriftCount = 0;
        ignoreCountWhenCalcuZeroDrift = 1000;
        initStatus = INIT_STATUS.ON_INITING;
//        setButtonStatus(initStatus);
    }

    public static float[] getGyroValues() {
        return gyroValues;
    }

    public static float getGyroX(){
        return gyroX;
    }

    public static float getGyroY(){
        return gyroY;
    }

    public static float[] getAccValues() {
        return accValues;
    }

    public static float getAngle_Pitch() {
        return Angle_Pitch;
    }

    private float Get_Angle(int way, float gyroX, float gyroZ, float accelY, float accelZ) {
        float Accel_Y;
        float Accel_Angle;
        float Accel_Z;
        float Gyro_X;
        float Gyro_Z;

        Gyro_X = gyroX;
        Gyro_Z = gyroZ;
        Accel_Y = accelY;
        Accel_Z = accelZ;
        if (Gyro_X > 32768) Gyro_X -= 65536;
        if (Gyro_Z > 32768) Gyro_Z -= 65536;
        if (Accel_Y > 32768) Accel_Y -= 65536;
        if (Accel_Z > 32768) Accel_Z -= 65536;
        Gyro_Balance = Gyro_X;
        Accel_Angle = (float) (Math.atan2(Accel_Y, Accel_Z) * 180 / (3.1415926));
        //Gyro_X= (float) (Gyro_X/16.4/2);
        if (way == 2) Kalman_Filter(Accel_Angle, Gyro_X);
        else if (way == 3) Yijielvbo(Accel_Angle, Gyro_X);
        Angle_Pitch = angleR;
        Gyro_Turn = Gyro_Z;
        Acceleration_Z = Accel_Z;
        return Angle_Pitch;
    }

    private void Kalman_Filter(float Accel, float Gyro) {
        angleR += (Gyro - Q_bias) * dt;
        Pdot[0] = (float) (Q_angle - PP_new[0][1] - PP_new[1][0]);

        Pdot[1] = -PP_new[1][1];
        Pdot[2] = -PP_new[1][1];
        Pdot[3] = (float) Q_gyro;
        PP_new[0][0] += Pdot[0] * dt;
        PP_new[0][1] += Pdot[1] * dt;
        PP_new[1][0] += Pdot[2] * dt;
        PP_new[1][1] += Pdot[3] * dt;

        Angle_err = Accel - angleR;

        PCt_0 = C_0 * PP_new[0][0];
        PCt_1 = C_0 * PP_new[1][0];

        E = (float) (R_angle + C_0 * PCt_0);

        K_0 = PCt_0 / E;
        K_1 = PCt_1 / E;

        t_0 = PCt_0;
        t_1 = C_0 * PP_new[0][1];

        PP_new[0][0] -= K_0 * t_0;
        PP_new[0][1] -= K_0 * t_1;
        PP_new[1][0] -= K_1 * t_0;
        PP_new[1][1] -= K_1 * t_1;

        angleR += K_0 * Angle_err;
        Q_bias += K_1 * Angle_err;
        angle_dot = Gyro - Q_bias;
    }

    private void Yijielvbo(float angle_m, float gyro_m) {
        angleR = (float) (K1 * angle_m + (1 - K1) * (angleR + gyro_m * 0.010));        // 0.005
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != sensorManager) {
            sensorManager.unregisterListener(this);
        }
    }
}
