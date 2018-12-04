package org.winplus.serial.utils;

import com.abilix.control.utils.LogMgr;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags)
            throws SecurityException, IOException {

        if (!device.canRead() || !device.canWrite()) {
            try {
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                        + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            LogMgr.e("open fail");
            throw new IOException();

        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public FileInputStream getInputStream() {
        if (mFileInputStream != null) {
            return mFileInputStream;
        } else {
            LogMgr.e("mFileInputStream is null");
            return null;
        }

    }

    public FileOutputStream getOutputStream() {
        if (mFileOutputStream != null) {
            return mFileOutputStream;
        } else {
            LogMgr.e("mFileOutputStream is null");
            return null;
        }

    }

    private native static FileDescriptor open(String path, int baudrate,
                                              int flags);

    public native void close();

    static {
        System.loadLibrary("serial_port");
    }
    public  void closeStream(){
        LogMgr.e("关闭串口stream");
        try {
            if (mFileOutputStream != null) {
                mFileOutputStream.close();
                mFileOutputStream = null;
            }
            if (mFileInputStream != null) {
                mFileInputStream.close();
                mFileInputStream = null;
            }
            if(mFd!=null){
                mFd=null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
