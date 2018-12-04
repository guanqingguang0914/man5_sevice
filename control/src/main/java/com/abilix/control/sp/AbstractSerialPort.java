package com.abilix.control.sp;


import com.abilix.control.utils.LogMgr;

import org.winplus.serial.utils.SerialPort;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author jingh
 * @Descripton:串口抽象类，集成了所有子类串口的共有成员变量、成员函数
 * @date2017-3-24上午11:21:04
 */
public abstract class AbstractSerialPort implements ISerial {
    protected FileOutputStream mOs;
    protected FileOutputStream mViceOs;
    protected FileInputStream mIs;
    protected FileInputStream mViceIs;
    protected SerialPort mSerialPort;
    protected SerialPort mViceSerialPort;

    public AbstractSerialPort() {
        initSerialPort();
    }

    protected void initSerialPort() {

    }

    @Override
    public FileInputStream getIs() {
        return mIs;
    }

    @Override
    public FileOutputStream getOs() {
        return mOs;
    }

    @Override
    public FileInputStream getViceIs() {
        return mViceIs;
    }

    @Override
    public FileOutputStream getViceOs() {
        return mViceOs;
    }

    @Override
    public synchronized void destorySerialPort() {
        SerialPortMgr.closeSerialPort();
        if (mOs != null) {
            try {
                mOs.close();
                mOs = null;
            } catch (IOException e) {
                try {
                    mOs.close();
                    mOs = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        if (mIs != null) {
            try {
                mIs.close();
                mIs = null;
            } catch (IOException e) {
                try {
                    mIs.close();
                    mIs = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        if (mSerialPort != null) {
           mSerialPort.close();
           mSerialPort.closeStream();
            mSerialPort = null;
        }
        if (mViceOs != null) {
            try {
                mViceOs.close();
                mViceOs = null;
            } catch (IOException e) {
                try {
                    mViceOs.close();
                    mViceOs = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        if (mViceIs != null) {
            try {
                mViceIs.close();
                mViceIs = null;
            } catch (IOException e) {
                try {
                    mViceIs.close();
                    mViceIs = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        if (mViceSerialPort != null) {
           mViceSerialPort.close();
            mViceSerialPort.closeStream();
            mViceSerialPort = null;
        }
        LogMgr.e("关闭串口");
    }

}
