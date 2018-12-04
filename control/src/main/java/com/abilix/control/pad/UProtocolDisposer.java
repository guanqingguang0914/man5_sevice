package com.abilix.control.pad;

import android.os.Handler;
import android.os.Message;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;


public class UProtocolDisposer extends AbstractProtocolDisposer {

    public UProtocolDisposer(Handler handler) {
        super(handler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        LogMgr.e("== UProtocolDisposer : receive message == ");
        byte[] data = (byte[]) msg.obj;
        LogMgr.d("UProtocolDisposer pad cmd::" + Utils.bytesToString(data));

        try {
            if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55) {
//				LogMgr.d("pad query serial port response  "+data[5]+"   "+data[6]+"   "+data[11]);
                //测试UDP命令是否是都收到了
                byte[] buffer = SP.request(data, 20);
                if (buffer == null) {
                    return;
                }
                Message sensorMsg = mHandler.obtainMessage();
                // 新协议 lz 2017-6-7 10:41:47添加
                sensorMsg.what = 1;
                sensorMsg.obj = buffer;
                mHandler.sendMessage(sensorMsg);

            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("dispose cmd error::" + e);
        }
    }


    @Override
    public void stopDisposeProtocol() {
        super.stopDisposeProtocol();
    }
}
