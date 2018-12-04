package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.IFirmwareUpgrade;

/**
 * @author jingh
 * @Descripton:IControlFactory 为工厂类的接口，定义了ControlFactory生产的所有产品
 * @date2017-3-24上午11:16:47
 */
public interface IControlFactory {
    IProtocolDisposer createProtocolDisposer(Handler handler);

    ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler);

    IPatchDisposer createPatchDisposer(Handler handler);

    IScratchExecutor createScratchExecutor(Handler handler);

    ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler);

    ISoulExecutor createSoulExecutor();

    IFirmwareUpgrade createFirmwareUpgrade();

}
