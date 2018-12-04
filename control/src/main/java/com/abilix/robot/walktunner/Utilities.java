package com.abilix.robot.walktunner;

/**
 * @author tony
 *
 * 公用类
 */

public class Utilities {

    public static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
