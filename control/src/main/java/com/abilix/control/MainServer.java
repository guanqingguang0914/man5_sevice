package com.abilix.control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.abilix.control.aidl.Control;
import com.abilix.control.aidl.IControl;
import com.abilix.control.aidl.IPushListener;
import com.abilix.control.aidl.IPlayCallBack;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.sp.PushMsg;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.GaitAlgorithm;

import java.util.HashMap;

/**
 * @author jingh
 * @Descripton:MainServer是Control的Service,对外提供接口。ControlInterface接收从Brain发过来的指令，同时在被Brain绑定后去绑定Brain。 MainServer将从Brain发来得指令交给ControlKernel进行分发。
 * @date2017-3-30上午10:55:06
 */
public class MainServer extends Service {
    HashMap<String, PushMsg> mListenerMap = new HashMap();
    //ArrayList<String> remoteRegisterList=new ArrayList<>();
    private final IControl.Stub mIBinder = new IControl.Stub() {
        @Override
        public void ControlInterface(Control control) throws RemoteException {
            LogMgr.d("receive control from brain");
            ControlKernel.getInstance().dispatchCmd(control);
        }

        @Override
        public byte[] request(byte[] data) throws RemoteException {
            LogMgr.e("request 请求数据：" + Utils.bytesToString(data));
            return SP.request(data);
        }


        @Override
        public byte[] requestTimeout(byte[] data, int timeout) throws RemoteException {
            LogMgr.e("requestTimeout 请求数据：" + Utils.bytesToString(data));
            return SP.request(data,timeout);
        }

        @Override
        public void cancelRequestTimeout() throws RemoteException {
            SP.cancelRequestTimeOut();
        }

        @Override
        public byte[] requestVice(byte[] data) throws RemoteException {
            return SP.requestVice(data);
        }

        @Override
        public void controlSkillPlayer(int state, String filePath) throws RemoteException {
            ControlKernel.getInstance().dispatchSkillPlayerCmd(state, filePath);
        }

        @Override
        public int getRobotType() throws RemoteException {
            int robotType = -1;
            if (ControlInfo.getChild_robot_type() > 0) {
                robotType = ControlInfo.getChild_robot_type();
            }
            LogMgr.d("robotType::" + robotType);
            return robotType;
        }

        @Override
        public void registerPush(final IPushListener mListener, String fullClassName) throws RemoteException {
            //注册前先判断之前有没有注册过，如果有先反注册掉
            PushMsg msg=mListenerMap.get(fullClassName);
            if (msg!=null){
                LogMgr.e("反注册已经存在的监听器");
                SP.unRegisterPushEvent(msg);
            }
            PushMsg mPushMsg = new PushMsg() {
                @Override
                public void onPush(byte[] pushData) {
                    try {
                        LogMgr.e("推送上报数据");
                        mListener.onPush(pushData);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };
            mListenerMap.put(fullClassName, mPushMsg);
            SP.registerPushEvent(mPushMsg);
            LogMgr.d("注册的监听器是:" + fullClassName);
        }

        @Override
        public void unregisterPush(IPushListener mListener, String fullClassName) throws RemoteException {
            LogMgr.e("反注册上报事件监听");
            if (mListenerMap != null) {
                SP.unRegisterPushEvent((PushMsg) mListenerMap.get(fullClassName));
                mListenerMap.remove(fullClassName);
            }
        }

        @Override
        public void write(byte[] data) throws RemoteException {
            SP.write(data);
        }

        @Override
        public void writeVice(byte[] data) throws RemoteException {
            SP.writeVice(data);
        }

        @Override
        public void destorySP() throws RemoteException {
            LogMgr.i("SP.destroySP()");
            SP.destroySP();
        }

        @Override
        public void startForwardWalk(){//前进
            LogMgr.d("startForwardWalk");
            GaitAlgorithm.getInstance().startForwardWalk();
        }
        @Override
        public void startBackwardWalk(){//后退
            LogMgr.d("startBackwardWalk");
            GaitAlgorithm.getInstance().startBackwardWalk();
        }
        @Override
        public void startTurnLeftWalk(){//左走
            LogMgr.d("startTurnLeftWalk");
            GaitAlgorithm.getInstance().startTurnLeftWalk();
        }
        @Override
        public void startTurnRightWalk(){//右转
            LogMgr.d("startTurnRightWalk");
            GaitAlgorithm.getInstance().startTurnRightWalk();
        }
        @Override
        public void startLeftWalk(){//左走
            LogMgr.d("startLeftWalk");
            GaitAlgorithm.getInstance().startLeftWalk();
        }
        @Override
        public void startLeftForwardWalk(){//左前走
            LogMgr.d("startLeftForwardWalk");
            GaitAlgorithm.getInstance().startLeftForwardWalk();
        }
        @Override
        public void startLeftBackwardWalk(){//左后走
            LogMgr.d("startLeftBackwardWalk");
            GaitAlgorithm.getInstance().startLeftBackwardWalk();
        }
        @Override
        public void startRightWalk(){//右走
            LogMgr.d("startRightWalk");
            GaitAlgorithm.getInstance().startRightWalk();
        }
        @Override
        public void startRightForwardWalk(){//右前走
            LogMgr.d("startRightForwardWalk");
            GaitAlgorithm.getInstance().startRightForwardWalk();
        }
        @Override
        public void startRightBackwardWalk(){//右后走
            LogMgr.d("startRightBackwardWalk");
            GaitAlgorithm.getInstance().startRightBackwardWalk();
        }
        @Override
        public void stopWalk(){//停止
            LogMgr.d("stopWalk");
            GaitAlgorithm.getInstance().stopWalk();
        }
        @Override
        public void destoryWalk(){//关闭步态算法
            LogMgr.d("destoryWalk");
            GaitAlgorithm.getInstance().destoryWalk();
        }
        @Override
        public void setWalkSpeed(int[] speed){//设置速度
            LogMgr.d("setWalkSpeed");
            GaitAlgorithm.getInstance().setWalkSpeed(speed);
        }

        @Override
        public byte[] requestOnlyUpdate(byte[] data, int time){
            LogMgr.e("requestOnlyUpdate 请求数据：" + Utils.bytesToString(data));
            return SP.requestOnlyUpdate(data, time);
        }

        @Override
        public void controlSkillPlayerWithCallBack(int state, String actionFileName, String soundFileName,
                                                   boolean isLoop,boolean isUseAutoBalance, boolean isMObstacleAvoidanceEnable,final IPlayCallBack playCallBack) throws RemoteException {
            final int MODE_STATE_STOP = 0;
            final int MODE_STATE_PLAY = 1;
            final int MODE_STATE_PAUSE = 2;
            final int MODE_STATE_RESUME = 3;
            switch (state){
                case MODE_STATE_STOP:
                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                    break;
                case MODE_STATE_PLAY:
                    PlayMoveOrSoundUtils.PlayCallBack playCallBack1 = null;
                    if(playCallBack!=null){
                        playCallBack1 = new PlayMoveOrSoundUtils.PlayCallBack() {
                            @Override
                            public void onStart(){
                                try {
                                    playCallBack.onStart();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onPause() {
                                try {
                                    playCallBack.onPause();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onResume() {
                                try {
                                    playCallBack.onResume();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onStop() {
                                try {
                                    playCallBack.onStop();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onSingleMoveStopWhileLoop() {
                                try {
                                    playCallBack.onSingleMoveStopWhileLoop();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onNormalStop() {
                                try {
                                    playCallBack.onNormalStop();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onForceStop() {
                                try {
                                    playCallBack.onForceStop();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                    }
                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(actionFileName, soundFileName, isLoop, false, 0, false,
                            PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, isUseAutoBalance, isMObstacleAvoidanceEnable, playCallBack1);
                    break;
                case MODE_STATE_PAUSE:
                    PlayMoveOrSoundUtils.getInstance().pauseCurrentMove();
                    break;
                case MODE_STATE_RESUME:
                    PlayMoveOrSoundUtils.getInstance().resumeCurrentMove();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogMgr.d("service onStartCommand and bind brainservice");
        //当Brain启动Control的时候，Control同时去绑定Brain
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogMgr.d("mMainServer destory");
    }
}
