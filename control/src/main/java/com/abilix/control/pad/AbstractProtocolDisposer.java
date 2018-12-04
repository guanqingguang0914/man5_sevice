package com.abilix.control.pad;

import android.os.Handler;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInitiator;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.vedio.Player;

import java.util.concurrent.TimeUnit;

public abstract class AbstractProtocolDisposer implements IProtocolDisposer {
    protected Player mPlayer;
    protected MySensor mSensor;
    protected Handler mHandler;

    public AbstractProtocolDisposer(Handler mHandler) {
        mPlayer = new Player();
        this.mHandler = mHandler;
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
//        mSensor.openSensorEventListener();
    }
    @Override
    public void stopDisposeProtocol() {
//        ProtocolBuilder.sendProtocol(ControlInitiator.ROBOT_TYPE_COMMON,ProtocolBuilder.CMD_RESET,null);
//        ProtocolBuilder.sendProtocol(ControlInitiator.ROBOT_TYPE_COMMON,ProtocolBuilder.CMD_DATA_RESET,null);
//        byte[] data_sleep={(byte)0x01};
//        ProtocolBuilder.sendProtocol(ControlInitiator.ROBOT_TYPE_COMMON,ProtocolBuilder.CMD_SLEEP,data_sleep);
//        byte[] data_weak={(byte)0x00};
//        ProtocolBuilder.sendProtocol(ControlInitiator.ROBOT_TYPE_COMMON,ProtocolBuilder.CMD_SLEEP,data_weak);
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    /**
     * 写入SN码的时候需要时间，这边特殊处理 lz 2017-6-7 11:15:03
     * @param data
     */
    protected void setTimeSleep(byte [] data){
        if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55 && data[5] == (byte) 0x11&& data[6] == (byte) 0x0e){
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
