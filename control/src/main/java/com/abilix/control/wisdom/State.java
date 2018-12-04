package com.abilix.control.wisdom;

import com.abilix.control.patch.PlayMoveOrSoundUtils;


/**
 * 一整套动作中的一个步骤
 *
 * @author Yang
 */
public class State {

    public static int STATE_TYPE_MOVE = 1001;
    public static int STATE_TYPE_SOUND = 1002;
    public static int STATE_TYPE_MOVE_AND_SOUND = 1003;
    public static int STATE_TYPE_IDLE = 1004;

    /**
     * 步骤ID
     */
    private int stateId;
    /**
     * 步骤类型
     */
    private int stateType;
    /**
     * 步骤动作名
     */
    private String actionName;
    /**
     * 步骤声音名
     */
    private String soundName;
    /**
     * 步骤单步时间
     */
    private long singleTime;
    /**
     * 步骤是否循环
     */
    private boolean isLoop;
    /**
     * 步骤是否可停
     */
    private boolean isStopable;

    private OnStateChangedListener onStateChangedListener;

    /**
     * @param stateId    步骤ID
     * @param stateType  步骤类型
     * @param actionName 步骤动作名
     * @param soundName  步骤声音名
     * @param singleTime 步骤单步时间
     * @param isLoop     步骤是否循环
     * @param isStopable 步骤是否可停
     */
    public State(int stateId, int stateType, String actionName, String soundName, long singleTime, boolean isLoop, boolean isStopable) {
        super();
        this.stateId = stateId;
        this.stateType = stateType;
        this.actionName = actionName;
        this.soundName = soundName;
        this.singleTime = singleTime;
        this.isLoop = isLoop;
        this.isStopable = isStopable;
    }

    /**
     * 开始步骤
     *
     * @return
     */
    public boolean startState() {
        if (onStateChangedListener != null) {
            onStateChangedListener.onStateBegin();
        }
        if (stateType == STATE_TYPE_MOVE) {
            return PlayMoveOrSoundUtils.getInstance().handlePlayCmd(getActionName(), null, isLoop(), false, 0, true, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
        } else if (stateType == STATE_TYPE_SOUND) {
            return PlayMoveOrSoundUtils.getInstance().handlePlayCmd(null, getSoundName(), isLoop(), false, 0, true, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
        } else if (stateType == STATE_TYPE_MOVE_AND_SOUND) {
            return PlayMoveOrSoundUtils.getInstance().handlePlayCmd(getActionName(), getSoundName(), isLoop(), false, 0, true, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
        } else if (stateType == STATE_TYPE_IDLE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 推荐在步骤结束时调用
     */
    public void stopState() {
        if (onStateChangedListener != null) {
            onStateChangedListener.onStateEnd();
        }
    }

    private interface OnStateChangedListener {

        void onStateBegin();

        void onStateEnd();

    }

    public int getStateId() {
        return stateId;
    }

    public void setStateId(int stateId) {
        this.stateId = stateId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getSoundName() {
        return soundName;
    }

    public void setSoundName(String soundName) {
        this.soundName = soundName;
    }

    public long getSingleTime() {
        return singleTime;
    }

    public void setSingleTime(long singleTime) {
        this.singleTime = singleTime;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean isLoop) {
        this.isLoop = isLoop;
    }


    public boolean isStopable() {
        return isStopable;
    }

    public void setStopable(boolean isStopable) {
        this.isStopable = isStopable;
    }

    public int getStateType() {
        return stateType;
    }

    public void setStateType(int stateType) {
        this.stateType = stateType;
    }

    public OnStateChangedListener getOnStateChangedListener() {
        return onStateChangedListener;
    }

    /**
     * 设置回调函数
     */
    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener;
    }
}
