package com.abilix.control.wisdom;

import com.abilix.control.utils.LogMgr;

public class ProcedureControl {

    private static ProcedureControl instance;

    private Procedure mProcedure;
    private boolean isProcedureRunning;

    private ProcedureControl() {

    }

    public static ProcedureControl getInstance() {
        if (instance == null) {
            synchronized (ProcedureControl.class) {
                if (instance == null) {
                    instance = new ProcedureControl();
                }
            }
        }
        return instance;
    }

    /**
     * 开始过程
     *
     * @param procedure
     * @return 过程是否真正开始
     */
    public synchronized boolean startProcedure(Procedure procedure) {
        LogMgr.v("ProcedureControl startProcedure()");
        if (procedure == null) {
            return false;
        }
        if (procedure.startProcedure()) {
            this.mProcedure = procedure;
            isProcedureRunning = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 过程停止后的处理
     */
    public void afterStopCurrentProcedure() {
        setmProcedure(null);
        setProcedureRunning(false);
    }

    /**
     * 获取当前过程中正在进行的步骤
     *
     * @return
     */
    public State getCurrentState() {
        if (getmProcedure() != null) {
            return getmProcedure().getStateList().get(getmProcedure().getCurrentStateId());
        }
        return null;
    }

    public Procedure getmProcedure() {
        return mProcedure;
    }

    public void setmProcedure(Procedure mProcedure) {
        this.mProcedure = mProcedure;
    }

    public boolean isProcedureRunning() {
        return isProcedureRunning;
    }

    public void setProcedureRunning(boolean isProcedureRunning) {
        this.isProcedureRunning = isProcedureRunning;
    }


}
