package com.abilix.control.process;

import android.util.Log;

public class ProcessControl {
    private volatile static ProcessControl instance;

    private ProcessControl() {
    }

    public static ProcessControl getInstance() {
        if (instance == null) {
            synchronized (ProcessControl.class) {
                if (instance == null) {
                    instance = new ProcessControl();
                }
            }
        }
        return instance;
    }

    public native void timerslack(int tid,int period);

    static {
        Log.e("ProcessControl","======================================>");
        System.loadLibrary("process_control");
    }
}
