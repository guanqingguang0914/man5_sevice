package com.abilix.control;

import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.Control;

public interface IControlKenel {
    void dispatchCmd(Control control);

    void responseCmdToBrain(Brain brain);

    void responseCmdToUpgrade(ResponseBean responseBean);

    void doPadCmdCallBack(byte[] buff_resposne);

    void doPatchCmdCallBack(Brain patchBrain);

    void doScratchCmdCallBack(byte[] buff_resposne);

    void doScratchCmdCallBack(Brain scratchBrain);

    void doSkillPlayerCmdCallBack(byte[] buff_resposne);

    void doLearnLetterCmdCallBack(byte[] buff_resposne);

    void doVjcCmdCallBack(Brain vjcBrain);

    void doSoulCmdCallBack(byte[] buff_resposne);

    void doUpgradeCmdCallBack(ResponseBean responseBean);

    void onDestory();

    void dispatchSkillPlayerCmd(int state, String filePath);

    byte[] serialWrite(byte[] data);
}
