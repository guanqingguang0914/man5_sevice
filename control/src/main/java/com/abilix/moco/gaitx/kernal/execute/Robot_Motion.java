package com.abilix.moco.gaitx.kernal.execute;

public enum Robot_Motion {
    WALK("WALK", 0x1234),
    RUN("RUN", 0x1235),
    UPSTAIRS("UPSTAIRS", 0x1236),
    DOWNSTAIRS("DOWNSTAIRS", 0x1237),
    ROTATE1("ROTATE1", 0x1238),
    ROTATE2("ROTATE2", 0x1239),
    ROTATE11("ROTATE11", 0x123A),
    ROTATE22("ROTATE22", 0x123B),
    WALK2RUN("WALK2RUN", 0x123C),
    STAY("STAY", 0x123D),
    CALIB("CALIB", 0x123E),
    READBIN("READBIN", 0x1235);

    private String name;
    private int index;

    Robot_Motion(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name + " = " + this.index;
    }

}
