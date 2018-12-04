package com.abilix.control.uav;

import com.abilix.control.utils.LogMgr;

import java.io.ByteArrayOutputStream;

/**
 * Created by yangq on 2017/6/30.
 */

public class CustomDataProcess extends DataProcess{
    private static CustomDataProcess dataProcess = null;
    public static CustomDataProcess GetManger() {
        // 单例
        if (dataProcess == null) {
            dataProcess = new CustomDataProcess();
        }
        return dataProcess;
    }
    @Override
    public void specificParseData(byte[] data){
        switch (data[5] & 0xff) {//命令字1
            case 0xA3:// 控制指令
                try {
                    if ((data[6] & 0xff) == 0x91  || (data[6] & 0xff)==0x92 || (data[6] & 0xff)==0x93) {//发给串口2的关闭电源数据
                        if(isShowLog[0]) LogMgr.i("ParseDataUDP Motor收到数据 "+showDataHex(data));
//                        getSerialMotorData();
                        sendSerialMotorData(data);
                    } else if (System.currentTimeMillis() - lastTime >= 10) {//速度太快了导致handler卡死
                        LogMgr.e("ParseDataUDP收到的数据校验通过 "+(System.currentTimeMillis() - lastTime));
                        if (dataBuf.size() > 0) {
                            dataBuf.write(data);
                            data = dataBuf.toByteArray();
                            dataBuf = new ByteArrayOutputStream();
                        }
                        //AFIndex0= DataTransform.bytesToInt2h(data,8);
                        //if(AFIndex0!=AFIndex){
                        sendSerialData(data);//直接转发给飞控
                        //AFIndex=AFIndex0;
                        //}
                        lastTime = System.currentTimeMillis();
                    } else {
                        dataBuf.write(data);
                        LogMgr.i("ParseDataUDP收到的数据太快");
                    }
                } catch (Exception e) {
                    LogMgr.e("ParseDataUDP  发送数据出错了" + e);
                }
//							LogMgr.i("ParseDataUDP收到的数据校验通过");
                break;
        }
    }
    @Override
    public void ParseSerialData(byte[] data){//处理接收到的串口数据
        sendData(data);//直接发送出去
    }
    @Override
    public void readDataRun(byte[] data){
//        dataProcess.getSerialData();
//        if ((data = DataBuffer.readData()) != null) {
//            dataProcess.sendData(data);
//        }
        try {
            Thread.sleep(15);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
