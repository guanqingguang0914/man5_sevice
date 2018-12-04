package com.abilix.robot.walktunner;

/**
 * Created by yangq on 2017/8/29.
 */

public interface Hwalk {
    void startForwardWalk();//前进;
    void startBackwardWalk();//后退
    void startTurnLeftWalk();//左转
    void startTurnRightWalk();//右转
    void startLeftWalk();//左走
    void startLeftForwardWalk();//左前走
    void startLeftBackwardWalk();//左后走
    void startRightWalk();//右走
    void startRightForwardWalk();//右前走
    void startRightBackwardWalk();//右后走
    void stopWalk();//停止
    void destoryWalk();//关闭步态算法
    void setWalkSpeed(int[] speed);//设置速度
}
