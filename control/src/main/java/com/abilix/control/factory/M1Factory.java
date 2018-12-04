package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.learnletter.M1LearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.pad.M1ProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.patch.M1PatchDisposer;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.scratch.M1ScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.M1SkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.soul.M1SoulExecutor;
import com.abilix.control.upgrade.AFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * @author jingh
 * @Descripton:M1工厂类，生产Control运行所需的具体的任务执行者，例如protocolDisposer、scratchExecutor等。
 * @date2017-3-24下午2:25:01
 */
public class M1Factory extends AbstractControlFactory {

    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new M1LearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new M1ProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new M1PatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new M1ScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }

    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new M1SkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new M1SoulExecutor();
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
