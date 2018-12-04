package com.abilix.control.model;

import android.os.Handler;
import android.support.annotation.CallSuper;

import com.abilix.control.ControlInfo;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;


/**
 * Created by yangz on 2017/7/15.
 */

    public class Model {

    private static Model instance;

    public static final int MODEL_TYPE_EXIT = 0;
    public static final int MODEL_TYPE_TANK = 1;
    public static final int MODEL_TYPE_SCORPION = 2;

    private static final byte SENSOR_CMD_OUT_1 = (byte)0xA3;
    private static final byte SENSOR_CMD_OUT_2 = (byte)0x06;
    private static final byte SENSOR_CMD_IN_1 = (byte)0xF0;
    private static final byte SENSOR_CMD_IN_2 = (byte)0x22;

    public static final int MOVE_STOP = 0;
    public static final int MOVE_FORWARD = 1;
    public static final int MOVE_BACKWARD = 2;
    public static final int MOVE_LEFT = 3;
    public static final int MOVE_RIGHT = 4;
    public static final int MOVE_FORWARD_LEFT = 5;
    public static final int MOVE_FORWARD_RIGHT = 6;
    public static final int MOVE_BACKWARD_LEFT = 7;
    public static final int MOVE_BACKWARD_RIGHT = 8;


    public static final int FUNCTION_AVOID_OBSTACLE = 1;
    public static final int FUNCTION_AUTO_ATTACK = 2;
    public static final int FUNCTION_TOUCH_SENSOR_ENABLE = 3;

    public static final int FUNCTION_OFF = 0;
    public static final int FUNCTION_ON = 1;

    public static final int ACTION_ATTACK = 1;
    public static final int ACTION_RELAX = 2;
    public static final int ACTION_ALERT = 3;
    public static final int ACTION_ROAR = 4;

    protected boolean isCmdAvaliable = true;
    /**碰撞传感器是否生效*/
    protected boolean isTouchSensorEnable = true;
    protected Handler mHandler;
    public int type = -1;
    public ModelCallback mCallback;

    private Model() {
        type = MODEL_TYPE_EXIT;
    }

    protected Model(Handler handler) {
        mHandler = handler;
    }

    /**
     * 初始化或者销毁Model实例
     * @param handler
     * @param modelType Model.MODEL_TYPE_EXIT 代表销毁现有实例
     *                    MODEL_TYPE_TANK 坦克模型 MODEL_TYPE_SCORPION 蝎子模型
     */
    public synchronized static void initOrDestroyInstance(Handler handler, int modelType){
        if(instance != null){
            switch (modelType){
                case MODEL_TYPE_EXIT:
                    LogMgr.i("销毁当前模型");
                    instance.onDestroy();
                    instance = null;
                    return;
                case MODEL_TYPE_TANK:
                    if(instance instanceof TankModel){
                        //当前实例已经是坦克模型
                        LogMgr.d("当前已是坦克模型实例");
                        return;
                    }else{
                        LogMgr.i("销毁当前模型");
                        instance.onDestroy();
                        instance = null;
                    }
                    break;
                case MODEL_TYPE_SCORPION:
                    if(instance instanceof ScorpionModel){
                        //当前实例已经是蝎子模型
                        LogMgr.d("当前已是蝎子模型实例");
                        return;
                    }else{
                        LogMgr.i("销毁当前模型");
                        instance.onDestroy();
                        instance = null;
                    }
                    break;
                default:
                    LogMgr.w("错误的模型类型");
                    instance.onDestroy();
                    instance = null;
                    return;
            }
        }
        switch (modelType){
            case MODEL_TYPE_TANK:
                LogMgr.d("初始化坦克模型实例");
                instance = new TankModel(handler);
                instance.onCreate();
                break;
            case MODEL_TYPE_SCORPION:
                LogMgr.d("初始化蝎子模型实例");
                instance = new ScorpionModel(handler);
                instance.onCreate();
                break;
            default:
                break;
        }
    }

    public static Model getInstance(){
        if(instance == null){
            instance = new Model();
        }
        return instance;
    }

    @CallSuper
    public void move(int moveMode, int speed){
        if(speed < 0 || speed >100){
            LogMgr.e("速度参数异常 speed = "+speed);
            throw new IllegalArgumentException("速度参数异常 speed = "+speed);
        }
    }

    /**
     * 控制模型的功能开关
     * @param functionMode
     * @param onOrOff
     */
    @CallSuper
    public void function(int functionMode, boolean onOrOff){
        switch (functionMode){
            case FUNCTION_TOUCH_SENSOR_ENABLE:
                isTouchSensorEnable = onOrOff;
                break;
            default:
                break;
        }
    }

    @CallSuper
    public void action(int actionType, ModelCallback callback){
        mCallback = callback;
    }

    public void onCreate(){

    }
    @CallSuper
    public void onDestroy(){
        instance = null;
    }

    /**
     * 获取传感器数值
     * @return 第零位为1时代表数据有效，为零时代表此数据无效
     */
    protected int[] getSensorValues(){
        int[] sensorValues = new int[8];

        byte[] cmd = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), SENSOR_CMD_OUT_1,SENSOR_CMD_OUT_2,null);
        byte[] returnData = SP.request(cmd);
        if (returnData == null) {
            LogMgr.e("获取传感器的值为 NULL");
            return sensorValues;
        }
        LogMgr.v("获取传感器的值为 = "+ Utils.bytesToString(returnData));
        if(returnData[5] == SENSOR_CMD_IN_1 && returnData[6] == SENSOR_CMD_IN_2){
            sensorValues[0] = 1;
            for(int i = 0;i<7;i++){
                sensorValues[i + 1] = (int)( ((returnData[11 + i*2]&0xFF)<<8) | (returnData[11 + i*2 + 1]&0xFF) );
                LogMgr.v("传感器"+(i + 1)+"的值 = "+sensorValues[i + 1]);
            }
        }else{
            sensorValues[0] = 0;
        }
        return sensorValues;
    }

    public interface ModelCallback{
        void onActionStart();
        void onActionStop();
        void onActionRefused();
    }

}

