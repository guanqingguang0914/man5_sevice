package com.abilix.control.factory;


import android.os.Handler;

import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.sp.ISerial;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * @author jingh
 * @Descripton:Control抽象工厂类，是不同具体工程类的父类
 * @date2017-3-24下午3:36:28
 */
public abstract class AbstractControlFactory implements IControlFactory {
    protected IProtocolDisposer mProtocolDisposer;
    protected ILearnLetterCmdDisposer mLearnLetterCmdDisposer;
    protected IPatchDisposer mPatchDisposer;
    protected IScratchExecutor mScratchExecutor;
    protected ISkillPlayerCmdDisposer mSkillPlayerCmdDisposer;
    protected ISoulExecutor mSoulExecutor;
    protected IFirmwareUpgrade mFirmwareUpgrade;
    protected ISerial mSerial;

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        return mProtocolDisposer;
    }

    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        // TODO Auto-generated method stub
        return mLearnLetterCmdDisposer;
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        // TODO Auto-generated method stub
        return mPatchDisposer;
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        // TODO Auto-generated method stub
        return mScratchExecutor;
    }


    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        // TODO Auto-generated method stub
        return mSkillPlayerCmdDisposer;
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        // TODO Auto-generated method stub
        return mSoulExecutor;
    }

    @Override
    public IFirmwareUpgrade createFirmwareUpgrade() {
        // TODO Auto-generated method stub
        return mFirmwareUpgrade;
    }

}
