package com.abilix.robot.walktunner;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.utils.LogMgr;

public class Walk {

    private volatile static Walk singleton;
    public static boolean isInit=false;
    private Walk() {
    }

    public static Walk getSingleton() {
        if (singleton == null) {
            synchronized (Walk.class) {
                if (singleton == null) {
                    singleton = new Walk();
                    singleton.initWalk();
                }
            }
        }
        return singleton;
    }

    public native void initWalk();

    public native void stopwalk();

    public native void startForwardWalk(int speed);

    public native void startBackwardWalk(int speed);

    public native void startLeftWalk(int speed);

    public native void startRightWalk(int speed);

    public native void startLeftForwardWalk(int leftSpeed,int forwardSpeed);

    public native void startLeftBackwardWalk(int leftSpeed,int backwardSpeed);

    public native void startRightForwardWalk(int rightSpeed,int forwardSpeed);

    public native void startRightBackwardWalk(int rightSpeed,int forwardSpeed);

    public native void startTurnWalk(int speed,int flag);

    public native void startTurnWalk(int speed,int direction,int flag);

    public native int[] getMotorValue();

    public native void updateGyro(float x, float y, float z);

    public native void updateAccele(float x, float y, float z);

    public native void updateAngle(float x, float y, float z);

    static {
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
            LogMgr.i("System.loadLibrary(\"walk_tuner_h5\");");
            System.loadLibrary("walk_tuner_h5");
        } else {
            LogMgr.i("System.loadLibrary(\"walk_tuner\");");
            System.loadLibrary("walk_tuner");
        }
    }
}
