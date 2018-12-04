package com.abilix.control.scratch;

import com.abilix.control.ControlApplication;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import android.os.Handler;

public class FScratchExecutor extends AbstractScratchExecutor {

    private final int toPad = 0;
    private final int toStm32 = 1;
    private final int toWrite = 2;
    private long pic_pre = 0;
    private long pic_next = 0;
    private long Record_pre = 0;
    private long record_next = 0;
    private byte[] id = new byte[4];// 这里就不用给个都传递请求ID了。
    private final int display_String_mode = 12;
    private final int displayClose = 2;
    protected MySensor mSensor;

    public FScratchExecutor(Handler mHandler) {
        super(mHandler);
        mSensor = MySensor.obtainMySensor(ControlApplication.instance);
        mSensor.openSensorEventListener();
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e(Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private void checkFunction(final byte[] data) {
        int parms1 = -1, parms2 = -1, parms3 = -1;
        System.arraycopy(data, 11, id, 0, id.length);
        switch (data[6]) {
            case 0x01://起飞第一个参数为起飞高度(float类型);
                byte[] heigh = new byte[4];
                System.arraycopy(data,15,heigh,0,4);
                move(0,heigh);
                break;
            case 0x02://平移
                byte[] linear = new byte[4];
                System.arraycopy(data,15,linear,0,4);
                move(1,linear);
                break;
            case 0x03://归位。
                move(2,null);
                break;
            case 0x04://周边LED
                break;
            case 0x05://底部LED。
                break;
            case 0x06://扬声器模拟动物。
                break;
            case 0x07://扬声器模拟乐器
                break;
            case 0x08://扬声器自我介绍
                break;
            case 0x09://探测飞行器起飞高度
                break;
            case 0x0A://探测飞行器朝向角度
                break;
            case 0x0B:
                break;
            case 0x0C:
                break;

        }

    }

    private void move(int type, byte[] heigh) {
        switch (type){
            case 0://高度

                break;
            case 1://平移距离

                break;
            case 2://归位,暂无查到

                break;
        }
    }

    @Override
    public void clearState() {
        // TODO Auto-generated method stub
        pic_next = 0;
        record_next = 0;
        mPlayer.stop();
    }

}
