package com.abilix.control.sp;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public interface ISerial {
    FileOutputStream getOs();
    FileInputStream getIs();
    FileOutputStream getViceOs();
    FileInputStream getViceIs();
    void destorySerialPort();
}
