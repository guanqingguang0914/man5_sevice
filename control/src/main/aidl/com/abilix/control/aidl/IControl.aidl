package com.abilix.control.aidl;
import com.abilix.control.aidl.Control;
import com.abilix.control.aidl.IPushListener;
import com.abilix.control.aidl.IPlayCallBack;
interface IControl {
   void ControlInterface(in Control mBrain);
   void controlSkillPlayer(int state,String filePath);
   byte[] request(in byte[] data);
   byte[] requestTimeout(in byte[] data,int timeout);
   void cancelRequestTimeout();
   byte[] requestVice(in byte[] data);
   int getRobotType();
   void registerPush(IPushListener mListener,String fullClassName);
   void unregisterPush(IPushListener mListener,String fullClassName);
   void write(in byte[] data);
   void writeVice(in byte[] data);
   void destorySP();

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
   void setWalkSpeed(in int[] speed);//设置速度

   byte[] requestOnlyUpdate(in byte[] data, int time);
   void controlSkillPlayerWithCallBack(int state, String actionFileName, String soundFileName,
                                        boolean isLoop,boolean isUseAutoBalance, boolean isMObstacleAvoidanceEnable,IPlayCallBack playCallBack);
}