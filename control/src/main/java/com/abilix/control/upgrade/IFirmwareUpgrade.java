package com.abilix.control.upgrade;

public interface IFirmwareUpgrade {

    int getVersion(byte type);

    boolean upgrade(byte type, String filePath);

    boolean transFileToStm32(byte type, String filePath);

    int getSERVOVersion(byte type);
}
