package com.abilix.control.wisdom;

import java.util.List;

public abstract class Procedure {

    public static final int PROCEDURE_TYPE_WANDERPROCEDURE = 2001;

    /**
     * 过程类型
     */
    private int procedureType;
    /**
     * 当前步骤ID
     */
    private int currentStateId = -1;
    /**
     * 步骤列表
     */
    private List<State> stateList;
    /**
     * 此过程是否可循环
     */
    private boolean isLoop;
    /**
     * 此过程是否已被要求停止
     */
    private boolean isAskedToStop = false;

    /**
     * @param procedureType 过程的类型
     * @param isLoop        该过程是否循环
     */
    public Procedure(int procedureType, boolean isLoop) {
        this.procedureType = procedureType;
        this.isLoop = isLoop;
    }

    public abstract boolean startProcedure();

    public abstract void stopProcedure();

    public abstract void startNextState();

    public int getCurrentStateId() {
        return currentStateId;
    }

    public void setCurrentStateId(int currentStateId) {
        this.currentStateId = currentStateId;
    }

    public List<State> getStateList() {
        return stateList;
    }

    public void setStateList(List<State> stateList) {
        this.stateList = stateList;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean isLoop) {
        this.isLoop = isLoop;
    }

    public boolean isAskedToStop() {
        return isAskedToStop;
    }

    public void setAskedToStop(boolean isAskedToStop) {
        this.isAskedToStop = isAskedToStop;
    }

    public int getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(int procedureType) {
        this.procedureType = procedureType;
    }
}
