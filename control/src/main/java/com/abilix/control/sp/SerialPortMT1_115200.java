package com.abilix.control.sp;

/**
 * Created by yangq on 2017/7/4.
 */

public class SerialPortMT1_115200 extends AbstractSerialPort {

    public SerialPortMT1_115200() {
        super();
    }

    @Override
    protected void initSerialPort() {
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT1_115200();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
    }
}
