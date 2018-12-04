package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.learnletter.MLearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.pad.MProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.patch.MPatchDisposer;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.scratch.MScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.MSkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.soul.MSoulExecutor;
import com.abilix.control.upgrade.AFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

public class M5Factory extends AbstractControlFactory {

    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new MLearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new MProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new MPatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new MScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }

    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new MSkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new MSoulExecutor();
        }
        return super.createSoulExecutor();
    }

    @Override
    public IFirmwareUpgrade createFirmwareUpgrade() {
        if (mFirmwareUpgrade == null) {
            mFirmwareUpgrade = new AFirmwareUpgrader();
        }
        return super.createFirmwareUpgrade();
    }
}
