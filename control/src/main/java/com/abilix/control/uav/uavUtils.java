package com.abilix.control.uav;

/**
 * Created by use038 on 2017/10/17 0017.
 */

public class uavUtils {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("uav-utils");
    }

    public static native long crc32(byte[] a, int len, long state);

}