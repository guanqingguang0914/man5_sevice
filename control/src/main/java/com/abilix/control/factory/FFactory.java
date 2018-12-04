package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.FLearnLetterCmdDisposer;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.FProtocolDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.FPatchDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.FScratchExecutor;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.FSkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.FSoulExecutor;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.AFirmwareUpgrader;
import com.abilix.control.upgrade.FFirewareUpgrade;
import com.abilix.control.upgrade.IFirmwareUpgrade;

public class FFactory extends AbstractControlFactory {
    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new FLearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new FProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new FPatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new FScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }


    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new FSkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new FSoulExecutor();
        }
        return super.createSoulExecutor();
    }

    @Override
    public IFirmwareUpgrade createFirmwareUpgrade() {
        if (mFirmwareUpgrade == null) {
            mFirmwareUpgrade = new FFirewareUpgrade();
        }
        return super.createFirmwareUpgrade();
    }

}
