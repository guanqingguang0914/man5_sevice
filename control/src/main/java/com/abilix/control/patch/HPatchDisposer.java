package com.abilix.control.patch;

import android.os.Environment;
import android.os.Handler;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.aidl.Control;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.File;

public class HPatchDisposer extends AbstractPatchDisposer {
    public static final int H_FINGER_PROTECT = 12;
    private FingerProtector mFingerProtector;

    public HPatchDisposer(Handler mHandler) {
        super(mHandler);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void DisposeProtocol(Control control) {
        final int modeState = control.getModeState();
        LogMgr.d("modeState = " + modeState);
        switch (modeState) {
            case 30:
                String movePath5 = Environment.getExternalStorageDirectory().getPath() + File.separator + control.getFileFullPath();
                LogMgr.d("movePath5 = " + movePath5);
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath5, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onResume() {

                    }

                    @Override
                    public void onStop() {
//                        byte[] result1 = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x78,
//                                new byte[]{(byte)0x02,(byte)0x03,(byte)0x00});
//                        SP.write(result1);
                        byte[] color = new byte[14];//这里显示的额头的灯
                        color[0] = 2;
                        color[1] = 3;
                        color[2] = 1;
                        color[6] = 2;
                        color[10] = 3;
                        color[3] = (byte)0;
                        color[4] = (byte)0xFF;
                        color[5] = (byte)0;
                        SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
                            }
                        }, 2500);

                    }

                    @Override
                    public void onSingleMoveStopWhileLoop() {

                    }

                    @Override
                    public void onNormalStop() {

                    }

                    @Override
                    public void onForceStop() {

                    }
                });
                break;
            case H_FINGER_PROTECT:
                // 开启H系列机器人手指保护
                LogMgr.d("开启H系列机器人手指保护");
//			mFingerProtector.startSelfProtectTimer();
                break;
//            case PatchTracker.CHARGING_MOVE:
//                LogMgr.i("执行停电动作 ControlInfo.getMain_robot_type() = "+ControlInfo.getMain_robot_type());
//                if(ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3){
//                    PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
//                    String movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + "H_csh.bin";
//                    PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath,null,false,false,0,false,
// PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT,null);
//                }
//                break;
            case PatchTracker.OPENROBOT_TOUCH_CSH:
            case PatchTracker.OPENROBOT_MOVE:
            case PatchTracker.CLOSE_GROUPControl:
                LogMgr.i("执行开机动作 ControlInfo.getMain_robot_type() = " + ControlInfo.getMain_robot_type());
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
//                boolean isStateXiaDun = false;
                boolean isStateXiaDun = ProtocolUtils.isStateXiadun();
                LogMgr.d("isStateXiaDun = " + isStateXiaDun);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String movePath = null;
                if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
                    movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_BOOT_RECOVER;
                } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                    if(isStateXiaDun){
                        movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + "H5_qishen.bin";
                    }else {
                        movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_BOOT_RECOVER_H;
                    }
                }
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onResume() {

                    }

                    @Override
                    public void onStop() {
//                        byte[] result1 = ProtocolUtils.buildProtocol((byte) ControlInfo.getMain_robot_type(), (byte) 0xA3, (byte) 0x78,
//                                new byte[]{(byte)0x02,(byte)0x03,(byte)0x00});
//                        SP.write(result1);
                            byte[] color = new byte[14];//这里显示的额头的灯
                            color[0] = 2;
                            color[1] = 3;
                            color[2] = 1;
                            color[6] = 2;
                            color[10] = 3;
                            color[3] = (byte)0;
                            color[4] = (byte)0xFF;
                            color[5] = (byte)0;
                            SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
//                        ProtocolUtils.relAndFix(1, (byte) 0x18);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
                            }
                        }, 2500);

                    }

                    @Override
                    public void onSingleMoveStopWhileLoop() {

                    }

                    @Override
                    public void onNormalStop() {

                    }

                    @Override
                    public void onForceStop() {

                    }
                });
                break;

            case PatchTracker.STARTING_UP_FINGER_PROTECT:
                LogMgr.d("开启H系列机器人开机手指保护");
                byte[] byteAngle = new byte[4];
                byte[] byteId = new byte[]{(byte) 19,(byte) 20};
                byte[] bs19 = Utils.intToBytesLH(375);
                byte[] bs20 = Utils.intToBytesLH(375+512);
                System.arraycopy(bs19, 0, byteAngle, 0, bs19.length);
                System.arraycopy(bs20, 0, byteAngle, 2, bs20.length);
                ProtocolUtils.sendEngineAngles((byte) 2,byteId,byteAngle);
//                bytes[0] = (byte) 19;
//                byte[] bs = Utils.intToBytes(375);
//                System.arraycopy(bs, 0, bytes, 1, bs.length);
//                byte[] result = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, (byte) 0xA3, (byte) 0xA1,
//                        bytes);
//                SP.write(result);
//
//                byte[] bytes1 = new byte[3];
//                bytes1[0] = (byte) 20;
//                byte[] bs1 = Utils.intToBytes(375 + 512);
//                System.arraycopy(bs1, 0, bytes1, 1, bs1.length);
//                byte[] result1 = ProtocolUtils.buildProtocol(ControlInitiator.ROBOT_TYPE_H, (byte) 0xA3, (byte) 0xA1,
//                        bytes1);
//                SP.write(result1);
                break;
            case PatchTracker.CHARGE_PROTECTION_UP://充电保护对应的起身动作
                LogMgr.d("H5起身动作执行");
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                String movePath3 = GlobalConfig.MOVE_BIN_PATH + File.separator + "H5_qishen.bin";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath3, null, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {

                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onPause() {

                            }

                            @Override
                            public void onResume() {

                            }

                            @Override
                            public void onStop() {
                                LogMgr.i("onStop");
//                                SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0x78, new byte[]{0x02, 0x03, 0x00}));
                                byte[] color = new byte[14];//这里显示的额头的灯
                                color[0] = 2;
                                color[1] = 3;
                                color[2] = 1;
                                color[6] = 2;
                                color[10] = 3;
                                color[3] = (byte)0;
                                color[4] = (byte)0xFF;
                                color[5] = (byte)0;
                                SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
                                    }
                                }, 2500);
                            }

                            @Override
                            public void onSingleMoveStopWhileLoop() {

                            }

                            @Override
                            public void onNormalStop() {

                            }

                            @Override
                            public void onForceStop() {

                            }
                        });
                break;
            case PatchTracker.CHARGE_PROTECTION_MOVE:
                if (ControlInfo.getMain_robot_type() != ControlInitiator.ROBOT_TYPE_H) {
                    LogMgr.d("不是H5不用下蹲动作保护");
                    return;
                }
                if(ProtocolUtils.isStateXiadun()){
                    LogMgr.d("已经是下蹲状态;低电量时下蹲，手脚灯关闭");
                    try {
                        byte[] color = new byte[14];//这里显示的额头红色和双眼的灯关闭
                        color[0] = 2;
                        color[1] = 3;
                        color[2] = 1;
                        color[3] = (byte) 0xff;
                        color[4] = (byte) 0;
                        color[5] = (byte) 0;
                        color[6] = 2;
                        color[10] = 3;
                        SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
                        Thread.sleep(100);
                        ProtocolUtils.relAndFix(0, (byte) 0x18);
                        Thread.sleep(50);
                        ProtocolUtils.handAndFeetLight(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                LogMgr.d("H5下蹲动作保护");
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                String movePath2 = GlobalConfig.MOVE_BIN_PATH + File.separator + "H5_xiadun.bin";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath2, null, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {

                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onPause() {

                            }

                            @Override
                            public void onResume() {

                            }

                            @Override
                            public void onStop() {
                                LogMgr.i("onStop");
                                //下蹲之后，释放13,14,15,16,17,18,19,20,21,22，并关灯
                                try {
//                                    for (int i = 0; i < 2; i++) {
//                                        SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0x78, new byte[]{0x02, 0x03, 0x01}));
//                                        Thread.sleep(50);
//                                    }
                                    byte[] color = new byte[14];//这里显示的额头红色和双眼的灯关闭
                                    color[0] = 2;
                                    color[1] = 3;
                                    color[2] = 1;
                                    color[3] = (byte) 0xff;
                                    color[4] = (byte) 0;
                                    color[5] = (byte) 0;
                                    color[6] = 2;
                                    color[10] = 3;
                                    SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC0, color));
                                    Thread.sleep(100);
                                    ProtocolUtils.relAndFix(0, (byte) 0x18);
                                    Thread.sleep(50);
                                    ProtocolUtils.handAndFeetLight(false);
//                                    SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC1, new byte[]{0x02, 0x00, 0x00}));
//                                    Thread.sleep(50);
//                                    SP.write(ProtocolUtils.buildProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xC2, new byte[]{0x02, 0x00, 0x00, 0x00}));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onSingleMoveStopWhileLoop() {

                            }

                            @Override
                            public void onNormalStop() {

                            }

                            @Override
                            public void onForceStop() {

                            }
                        });
                break;
            default:
                break;
        }

    }

}
