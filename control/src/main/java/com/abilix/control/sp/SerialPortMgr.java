package com.abilix.control.sp;

import com.abilix.control.utils.LogMgr;

import org.winplus.serial.utils.SerialPort;

import java.io.File;
import java.security.InvalidParameterException;

public final class SerialPortMgr {
    private static SerialPort mSerialPort_MT1_230400 = null;
    private static SerialPort mSerialPort_MT1_57600 = null;
    private static SerialPort mSerialPort_MT0_115200 = null;
    private static SerialPort mSerialPort_MT1_115200 = null;
    private static SerialPort mSerialPort_MT0_57600 = null;
    private static SerialPort mSerialPort_MT0_9600 = null;
    private static SerialPort mSerialPort_MT1_500000 = null;
    private static SerialPort mSerialPort_MT0_500000 = null;
    private static SerialPort mSerialPort_MT0_230400 = null;

    public static SerialPort getSerialPort_MT1_230400() {
        try {
            if (mSerialPort_MT1_230400 == null) {
                String path = "/dev/ttyMT1";
              int baudrate = 230400;
             //  int baudrate = 115200;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
                /* Open the serial port */
                LogMgr.d("open the MT1 port baudrate::" + baudrate);
                mSerialPort_MT1_230400 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mSerialPort_MT1_230400;
    }
    public static SerialPort getSerialPort_MT0_9600() {
        try {
            if (mSerialPort_MT0_9600 == null) {
                String path = "/dev/ttyMT0";
                int baudrate = 9600;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT0 port baudrate::" + baudrate);
                mSerialPort_MT0_9600 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT0_9600;
    }
    public static SerialPort getSerialPort_MT0_115200() {
        try {
            if (mSerialPort_MT0_115200 == null) {
                String path = "/dev/ttyMT0";
                int baudrate = 115200;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT0 port baudrate::" + baudrate);
                mSerialPort_MT0_115200 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mSerialPort_MT0_115200;
    }

    public static SerialPort getSerialPort_MT0_500000() {
        try {
            if (mSerialPort_MT0_500000 == null) {
                String path = "/dev/ttyMT0";
                int baudrate = 500000;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT1 port baudrate::" + baudrate);
                mSerialPort_MT0_500000 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT0_500000;
    }

    public static SerialPort getSerialPort_MT1_500000() {
        try {
            if (mSerialPort_MT1_500000 == null) {
                String path = "/dev/ttyMT1";
                int baudrate = 500000;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT1 port baudrate::" + baudrate);
                mSerialPort_MT1_500000 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT1_500000;
    }

    public static SerialPort getSerialPort_MT0_230400() {
        try {
            if (mSerialPort_MT0_230400 == null) {
                String path = "/dev/ttyMT0";
                int baudrate = 230400;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT0 port baudrate::" + baudrate);
                mSerialPort_MT0_230400 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT0_230400;
    }


    public static SerialPort getSerialPort_MT0_57600() {
        try {
            if (mSerialPort_MT0_57600 == null) {
                String path = "/dev/ttyMT0";
                int baudrate = 57600;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT0 port baudrate::" + baudrate);
                mSerialPort_MT0_57600 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT0_57600;
    }

    public static SerialPort getSerialPort_MT1_57600() {
        try {
            if (mSerialPort_MT1_57600 == null) {
                String path = "/dev/ttyMT1";
                int baudrate = 57600;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT1 port baudrate::" + baudrate);
                mSerialPort_MT1_57600 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSerialPort_MT1_57600;
    }

    public static SerialPort getSerialPort_MT1_115200() {
        try {
            if (mSerialPort_MT1_115200 == null) {
                String path = "/dev/ttyMT1";
                int baudrate = 115200;
                if ((path.length() == 0) || (baudrate == -1)) {
                    throw new InvalidParameterException();
                }
				/* Open the serial port */
                LogMgr.d("open the MT1 port baudrate::" + baudrate);
                mSerialPort_MT1_115200 = new SerialPort(new File(path), baudrate, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mSerialPort_MT1_115200;
    }

    public static void closeSerialPort() {
        if (mSerialPort_MT1_230400 != null) {
            mSerialPort_MT1_230400.close();
            mSerialPort_MT1_230400 = null;
        }
        if (mSerialPort_MT0_115200 != null) {
            mSerialPort_MT0_115200.close();
            mSerialPort_MT0_115200 = null;
        }
        if (mSerialPort_MT1_500000 != null) {
            mSerialPort_MT1_500000.close();
            mSerialPort_MT1_500000 = null;
        }
        if (mSerialPort_MT0_230400 != null) {
            mSerialPort_MT0_230400.close();
            mSerialPort_MT0_230400 = null;
        }
        if (mSerialPort_MT0_57600 != null) {
            mSerialPort_MT0_57600.close();
            mSerialPort_MT0_57600 = null;
        }
        if (mSerialPort_MT1_57600 != null) {
            mSerialPort_MT1_57600.close();
            mSerialPort_MT1_57600 = null;
        }
        if(mSerialPort_MT1_115200 != null){
            mSerialPort_MT1_115200.close();
            mSerialPort_MT1_115200=null;
        }
    }

}
