package com.abilix.control.wisdom;

import android.os.Environment;

import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WanderProcedure extends Procedure {

    private static final String MOVE_WALK_F_BEG = "Walk_F_beg.bin";
    private static final String MOVE_WALK_F_MID = "Walk_F_mid.bin";
    private static final String MOVE_WALK_F_END = "Walk_F_end.bin";
    private static final String MOVE_TURN_LEFT = "Turn_Left.bin";
    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + File.separator;

    private long mWavePerid = 200;
    private Timer mWaveTimer;
    private TimerTask mWaveTimerTask;
    private byte[] waveByteArray;
    private int waveCount;


    public WanderProcedure() {
        super(Procedure.PROCEDURE_TYPE_WANDERPROCEDURE, false);
        List<State> wanderProcedureStateList = new ArrayList<State>();
        State state_Walk_F_beg = new State(0, State.STATE_TYPE_MOVE, PATH + MOVE_WALK_F_BEG, null, 0, false, false);
        State state_Walk_F_mid = new State(1, State.STATE_TYPE_MOVE, PATH + MOVE_WALK_F_MID, null, 0, true, false);
        State state_Walk_F_end = new State(2, State.STATE_TYPE_MOVE, PATH + MOVE_WALK_F_END, null, 0, false, true);
//		State state_Turn_Left  = new State(3, State.STATE_TYPE_MOVE, PATH + MOVE_TURN_LEFT, null, 0, false, true);
        wanderProcedureStateList.add(state_Walk_F_beg);
        wanderProcedureStateList.add(state_Walk_F_mid);
        wanderProcedureStateList.add(state_Walk_F_end);
//		wanderProcedureStateList.add(state_Turn_Left);
        setStateList(wanderProcedureStateList);
        waveByteArray = new byte[20];
        waveByteArray[0] = (byte) 0xAA;
        waveByteArray[1] = (byte) 0x54;
        for (int i = 2; i < 20; i++) {
            waveByteArray[i] = (byte) 0x00;
        }
    }

    @Override
    public boolean startProcedure() {
        LogMgr.v("WanderProcedure startProcedure()");
        if (getStateList().get(0).startState()) {
            setCurrentStateId(0);
            LogMgr.v("WanderProcedure startProcedure() 成功");
            return true;
        }
        LogMgr.v("WanderProcedure startProcedure() 失败");
        return false;
    }

    @Override
    public void stopProcedure() {
        setAskedToStop(true);
    }

    @Override
    public synchronized void startNextState() {
        getStateList().get(getCurrentStateId()).stopState();
        if (isAskedToStop() == true && getStateList().get(getCurrentStateId()).isStopable() == true) {
            //停止过程
            LogMgr.d("过程被要求停止，切当前步骤可以停止，故停止");
            ProcedureControl.getInstance().afterStopCurrentProcedure();
            return;
        }
        if (getCurrentStateId() + 1 == getStateList().size()) {
            //已到达列表中的最后一个步骤
            LogMgr.d("已到达列表中的最后一个步骤");
            if (isLoop()) {
                //当前过程是循环的 从头开始
                LogMgr.d("当前过程是循环的 从头开始");
                setCurrentStateId(0);
                getStateList().get(0).startState();
            } else {
                //当前过程不是循环的 停止过程
                LogMgr.d("当前过程不是循环的 停止过程");
                ProcedureControl.getInstance().afterStopCurrentProcedure();
            }
        } else {
            LogMgr.d("当前步骤" + getCurrentStateId() + "结束");
            setCurrentStateId(getCurrentStateId() + 1);
            LogMgr.d("新步骤" + getCurrentStateId() + "开始");
            getStateList().get(getCurrentStateId()).startState();
            if (isWaveOnState(getCurrentStateId())) {
                startWaveTimer();
            } else {
                stopWaveTimer();
            }

        }

    }

    private void stopWaveTimer() {
        if (mWaveTimer != null) {
            mWaveTimer.cancel();
        }
        if (mWaveTimerTask != null) {
            mWaveTimerTask.cancel();
        }
    }

    private void startWaveTimer() {
        stopWaveTimer();
        waveCount = 0;
        mWaveTimer = new Timer();
        mWaveTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    byte[] buffer = SP.request(waveByteArray);
                    // 超声波的情况 需要返回数据
                    if (buffer[0] == (byte) 0xAA && buffer[1] == (byte) 0x54) {
                        int distance = (int) (buffer[2] & 0xFF);
                        LogMgr.d("超声波数据 = " + distance);
                        if (distance > 0 && distance < 50) {
                            waveCount++;
                            if (waveCount >= 5 * 1000 / mWavePerid) {
                                //有壁障 5秒内都小于30
                                stopWaveTimer();
                                startNextState();
                            }
                        } else {
                            waveCount = 0;
                        }
                    } else {
                        LogMgr.e("超声波返回数据错误");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mWaveTimer.schedule(mWaveTimerTask, 20, mWavePerid);
    }

    /**
     * 该步骤是否需要开启超声波
     *
     * @param currentStateId
     * @return
     */
    private boolean isWaveOnState(int currentStateId) {
        if (currentStateId == 1) {
            return true;
        }
        return false;
    }

}
