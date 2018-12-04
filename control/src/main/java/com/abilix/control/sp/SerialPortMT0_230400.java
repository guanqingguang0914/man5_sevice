package com.abilix.control.sp;

import com.abilix.control.utils.LogMgr;



/**
 * @author jingh
 * @Descripton:MT0串口、波特率为230400具体串口实现
 * @date2017-3-24上午11:22:24
 */
public class SerialPortMT0_230400 extends AbstractSerialPort {

    public SerialPortMT0_230400() {
        super();
    }

    @Override
    protected void initSerialPort() {
        LogMgr.d("initSerialPort");
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT0_230400();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
        mViceSerialPort = SerialPortMgr.getSerialPort_MT1_500000();
        mViceOs = mViceSerialPort.getOutputStream();
        mViceIs = mViceSerialPort.getInputStream();

    }
}
