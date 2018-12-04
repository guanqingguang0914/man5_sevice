package com.abilix.control;

import android.content.Intent;
import android.content.IntentFilter;

import com.abilix.control.aidl.Brain;
import com.abilix.control.aidl.Control;
import com.abilix.control.learnletter.LearnLetterTracker;
import com.abilix.control.model.Model;
import com.abilix.control.pad.HProtocolDisposer;
import com.abilix.control.pad.PadTracker;
import com.abilix.control.patch.PatchTracker;
import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.scratch.ScratchTracker;
import com.abilix.control.skillplayer.SkillPlayerTracker;
import com.abilix.control.soul.SoulTracker;
import com.abilix.control.upgrade.UpgradeTracker;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.moco.gaitx.kernal.execute.Executor;
import com.abilix.moco.gaitx.kernal.execute.GaitAlgorithmForH5;
import com.abilix.robot.walktunner.GaitAlgorithm;

public class ControlKernel implements IControlKenel, IInitCallback {
    private static ControlKernel instance = new ControlKernel();
    private BroadcastResponder mBroadcastResponder;
    private ControlReceiveBroadcastReceiver mBroadcastReceiver;
    private final static int SEND_ELF_FILE = 17;

    private ControlKernel() {
        mBroadcastResponder = new BroadcastResponder();
        registerBroadcastReceiver();
        LogMgr.d("initial controlkenel");
    }

    public static ControlKernel getInstance() {
        return instance;
    }

    @Override
    public void dispatchCmd(Control control) {
        LogMgr.d("receive cmd from brain cmd mode::" + control.getControlFuncType());
        switch (control.getControlFuncType()) {
            case 0:
                LogMgr.d("receive pad cmd from brain");
                PadTracker.getInstance().doPadCmd(control.getSendByte(), this);
                break;
            case 2:
                LogMgr.d("receive soul cmd from brain");
                SoulTracker.getInstance().doSoulCmd(control.getModeState(), this);
                break;
            case 3:
                LogMgr.d("receive vjc cmd from brain");
        /*if (control.getModeState() == 0) {
                VjcTracker.getInstance().doVjcCmd(null, this);
			} else {
				LogMgr.e("control.getFileFullPath() is:   " + control.getFileFullPath());
				VjcTracker.getInstance().doVjcCmd(control, this);
			}*/
                break;
            case 4:
                LogMgr.d("receive scratch cmd from brain");
                ScratchTracker.getInstance().doScratchCmd(control.getSendByte(), this);
                break;
            case 5:
                LogMgr.d("receive skillplayer cmd from brain");
                SkillPlayerTracker.getInstance().doSkillPlayerCmd(control.getSendByte(), control.getModeState(), this);
                break;
            case 6:
                LogMgr.d("receive robot type cmd from brain");
                BrainResponder.initBrainResponder(this);
                break;
            case 7:
                LogMgr.d("receive sleep or weak cmd from brain");
                // sleepOrWeak(control);
                PatchTracker.getInstance().doPatchCmd(control, this);
                break;
            case 8:
                LogMgr.d("receive project cmd from brain");
/*			if (control.getModeState() == 0) {
                //VjcTracker.getInstance().doVjcCmd(null, this);
			} else {
				// 这个先屏蔽。
				//VjcTracker.getInstance().doVjcCmd(control, this);
			}*/
                break;
            case 9:
                LogMgr.d("receive query stm version cmd from upgrade app");
                UpgradeTracker.getInstance().doUpgradeCmd(control, this);
                break;
            case 10:
                LogMgr.d("receive upgrade cmd from upgrade app");
                UpgradeTracker.getInstance().doUpgradeCmd(control, this);
                break;
            case 11:
                LogMgr.d("receive turnoff cmd from brain");
                PatchTracker.getInstance().doPatchCmd(control, this);
                break;
            case 12:
                LogMgr.d("receive Scratch cmd from brain");
                PatchTracker.getInstance().doPatchCmd(control, this);
                break;
            case 13:
                LogMgr.d("receive project stop cmd from brain1");
                if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H){
                    GaitAlgorithmForH5.getInstance().stopGait();
                }
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                PadTracker.getInstance().removeCmd();
                if(HProtocolDisposer.isInStepState){
                    GaitAlgorithm.getInstance().stopWalk();// 停止步态运动
                    HProtocolDisposer.isInStepState =false;
                }
                //xiongxin@20171124 add start
                //为CREATOR停止舵机固定
                if (HProtocolDisposer.H5ServoFixed.getInstance().status())
                    HProtocolDisposer.H5ServoFixed.getInstance().stopServoFixed();
                //add end
                PadTracker.getInstance().stopCmd();
                byte[] data = new byte[2];// Scratch 调用停止。    addTag:最新版本测试，先暂时注掉，之后再加上 10-9
                ScratchTracker.getInstance().doScratchCmd(data, this);
                Model.getInstance().onDestroy();
                LogMgr.d("receive project stop cmd from brain7");
                break;
            case 14:
                LogMgr.d("receive learn letter cmd from other app");
                LearnLetterTracker.getInstance().doLearnLetterCmd(control.getModeState(), control.getFileFullPath(), this);
                break;
            case 15:
                LogMgr.d("receive patch cmd from other app");
                PatchTracker.getInstance().doPatchCmd(control, this);
                break;
            case 16:
                LogMgr.d("receive battery cmd from control");
                PatchTracker.getInstance().doPatchCmd(control, this);
                break;

            case SEND_ELF_FILE:
                LogMgr.d("receive send elf file cmd from brain");
                UpgradeTracker.getInstance().sendElfFile(control, this);
                break;
            default:
                break;
        }

    }

    @Override
    public void responseCmdToBrain(Brain brain) {
        LogMgr.i("responseCmdToBrain()");
        BrainResponder.getInstance().responsetToBrain(brain);
    }

    @Override
    public void responseCmdToUpgrade(ResponseBean responseBean) {
        switch (responseBean.getMode()) {
            case 9:
                String type = responseBean.getStringType();
                int version = responseBean.getIntVersion();
                mBroadcastResponder.sendRobotInfoBroadCastToService(version, type);
                break;
            case 10:
                String filePath = responseBean.getUpgradeFilePath();
                byte upgradeResult = responseBean.getUpgradeResult();
                mBroadcastResponder.sendStm32UpdateStateBroadCastToService(upgradeResult, filePath);
                break;
            default:
                break;
        }
    }

    @Override
    public void doPadCmdCallBack(byte[] buff_resposne) {
        LogMgr.d("response pad cmd to brain");
        Brain padBrain = new Brain(0, buff_resposne);
        responseCmdToBrain(padBrain);

    }

    @Override
    public void doPatchCmdCallBack(Brain patchBrain) {
        LogMgr.d("doPatchCmdCallBack response to brain");
        responseCmdToBrain(patchBrain);

    }

    @Override
    public void doScratchCmdCallBack(byte[] buff_resposne) {
        Brain scratchBrain = new Brain(0, buff_resposne);
        responseCmdToBrain(scratchBrain);
    }

    @Override
    public void doSkillPlayerCmdCallBack(byte[] buff_resposne) {
        Brain skillplayerBrain = new Brain(0, buff_resposne);
        responseCmdToBrain(skillplayerBrain);
    }

    @Override
    public void doLearnLetterCmdCallBack(byte[] buff_resposne) {
        Brain learnLetterBrain = new Brain(0, buff_resposne);
        responseCmdToBrain(learnLetterBrain);
    }

    @Override
    public void doVjcCmdCallBack(Brain vjcBrain) {
        responseCmdToBrain(vjcBrain);
    }

    @Override
    public void doScratchCmdCallBack(Brain scratchBrain) {
        responseCmdToBrain(scratchBrain);
    }

    @Override
    public void doSoulCmdCallBack(byte[] buff_resposne) {
        Brain soulBrain = new Brain(0, buff_resposne);
        responseCmdToBrain(soulBrain);

    }

    @Override
    public void doUpgradeCmdCallBack(ResponseBean responseBean) {
        responseCmdToUpgrade(responseBean);
    }

    @Override
    public void onDestory() {
    }

    private void registerBroadcastReceiver() {
        mBroadcastReceiver = new ControlReceiveBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ControlReceiveBroadcastReceiver.ACTION_STM32_VERSION_QUERY);
        filter.addAction(ControlReceiveBroadcastReceiver.ACTION_SERVO_VERSION_QUERY);
        filter.addAction(ControlReceiveBroadcastReceiver.BROADCAST_ACTION_LOG);
        LogMgr.d("register charge state change broadcast");
        ControlApplication.instance.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void dispatchSkillPlayerCmd(int state, String filePath) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] serialWrite(byte[] data) {
        // TODO Auto-generated method stub
        return null;
    }

    //绑定Brain成功，返回机器人具体类型
    @Override
    public void onSucess() {
        byte[] robotType = Utils.intTo4Bytes(ControlInfo.getChild_robot_type());
        Brain robotTypeBrain = new Brain(2, robotType);
        robotTypeBrain.setModeState(13);
        responseCmdToBrain(robotTypeBrain);

    }

}
