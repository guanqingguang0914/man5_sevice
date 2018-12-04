package com.abilix.zmodem;

import com.abilix.control.utils.LogMgr;

import java.io.FileDescriptor;

/**
 * Created by use038 on 2017/10/19 0019.
 */

public class ZmodemUtils {

    public static synchronized int sendFile(String path) {
        int ret = -1;
        try {
            LogMgr.i("sendFile() path = " + path);
            ret = sendByZmodem(path);
            LogMgr.i("sendFile() ret = " + ret);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static synchronized int sendFile(FileDescriptor fileDescriptor, String path) {
        int ret = -1;
        try {
            String stringFd = fileDescriptor.toString();
            int start = stringFd.indexOf('[');
            int end = stringFd.indexOf(']');
            String fdValue = stringFd.substring(start + 1, end);
            int fd = Integer.valueOf(fdValue);
            LogMgr.e("sendFile() fd = " + fdValue);
            ret = sendByZmodemWithFD(fd, path);
            LogMgr.e("sendFile() ret = " + ret);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static native int sendByZmodem(String path);

    private static native int sendByZmodemWithFD(int fd, String path);

    static {
        System.loadLibrary("zmodem");
    }


}
