package com.abilix.control.sp;


/**
 * @author jingh
 * @Descripton:MT0串口、波特率为57600具体串口实现
 * @date2017-4-14下午5:21:56
 */
public class SerialPortMT0_57600 extends AbstractSerialPort {
    public SerialPortMT0_57600() {
        super();
    }

    @Override
    protected void initSerialPort() {
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT0_57600();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
        mViceSerialPort = SerialPortMgr.getSerialPort_MT1_57600();
        mViceOs = mViceSerialPort.getOutputStream();
        mViceIs = mViceSerialPort.getInputStream();
    }
}
