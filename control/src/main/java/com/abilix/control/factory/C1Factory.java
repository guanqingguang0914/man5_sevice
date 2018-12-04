package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.C1LearnLetterCmdDisposer;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.CXProtocolDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.C1PatchDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.CXScratchExecutor;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.C1SkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.C1SoulExecutor;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.BFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * @author jingh
 * @Descripton:C1工厂类，生产Control运行所需的具体的任务执行者，例如protocolDisposer、scratchExecutor等。
 * @date2017-3-24下午2:25:01
 */
public class C1Factory extends AbstractControlFactory {
    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new C1LearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new CXProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new C1PatchDisposer(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new CXScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }



    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new C1SkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new C1SoulExecutor();
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
