package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.AFLearnLetterCmdDisposer;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.AFProtocolDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.AFPatchDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.AFScratchExecutor;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.AFSkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.AFSoulExecutor;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.AFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

public class AFFactory extends AbstractControlFactory {
    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new AFLearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new AFProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new AFPatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new AFScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }


    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new AFSkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new AFSoulExecutor();
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
