package com.abilix.control.sensor;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.MainActivity;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.GaitAlgorithm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 指南针 陀螺仪
 *
 * @author luox
 */
public class MySensor {
    // 指南针 ， 陀螺仪
    private Sensor mZSensor, mTSensor, accelerometerSensor;
    ;
    private SensorManager mSensorManager;
    // 监听 指南针与陀螺仪
    private MySensorEventListener mYSensorEventListener;
    // 角度
    private float[] mO = {0, 0, 0};
    // 角速度
    private float[] mG = {0, 0, 0};
    // 加速度
    private float[] mS = {0, 0, 0};

    public double dtadd = 0;//平衡车数据
    public float zero_value = 0;//平衡车数据
    public boolean startBalanceCar = false;//平衡车数据
    public boolean getBalanceDate = false;//角速度变化

    private static MySensor mYSensor;

    public static MySensor obtainMySensor(Context mContext) {
        if (mYSensor == null) {
            synchronized (MySensor.class) {
                if (mYSensor == null) {
                    mYSensor = new MySensor(mContext);
                }
            }
        }
        return mYSensor;
    }

    private MySensor(Context mContext) {
        mSensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
    }

//    public boolean isOutPutGyr = false;
//    BufferedWriter bufferedWriter = null;

    /**
     * 监听 指南针与 陀螺仪
     *
     * @author luox
     */
//	public static int sonCount=0;
//	public static long sontime=0;
    private class MySensorEventListener implements SensorEventListener {
        private float dtadd0 = 0;
        private double dt0 = 0;
        private float[] values;
        private static final double NS2S = 1.0 / 1000000000.0;// 9个0.
        private double timestamp;

        private long lastReleaseTime;
        private int count=0;


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
//            LogMgr.v("sensor refresh value isOutPutGyr = "+isOutPutGyr+" bufferedWriter == null is "+(bufferedWriter == null));
//            try {
//                if(isOutPutGyr && bufferedWriter == null){
//                    bufferedWriter = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory().getPath() + File.separator + MainActivity.mFileNameForGyr,
//                            true));
//                }
//                if(!isOutPutGyr && bufferedWriter!=null){
//                    bufferedWriter.close();
//                    bufferedWriter = null;
//                }

                // 角速度
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {//10秒钟大概700次，14ms获取一次陀螺仪数据
                    mYSensor.setmG(event.values);
//                    if(isOutPutGyr){
//                        bufferedWriter.write(Utils.getNowDate()+",GYR,"+event.values[0]+","+event.values[1]+","+event.values[2]+"\n");
//                    }
                    if (startBalanceCar) {//平衡车数据
                        getBalanceDate = true;
                        values = event.values; // 角速度。
                        if (timestamp != 0) {
                            dt0 = (event.timestamp - timestamp);
                        }
                        timestamp = event.timestamp;
                        dtadd += (values[0] - zero_value) * dt0 * NS2S;// 这里减去一个偏差 得到偏移角度。
                    }
                }// 角度,方向，
                else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                    mYSensor.setmO(event.values);
                    LogMgr.i("values[0] == "+event.values[0] + " values[1] == "+event.values[1] + " values[2] == "+event.values[2] );
                    if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H && event.values[1] != 0.0f && (event.values[1] > 135 || event.values[1] < 45) ){
                        LogMgr.w("H角度倾斜大于45度");
                        count++;
                        if(count>=3 && System.currentTimeMillis() - lastReleaseTime > 5*1000){
                            LogMgr.e("H角度倾斜大于45度，释放全身舵机");
                            PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                            GaitAlgorithm.getInstance().destoryWalk();
//                            ProtocolUtils.engineStateChangeAll(ProtocolUtils.ENGINE_STATE_RELEASE);
                            ProtocolUtils.relAndFix(0, (byte) 0x18);
                            lastReleaseTime = System.currentTimeMillis();
                        }
                    }else{
                        count = 0;
                    }
//                    if(isOutPutGyr){
//                        bufferedWriter.write(Utils.getNowDate()+",ORI,"+event.values[0]+","+event.values[1]+","+event.values[2]+"\n");
//                    }
                }    // 加速度
                else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mYSensor.setmS(event.values);
//                    if(isOutPutGyr){
//                        bufferedWriter.write(Utils.getNowDate()+",ACC,"+event.values[0]+","+event.values[1]+","+event.values[2]+"\n");
//                    }
                }
//            } catch (IOException e) {
//                LogMgr.e("写文件错误");
//                e.printStackTrace();
//            }
        }
    }

    /**
     * 打开 指南针与陀螺仪
     */
    public void openSensorEventListener() {
        getOrientationData();
        getGyroscopeData();
        if (mYSensorEventListener == null){
            mYSensorEventListener = new MySensorEventListener();
        mSensorManager.registerListener(mYSensorEventListener, mZSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mYSensorEventListener, mTSensor,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mYSensorEventListener,
                accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /**
     * 关闭 指南针与陀螺仪
     */
    public void stopSensorEventListener() {
        if (mYSensorEventListener != null) {
            mSensorManager.registerListener(mYSensorEventListener, mZSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(mYSensorEventListener, mTSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(mYSensorEventListener,
                    accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        mYSensorEventListener = null;
        mZSensor = null;
        mTSensor = null;
        accelerometerSensor = null;
        mO = null;
        mS = null;
        mG = null;
        mYSensor = null;
    }

    // 指南针
    private void getOrientationData() {
        mZSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    // 陀螺仪
    private void getGyroscopeData() {
        accelerometerSensor = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mTSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public float[] getmO() {
        return mO;
    }

    public void setmO(float[] mO) {
        this.mO = mO;
    }

    public float[] getmG() {
        return mG;
    }

    public void setmG(float[] mG) {
        this.mG = mG;

    }

    public float[] getmS() {
        return mS;
    }

    public float getMcompass() {

//		float value = 0;
//		float rawValue = getmO()[0];
//		if (rawValue - 180 > 0) {
//			value = rawValue - 180;
//		} else {
//			value = rawValue + 180;
//		}
        return (getmO()[0] + 180) % 360;

        //return value;
    }

    public void setmS(float[] mS) {
        this.mS = mS;
    }

    public void unRegistSensor() {
        mSensorManager.unregisterListener(mYSensorEventListener);
    }
}
