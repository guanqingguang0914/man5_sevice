package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.HLearnLetterCmdDisposer;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.HProtocolDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.HPatchDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.HScratchExecutor;
import com.abilix.control.scratch.IScratchExecutor;

import com.abilix.control.skillplayer.HSkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.HSoulExecutor;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.BFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

public class H5Factory extends AbstractControlFactory {

    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new HLearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new HProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new HPatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new HScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }

    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new HSkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new HSoulExecutor();
        }
        return super.createSoulExecutor();
    }

    @Override
    public IFirmwareUpgrade createFirmwareUpgrade() {
        if (mFirmwareUpgrade == null) {
            mFirmwareUpgrade = new BFirmwareUpgrader();
        }
        return super.createFirmwareUpgrade();
    }
}
