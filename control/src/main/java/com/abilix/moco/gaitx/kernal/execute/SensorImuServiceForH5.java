package com.abilix.moco.gaitx.kernal.execute;

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
import com.abilix.control.utils.LogMgr;


public class SensorImuServiceForH5 extends Service implements SensorEventListener {

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
    //=================================Yaw=========================================
    private static boolean isAngleYawAvaiable = false;
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

    private static INIT_STATUS initStatus = INIT_STATUS.NOT_INIT;
    private float zeroDriftSum = 0.0f;
    private int zeroDriftCount = 0;

    private boolean beginYaw = false;
    private float beginYawTime = 0;
    private float beginYawValue = 0.0f;
    private static int ZERO_DRIFT_MAX_COUNT = 1000;   //计算零漂第一阶测试次数  默认值1000,大约耗时6秒钟 500,大约耗时3秒钟
    private int TestYawTime_s = 5;                   //计算零漂第二阶测试时长，单位：秒 默认测试值为15
    private float yawDiffPreMinute = 3.0f;            //除零漂后计算的yaw值，每分钟最大偏yawDiffPreMinute度 默认值：0.1
    private float preYawMaxDiff = TestYawTime_s * yawDiffPreMinute / 60.0f;
    private int ignoreCountWhenCalcuZeroDrift = 500;   //不考虑第一次放下机器人时，陀螺仪不准确。 默认值1000,大约耗时6秒钟
    //=========================================================================================================

    private Executor executor;

    /**平衡服务是否启动*/
    private static boolean instanceb = false;

    public static void startSensorImuServiceForH5() {
        if (!instanceb) {
            instanceb = true;
            LogMgr.e("startSensorImuServiceForH5 启动步态服务");
            ControlApplication.instance.startService(new Intent(ControlApplication.instance, SensorImuServiceForH5.class));
        }
    }


    /**
     * 停止服务
     */
    public static void stopSensorImuServiceForH5() {
        LogMgr.e("stopSensorImuServiceForH5 步态服务停止");
        if (instanceb) {
            ControlApplication.instance.stopService(new Intent(ControlApplication.instance, SensorImuServiceForH5.class));
            instanceb = false;
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        onInit();
        Log.e("IMU Service", "registerListener----- ");
        executor = Executor.getInstance();
        mRollFilter = new RollFilter();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroSsensor, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, gyroSsensor, SensorManager.SENSOR_DELAY_GAME);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != sensorManager) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroValues = event.values;
            //H3
//            gyroX =  gyroValues[1];//pitch
//            gyroY = -gyroValues[2];//roll
//            gyroZ =  gyroValues[0];//yaw
            //H5
            gyroX = -gyroValues[0];//pitch
            gyroY = -gyroValues[2];//roll
            gyroZ = -gyroValues[1];//yaw


            if (timestamp != 0) {
                if (initStatus == INIT_STATUS.ON_INITING) {
                    if (--ignoreCountWhenCalcuZeroDrift > 0) {
                        return;
                    }
                    zeroDriftSum += gyroZ;
                    ++zeroDriftCount;
                    if (zeroDriftCount >= ZERO_DRIFT_MAX_COUNT) {
                        zeroDrift = zeroDriftSum / zeroDriftCount;
                        initStatus = INIT_STATUS.ON_INITING2;
                        beginYaw = false;
                    }
                } else {
                    final float dT = (event.timestamp - timestamp) * NS2S;
                    angle[0] += (gyroZ - zeroDrift) * dT;
                    Angle_yaw = (float) Math.toDegrees(angle[0]);
                    if (Angle_yaw > 360) Angle_yaw -= 360;
                    if (Angle_yaw < -360) Angle_yaw += 360;
                    if (initStatus == INIT_STATUS.ON_INITING2) {
                        if (beginYaw == false) {
                            beginYawTime = event.timestamp;
                            beginYawValue = Angle_yaw;
                            beginYaw = true;
                        } else {
                            float t_time = event.timestamp - beginYawTime;
                            if (t_time > 1000000000 * TestYawTime_s) {
                                float diff = Math.abs(Math.abs(Angle_yaw) - Math.abs(beginYawValue));
                                if (preYawMaxDiff >= diff) {
                                    initStatus = INIT_STATUS.INITED;
                                    isAngleYawAvaiable = true;
                                } else {
                                    onInit();
                                }
                            }
                        }
                    }
                }
            }

            timestamp = event.timestamp;
            executor.updateGyro(gyroX, gyroY, gyroZ);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accValues = event.values;
//            accelX = accValues[1];
//            accelY = -accValues[2];
//            accelZ = accValues[0];
            accelX = -accValues[0];
            accelY = -accValues[2];
            accelZ = -accValues[1];
//            Log.e("SensorIMU", "accel [0]====> " + accValues[0] +" ,[1]====> "+accValues[1] + " ,[2]====> " + accValues[2]);
            executor.updateAccele(accelX, accelY, accelZ);
        }

        if (gyroValues != null && accValues != null) {
            Angle_Pitch = Get_Angle(2, gyroX, gyroZ, accelY, accelZ);

            Angle_roll = mRollFilter.Get_Angle(2, gyroY, gyroZ, accelX, accelZ);
//            Log.e("SensorIMU", "Angle_Pitch====> " + Angle_Pitch + " ,Angle_yaw====> " + Angle_yaw + " ,Angle_roll====> " + Angle_roll);
//            Log.e("SensorIMU","Angle_yaw====>"+Angle_yaw);
            //LogMgr.e("++++++++++++++++++++"+Angle_Pitch);
//            executor.updateAngle(Angle_roll, Angle_Pitch - 6, Angle_yaw);
            executor.updateAngle(Angle_roll, Angle_Pitch, Angle_yaw);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onInit() {
        zeroDriftSum = 0.0f;
        zeroDriftCount = 0;
        ignoreCountWhenCalcuZeroDrift = 0;
        initStatus = INIT_STATUS.ON_INITING;
        isAngleYawAvaiable = false;
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

    public static float getAngle_Roll() {
        return Angle_roll;
    }

    public static float getAngle_Pitch() {
        return Angle_Pitch;
    }

    public static float getAngle_yaw() {
        if (isAngleYawAvaiable) {
            return Angle_yaw;
        }
        return -1;
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
//        Log.e("SensorIMU", "Accel_Y: "+Accel_Y+" Accel_Z: "+Accel_Z );
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
}
