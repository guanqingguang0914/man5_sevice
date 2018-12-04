package com.abilix.control;

public class ResponseBean {
    private int mode;
    private String stringType;
    private int intVersion;
    private String upgradeFilePath;
    private byte upgradeResult;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getStringType() {
        return stringType;
    }

    public void setStringType(String stringType) {
        this.stringType = stringType;
    }

    public Integer getIntVersion() {
        return intVersion;
    }

    public void setIntVersion(Integer intVersion) {
        this.intVersion = intVersion;
    }

    public byte getUpgradeResult() {
        return upgradeResult;
    }

    public void setUpgradeResult(byte upgradeResult) {
        this.upgradeResult = upgradeResult;
    }

    public String getUpgradeFilePath() {
        return upgradeFilePath;
    }

    public void setUpgradeFilePath(String upgradeFilePath) {
        this.upgradeFilePath = upgradeFilePath;
    }

    public String toString() {
        return "mode:" + mode + " " + "stringType:" + stringType + " " + "intVersion:" + intVersion + " " + "upgradeFilePath:" + upgradeFilePath + " " + "upgradeResult:" + upgradeResult;
    }
}
