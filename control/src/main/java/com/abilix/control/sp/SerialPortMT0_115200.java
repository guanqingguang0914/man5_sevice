package com.abilix.control.sp;



/**
 * @author jingh
 * @Descripton:MT0串口、波特率为115200具体串口实现
 * @date2017-3-24上午11:22:24
 */
public class SerialPortMT0_115200 extends AbstractSerialPort {

    public SerialPortMT0_115200() {
        super();
    }

    @Override
    protected void initSerialPort() {
        super.initSerialPort();
        mSerialPort = SerialPortMgr.getSerialPort_MT0_115200();
        mOs = mSerialPort.getOutputStream();
        mIs = mSerialPort.getInputStream();
    }
}
