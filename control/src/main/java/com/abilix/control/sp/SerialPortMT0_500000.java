package com.abilix.control.sp;

import com.abilix.control.utils.LogMgr;


/**
 * @author jingh
 * @Descripton:MT1串口、波特率为500000具体串口实现
 * @date2017-3-24上午11:22:24
 */
public class SerialPortMT0_500000 extends AbstractSerialPort {

    public SerialPortMT0_500000() {
        super();
    }

    @Override
    protected void initSerialPort() {
        LogMgr.d("initSerialPort");
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT0_500000();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
    }

}
