package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.C5LearnLetterCmdDisposer;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.CProtocolDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.CPatchDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.C9ScratchExecutor;
import com.abilix.control.scratch.CnewScratchExecutor;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.C5SkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.CSoulExecutor;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.AFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * Created by jingh on 2017/6/28.
 */

public class C9Factory extends AbstractControlFactory {

    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new C5LearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new CProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new CPatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            //这里对应的是C5 Scratch
            mScratchExecutor = new C9ScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }

    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new C5SkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new CSoulExecutor();
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

