package com.abilix.control.pad;

import com.abilix.control.uav.F3MavDataProcess;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.uav.DataBuffer;

import android.os.Handler;
import android.os.Message;

public class FProtocolDisposer extends AbstractProtocolDisposer {
    public static F3MavDataProcess dataProcess = F3MavDataProcess.GetManger();

    public FProtocolDisposer(Handler mHandler) {
        super(mHandler);
        dataProcess.initHandler(mHandler);
    }

    @Override
    public void DisposeProtocol(Message msg) {
        try {
            byte[] data = (byte[]) msg.obj;
            // 新协议
            if ((data[0] & 0xff) == 0xaa && data[1] == 0x55) {//校验头
                dataProcess.ParseData(data);//解析
            }else{
                LogMgr.e("ParseDataUDP FProtocolDisposer  DisposeProtocol校验头不通过  ");
            }
            DataBuffer.spLastSendTime = System.currentTimeMillis();
        } catch (Exception e) {
            LogMgr.e("ParseDataUDP FProtocolDisposer  DisposeProtocol出错了  " + e);
        }
    }
}
