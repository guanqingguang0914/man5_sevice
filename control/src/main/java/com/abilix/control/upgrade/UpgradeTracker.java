package com.abilix.control.upgrade;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.abilix.control.BroadcastResponder;
import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.IControlKenel;
import com.abilix.control.R;
import com.abilix.control.ResponseBean;
import com.abilix.control.aidl.Control;
import com.abilix.control.factory.ControlFactory;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class UpgradeTracker {
    // 为兼容updateonlinetest 保留
    public static final String TYPE_C = "type_c"; //C系列 BrainB
    public static final String TYPE_M = "type_m"; //M系列M3~6
    public static final String TYPE_KC = "type_kc"; //C系列 BrainC
    public static final String TYPE_KA = "type_ka"; //C系列 BrainA
    public static final String TYPE_MB = "type_mb"; //M系列M1M2
    public static final String TYPE_H = "type_h"; //H系列
    public static final String TYPE_H3 = "type_h3"; //H系列
    public static final String TYPE_SE901 = "type_se9"; //SE901系列
    public static final String TYPE_F = "type_f"; //F系列
    public static final String TYPE_S = "type_s";
    public static final String TYPE_AF = "type_af";
    public static final String TYPE_C9 = "type_c9";
    public static final String TYPE_CU = "type_cu";
    public static final String TYPE_MS = "type_ms";//m3s,m4s
    public static final String TYPE_U = "type_u";//U5系列

    /* 系统版本号 */
    private final static String ROBOT_BUILD_TYPE2 = Build.DISPLAY;
    private static final int VISION_FUNCTION_SE901 = 3;

    public static final int QUERY_STM_VERSION = 0X00;
    public static final int UPGRADE_STM_VERSION = 0X01;
    public static final int SAVE_STM_VERSION = 0X02;
    public static final int SAVE_SERVO_VERSION = 0X04;
    public static final int SEND_ELF_FILE = 0X03;
    public static final int UPGRADE_SUCESS = 0X01;
    public static final int UPGRADE_FAILED = 0X00;
    private static UpgradeTracker instance = null;
    private final static Object mLock = new Object();
    private IControlKenel mIControl;
    private HandlerThread doUpgradeCmdThread;
    private DoUpgradeCmdThreadHandler doUpgradecmdThreadHandler;
    private IFirmwareUpgrade mIFirmwareUpgrade;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            ResponseBean responseBean = (ResponseBean) msg.obj;
            try {
                mIControl.doUpgradeCmdCallBack(responseBean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static UpgradeTracker getInstance() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new UpgradeTracker();
                }
            }
        }
        return instance;
    }

    private UpgradeTracker() {
        doUpgradeCmdThread = new HandlerThread("doUpgradeCmdThread");
        doUpgradeCmdThread.start();
        doUpgradecmdThreadHandler = new DoUpgradeCmdThreadHandler(doUpgradeCmdThread.getLooper());
        mIFirmwareUpgrade = ControlFactory.createFirmwareUpgrade();
    }

    public void doUpgradeCmd(Control control, IControlKenel iControl) {
        LogMgr.e("do upgrade cmd");
        doUpgradecmdThreadHandler.removeCallbacksAndMessages(null);
        Message msg = doUpgradecmdThreadHandler.obtainMessage();
        if (control.getControlFuncType() == 9) {
            LogMgr.d("query stm version");
            msg.what = QUERY_STM_VERSION;
        } else {
            msg.obj = control.getFileFullPath();
            LogMgr.d("update stm,update file::" + control.getFileFullPath());
            msg.what = UPGRADE_STM_VERSION;
        }
        this.mIControl = iControl;
        boolean isSendSuccess = doUpgradecmdThreadHandler.sendMessage(msg);
        LogMgr.d("is send message sucess:" + isSendSuccess);
    }

    public synchronized void saveSTMVersion() {
        LogMgr.e("do saveSTMVersion cmd");
        Message msg = doUpgradecmdThreadHandler.obtainMessage();
        msg.what = SAVE_STM_VERSION;
        boolean isSendSuccess = doUpgradecmdThreadHandler.sendMessage(msg);
        LogMgr.d("is send message sucess:" + isSendSuccess);
    }
    public synchronized void saveSERVOVersion() {
        LogMgr.e("do saveSERVOVersion cmd");
        Message msg = doUpgradecmdThreadHandler.obtainMessage();
        msg.what = SAVE_SERVO_VERSION;
        boolean isSendSuccess = doUpgradecmdThreadHandler.sendMessage(msg);
        LogMgr.d("is send message sucess:" + isSendSuccess);
    }

    public synchronized void sendElfFile(Control control, IControlKenel iControl) {
        LogMgr.e("do sendElfFile cmd");
        doUpgradecmdThreadHandler.removeCallbacksAndMessages(null);
        Message msg = doUpgradecmdThreadHandler.obtainMessage();
        msg.obj = control.getFileFullPath();
        msg.what = SEND_ELF_FILE;
        LogMgr.d("send elf file ,elf file::" + control.getFileFullPath());
        this.mIControl = iControl;
        doUpgradecmdThreadHandler.sendMessage(msg);
    }

    private class DoUpgradeCmdThreadHandler extends Handler {
        public DoUpgradeCmdThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogMgr.e("handle message");
            ResponseBean responseBean = new ResponseBean();
            int mode = msg.what;
            switch (mode) {
                case QUERY_STM_VERSION:
                    LogMgr.d("查询stm32版本开始");
                    //jingh add 查询版本号之前不用发让STM32不休息的指令
                    //STM32Mgr.setSTM32Sleep(false);
                    //LogMgr.d("set stm32 weak before query stm32 version");
                    if (SP.getUpdateState() == Utils.STM32_STATUS_UPGRADING){ //正在进行固件升级，不进行读取版本号的操作
                        return;
                    }
                    int version = mIFirmwareUpgrade.getVersion((byte) ControlInfo.getMain_robot_type());
                    responseBean.setMode(9);
                    String robotType = "";
                    if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C) {
                        robotType = TYPE_C;
                        if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
                            robotType = TYPE_C9;
                        } else if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_CU) {
                            robotType = TYPE_CU;
                        }
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
                        robotType = TYPE_C9;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_CU) {
                        robotType = TYPE_CU;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M) {
                        robotType = TYPE_M;
                        if(ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_M3S || ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_M4S){
                            robotType = TYPE_MS;
                        }
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_BRIANC) {
                        robotType = TYPE_KC;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1) {
                        robotType = TYPE_MB;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                        robotType = TYPE_H;
                        if(Utils.getFunctionVersionNumber(ROBOT_BUILD_TYPE2) == VISION_FUNCTION_SE901){
                            robotType = TYPE_SE901;
                        }
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
                        robotType = TYPE_H3;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_F) {
                        robotType = TYPE_F;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S) {
                        robotType = TYPE_S;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_AF) {
                        robotType = TYPE_AF;
                    } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_U) {
                        robotType = TYPE_U;
                    } else {
                        LogMgr.e("主类型错误");
                    }
                    responseBean.setStringType(robotType);
                    responseBean.setIntVersion(version);

                    LogMgr.d("response::" + responseBean.toString());
                    Message result_msg = mHandler.obtainMessage();
                    result_msg.obj = responseBean;
                    mHandler.sendMessage(result_msg);
                    LogMgr.d("查询stm32版本结束");
                    break;

                case UPGRADE_STM_VERSION:
                    LogMgr.d("升级stm32版本开始");
                    byte upgradeResult = 0x02;
                    String upgradePath = (String) msg.obj;
                    LogMgr.d("upgradePath::" + upgradePath);
                    if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
                        //K9状态在upgradeByZmodem()中设置
                    } else {
                        SP.setUpdateState(Utils.STM32_STATUS_UPGRADING);
                    }
                    boolean isUpgradeSucess = mIFirmwareUpgrade.upgrade((byte) ControlInfo.getMain_robot_type(), upgradePath);
                    if (upgradePath.contains("updateFile")) {
                        if (isUpgradeSucess) {
                            Toast.makeText(ControlApplication.instance, ControlApplication.instance.getString(R.string.update_sucess), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ControlApplication.instance, ControlApplication.instance.getString(R.string.update_fail), Toast.LENGTH_SHORT).show();
                        }
                    }
                    responseBean.setUpgradeFilePath(upgradePath);
                    responseBean.setMode(10);
                    if (isUpgradeSucess) {
                        upgradeResult = UPGRADE_SUCESS;
                    } else {
                        upgradeResult = UPGRADE_FAILED;
                    }
                    responseBean.setUpgradeResult(upgradeResult);

                    LogMgr.d("response::" + responseBean.toString());
                    Message version_msg = mHandler.obtainMessage();
                    version_msg.obj = responseBean;
                    mHandler.sendMessage(version_msg);
                    SP.setUpdateState(Utils.STM32_STATUS_NORMAL);
                    LogMgr.d("升级stm32版本结束");
                    break;
                case SAVE_STM_VERSION:
                    LogMgr.d("保存stm32版本开始");
                    int stmVersion = mIFirmwareUpgrade.getVersion((byte) ControlInfo.getMain_robot_type());
                    LogMgr.d("save stm version to sdcard version::" + stmVersion);
                    FileUtils.saveFile(stmVersion + "", FileUtils.STMVERSION_PATH);
                    LogMgr.d("保存stm32版本结束");
                    break;
                case SAVE_SERVO_VERSION:
                    LogMgr.d("保存servo舵机版本开始");
                    int servoVersion = mIFirmwareUpgrade.getSERVOVersion((byte) ControlInfo.getMain_robot_type());
                    LogMgr.d("save stm version to sdcard version::" + servoVersion);
                    BroadcastResponder.sendServoVersionToBrainset(servoVersion);
                    LogMgr.d("保存servo舵机版本结束");
                    break;
                case SEND_ELF_FILE:
                    LogMgr.d("发送elf文件开始");
                    String elf_filePath = (String) msg.obj;
                    LogMgr.d("elf_filePath::" + elf_filePath);
                    boolean isTransSucess= mIFirmwareUpgrade.transFileToStm32((byte) ControlInfo.getChild_robot_type(), elf_filePath);
                    if(isTransSucess){
                        //因为有可能出现elf文件先执行而注册上报事件晚 会导致出现问题
                        //目前Brain代码还会作较大调整，目前这个问题先临时这么处理
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(ControlApplication.instance, ControlApplication.instance.getString(R.string.download_sucess), Toast.LENGTH_SHORT).show();
                        String[] content = elf_filePath.split("/");
                        LogMgr.e("content length:" + content.length);
                        String fileName = content[content.length - 1];
                        LogMgr.d("excute fileName:" + fileName);
                        byte[] fileNameBytes = fileName.getBytes();
                        byte[] data = new byte[5 + fileNameBytes.length];
                        data[0] = (byte) 0x01;
                        data[1] = (byte) 0x02;
                        data[4] = (byte) fileNameBytes.length;
                        System.arraycopy(fileNameBytes, 0, data, 5, fileNameBytes.length);
                        LogMgr.d("excute cmd:" + Utils.bytesToString(data));
                        byte[] sendBytes = ProtocolBuilder.buildProtocol((byte) ControlInfo.getChild_robot_type(), ProtocolBuilder.CMD_FILE_EXCUTE, data);
                        LogMgr.d("excute elf");
                        byte[] response = SP.request(sendBytes, 500);
                        if (response != null && response.length > 12 && response[12] == ProtocolBuilder.EXECUTE_ERROR) {
                            LogMgr.e("执行elf文件不成功，再给nuttx发一次文件");
                            boolean isTransAgainSucess = mIFirmwareUpgrade.transFileToStm32((byte) ControlInfo.getChild_robot_type(), elf_filePath);
                            if (isTransAgainSucess) {
                                LogMgr.d("excute elf");
                                ProtocolBuilder.sendProtocol((byte) ControlInfo.getChild_robot_type(), ProtocolBuilder.CMD_FILE_EXCUTE, data);
                            }
                        }
                    }
                    LogMgr.d("发送elf文件结束");
                    //配合nutx临时测试
/*                       if(true){
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String[] content=elf_filePath.split("/");
                        LogMgr.e("content length:"+content.length);
                        String fileName=content[content.length-1];
                        LogMgr.d("excute fileName:"+fileName);
                        byte[] fileNameBytes=fileName.getBytes();
                        byte[] data=new byte[5+fileNameBytes.length];
                        data[0]=(byte)0x01;
                        data[1]=(byte)0x02;
                        data[4]=(byte)fileNameBytes.length;
                        System.arraycopy(fileNameBytes,0,data,5,fileNameBytes.length);
                        LogMgr.d("excute cmd:"+ Utils.bytesToString(data,data.length));
                        ProtocolBuilder.sendProtocol((byte) ControlInfo.getChild_robot_type(),ProtocolBuilder.CMD_FILE_EXCUTE,data);
                    }*/
                    break;
                default:
                    break;
            }

        }

    }
}
