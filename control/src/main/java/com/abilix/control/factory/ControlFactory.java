package com.abilix.control.factory;

import android.os.Handler;

import com.abilix.control.ControlInitiator;
import com.abilix.control.learnletter.ILearnLetterCmdDisposer;
import com.abilix.control.pad.IProtocolDisposer;
import com.abilix.control.patch.IPatchDisposer;
import com.abilix.control.scratch.IScratchExecutor;
import com.abilix.control.skillplayer.ISkillPlayerCmdDisposer;
import com.abilix.control.soul.ISoulExecutor;
import com.abilix.control.upgrade.IFirmwareUpgrade;
import com.abilix.control.utils.LogMgr;

/**
 * @author jingh
 * @Descripton:这是Contro工厂类对外的接口，所有调用者都只能使用这个类提供的接口。
 * @date2017-3-24下午3:42:16
 */
public class ControlFactory {

    private static IControlFactory mControlFactory;

    /**
     * @param robotType 使用工厂生产之前，必须先建造好工厂。
     */
    public static void buildFactory(int robotType) {
        LogMgr.d("Factory type::" + robotType);
        if (robotType < 1) {
            return;
        }
        switch (robotType) {
            case ControlInitiator.ROBOT_TYPE_C:
            case ControlInitiator.ROBOT_TYPE_CU:
                mControlFactory = new C5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_BRIANC:
            case ControlInitiator.ROBOT_TYPE_C1_2:
                mControlFactory = new C1Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M:
                mControlFactory = new M5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_H:
            case ControlInitiator.ROBOT_TYPE_H3:
            case ControlInitiator.ROBOT_TYPE_H5:
            case ControlInitiator.ROBOT_TYPE_SE901:
                mControlFactory = new H5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_F:
                mControlFactory = new FFactory();
                break;
            case ControlInitiator.ROBOT_TYPE_AF:
                mControlFactory = new AFFactory();
                break;
            case ControlInitiator.ROBOT_TYPE_M1:
                mControlFactory = new M1Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M2:
                mControlFactory = new M1Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M3:
                mControlFactory = new M5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M4:
                mControlFactory = new M5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M5:
            case ControlInitiator.ROBOT_TYPE_M3S:
            case ControlInitiator.ROBOT_TYPE_M4S:
                mControlFactory = new M5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_M6:
                mControlFactory = new M5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_S:
            case ControlInitiator.ROBOT_TYPE_S3:
            case ControlInitiator.ROBOT_TYPE_S4:
            case ControlInitiator.ROBOT_TYPE_S7:
            case ControlInitiator.ROBOT_TYPE_S8:
                mControlFactory = new S5Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_C9:
                mControlFactory = new C9Factory();
                break;
            case ControlInitiator.ROBOT_TYPE_U:
            case ControlInitiator.ROBOT_TYPE_U5:
                mControlFactory = new U5Factory();
                break;
            default:
                break;
        }
    }

    public static IProtocolDisposer createProtocolDisposer(Handler handler) {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
        }
        return mControlFactory.createProtocolDisposer(handler);
    }

    public static ILearnLetterCmdDisposer createLearnLetterCmdDisposer(Handler handler) {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
        }
        return mControlFactory.createLearnLetterCmdDisposer(handler);
    }

    public static IPatchDisposer createPatchDisposer(Handler handler) {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
            return null;
        }
        return mControlFactory.createPatchDisposer(handler);
    }

    public static IScratchExecutor createScratchExecutor(Handler handler) {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
            return null;
        }
        return mControlFactory.createScratchExecutor(handler);
    }

    public static ISkillPlayerCmdDisposer createSkillPlayerCmdDisposer(Handler handler) {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
            return null;
        }
        return mControlFactory.createSkillPlayerCmdDisposer(handler);
    }

    public static ISoulExecutor createSoulExecutor() {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
            return null;
        }
        return mControlFactory.createSoulExecutor();
    }

    public static IFirmwareUpgrade createFirmwareUpgrade() {
        if (mControlFactory == null) {
            LogMgr.e("Control 工厂还没被建造");
            return null;
        }
        return mControlFactory.createFirmwareUpgrade();
    }

}
