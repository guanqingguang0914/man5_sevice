package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.learnletter.S5LearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.pad.S5ProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.patch.S5PatchDisPoser;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.scratch.S5ScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.skillplayer.S5SkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.soul.S5SoulExecutor;
import com.abilix.control.upgrade.BFirmwareUpgrader;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * @author jingh
 * @Descripton:S5工厂类，生产Control运行所需的具体的任务执行者，例如protocolDisposer、scratchExecutor等。
 * @date2017-3-24下午2:25:01
 */
public class S5Factory extends AbstractControlFactory {
    @Override
    public ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mLearnLetterCmdDisposer == null) {
            mLearnLetterCmdDisposer = new S5LearnLetterCmdDisposer(handler);
        }
        return super.createLearnLetterCmdDisposer(handler);
    }

    @Override
    public IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mProtocolDisposer == null) {
            mProtocolDisposer = new S5ProtocolDisposer(handler);
        }
        return super.createProtocolDisposer(handler);
    }

    @Override
    public IPatchDisposer createPatchDisposer(Handler handler) {
        if (mPatchDisposer == null) {
            mPatchDisposer = new S5PatchDisPoser(handler);
        }
        return super.createPatchDisposer(handler);
    }

    @Override
    public IScratchExecutor createScratchExecutor(Handler handler) {
        if (mScratchExecutor == null) {
            mScratchExecutor = new S5ScratchExecutor(handler);
        }
        return super.createScratchExecutor(handler);
    }

    @Override
    public ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mSkillPlayerCmdDisposer == null) {
            mSkillPlayerCmdDisposer = new S5SkillPlayerCmdDisposer(handler);
        }
        return super.createSkillPlayerCmdDisposer(handler);
    }

    @Override
    public ISoulExecutor createSoulExecutor() {
        if (mSoulExecutor == null) {
            mSoulExecutor = new S5SoulExecutor();
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
