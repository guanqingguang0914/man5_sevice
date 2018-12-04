package com.abilix.control.skillplayer;

import android.os.Handler;

public abstract class AbstractSkillPlayerCmdDisposer implements ISkillPlayerCmdDisposer {

    protected Handler mHandler;

    public AbstractSkillPlayerCmdDisposer(Handler mHandler) {
        this.mHandler = mHandler;
    }

}
