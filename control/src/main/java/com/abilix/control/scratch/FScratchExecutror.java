package com.abilix.control.scratch;

import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import android.os.Handler;

public class FScratchExecutror extends AbstractScratchExecutor {

    public FScratchExecutror(Handler mHandler) {
        super(mHandler);
    }

    @Override
    public void execute(byte[] data) {
        LogMgr.e(Utils.bytesToString(data));
        byte[] rec = new byte[data.length];
        System.arraycopy(data, 0, rec, 0, data.length);
        checkFunction(rec);
    }

    private void checkFunction(final byte[] data) {
        switch (data[6]) {

            case 0x01://起飞
                break;
            case 0x02://平移
                break;
            case 0x03://归位。
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

    @Override
    public void clearState() {
        // TODO Auto-generated method stub

    }

}
