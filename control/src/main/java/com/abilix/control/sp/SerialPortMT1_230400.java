package com.abilix.control.sp;



/**
 * @author jingh
 * @Descripton:MT1串口、波特率为230400具体串口实现
 * @date2017-3-24上午11:22:24
 */
public class SerialPortMT1_230400 extends AbstractSerialPort {

    public SerialPortMT1_230400() {
        super();
    }

    @Override
    protected void initSerialPort() {
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT1_230400();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
    }

}
