package com.abilix.control.patch;

import android.os.Handler;


public abstract class AbstractPatchDisposer implements IPatchDisposer {

    protected Handler mHandler;

    public AbstractPatchDisposer(Handler mHandler) {
        this.mHandler = mHandler;
    }


}
