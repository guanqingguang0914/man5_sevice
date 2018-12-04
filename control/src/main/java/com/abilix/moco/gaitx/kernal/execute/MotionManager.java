package com.abilix.moco.gaitx.kernal.execute;

/**
 * Created by xiejj on 2018/1/4.
 */

public class MotionManager {
    static {
        System.loadLibrary("motionmanager");
    }

    /**
     * @param time
     * @return
     */
    public native double[] walk(double time, int motionType);

    /**
     * @return
     */
    public native double getTimeAll();

    /**
     * @param index
     * @param value
     */
    public native void setParam(int index, double value);

    /**
     * @param index
     * @param value
     */
    public native void setParam(int index, boolean value);

    /**
     * @param index
     * @param value
     */
    public native void setParam(int index, int value);

    /**
     * @param index
     * @return
     */
    public native double getParam(int index);

    public native void setAngle(double Angle[],int len);
}
