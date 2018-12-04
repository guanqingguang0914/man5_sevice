package com.abilix.moco.gaitx.kernal.execute;

/**
 * Created by xiejj on 2017/10/24.
 */

public enum WALK_PARAM{
    N("N",0x3857),
    ROLL("ROLL",0x3858),
    PITCH("PITCH",0x3859),
    YAW("YAW",0x385A),
    INITBETA("INITBETA",0x385B),
    INITGAMMA("INITGAMMA",0x385C),
    INITALPHA("INITALPHA",0x385D),
    BETATARGET("BETATARGET",0x385E),
    DISTANCE("DISTANCE",0x385F),
    ANGLE("ANGLE",0x3860),
    FORWARD("FORWARD",0x3861),
    BACKWARD("BACKWARD",0x3862),
    SPEEDLIMITVAL("SPEEDLIMITVAL",0x3863),
    WALK2RUNVAL("WALK2RUNVAL",0x3864),
    GYROX("GYROX",0x3865),
    GYROY("GYROY",0x3866),
    GYROZ("GYROZ",0x3867),
    ACCX("ACCX",0x3868),
    ACCY("ACCY",0x3869),
    ACCZ("ACCZ",0x386A),
    BALANCEB("BALANCEB",0x386B);

    private String name;
    private int index;

    WALK_PARAM(String name, int index){
        this.name = name;
        this.index = index;
    }

    public int getIndex(){
        return this.index;
    }
    public String getName(){
        return this.name;
    }
    @Override
    public String toString(){
        return this.name + " = " +this.index;
    }

}
