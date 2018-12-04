package com.abilix.control.sp;

import com.abilix.control.utils.LogMgr;



/**
 * @author jingh
 * @Descripton:MT1串口、波特率为500000具体串口实现
 * @date2017-3-24上午11:22:24
 */
public class SerialPortMT1_500000 extends AbstractSerialPort {

    public SerialPortMT1_500000() {
        super();
    }

    @Override
    protected void initSerialPort() {
        LogMgr.d("initSerialPort");
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT1_500000();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();

        mViceSerialPort = SerialPortMgr.getSerialPort_MT0_9600();
        mViceOs = mViceSerialPort.getOutputStream();
        mViceIs = mViceSerialPort.getInputStream();
    }

}
