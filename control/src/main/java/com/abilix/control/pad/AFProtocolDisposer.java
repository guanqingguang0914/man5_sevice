package com.abilix.control.pad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.abilix.control.uav.CustomDataProcess;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.uav.DataBuffer;
import com.abilix.control.uav.DataProcess;

import android.os.Handler;
import android.os.Message;

public class AFProtocolDisposer extends AbstractProtocolDisposer {
    public static CustomDataProcess dataProcess = CustomDataProcess.GetManger();

    public AFProtocolDisposer(Handler mHandler) {
        super(mHandler);
        dataProcess.initHandler(mHandler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        try {
            LogMgr.i("ParseDataUDP AFProtocolDisposer pad cmd AF ::");
            byte[] data = (byte[]) msg.obj;
            // 新协议
            if ((data[0] & 0xff) == 0xaa && data[1] == 0x55) {//校验头
                dataProcess.ParseData(data);//解析
            }
            DataBuffer.spLastSendTime = System.currentTimeMillis();
        } catch (Exception e) {
            LogMgr.e("ParseDataUDP AFProtocolDisposer  DisposeProtocol出错了  " + e);
        }
    }
}
