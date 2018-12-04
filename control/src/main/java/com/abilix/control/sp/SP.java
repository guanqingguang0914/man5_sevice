package com.abilix.control.sp;

import android.os.Handler;
import android.os.Looper;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

/**
 * 串口读写模块对外接口，包括读写数据和主动上报数据接口
 * Created by jingh on 2017/7/10.
 */

public class SP {
    /**当前充电保护状态*/
    private static boolean mChargeProtect = false;
    private static int mSerialPortType = -1;
    private static ISerial mSerial;
    private static int mUpgradeStatus;
    private static SPReceiver mSPReceiver;
    private static Handler mHandler=new Handler(Looper.getMainLooper());

    /**
     * 初始化串口
     * @param serialPort 串口主类型
     */
    public synchronized static void initSerial(int serialPort) {
        LogMgr.d("initSerial 串口主类型 serialPort:" + serialPort);
        if (serialPort > 0) {
            mSerialPortType = serialPort;
        }
        switch (mSerialPortType) {
            case ControlInitiator.SERIALPORT_TYPE_MT0_115200:
                mSerial = new SerialPortMT0_115200();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT0_230400:
                mSerial = new SerialPortMT0_230400();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT1_115200:
                mSerial = new SerialPortMT1_115200();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT1_230400:
                mSerial = new SerialPortMT1_230400();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT1_500000:
                mSerial = new SerialPortMT1_500000();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT0_500000:
                mSerial = new SerialPortMT0_500000();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT0_56700:
                mSerial = new SerialPortMT0_57600();
                break;

            case ControlInitiator.SERIALPORT_TYPE_MT1_56700:
                mSerial = new SerialPortMT1_57600();
                break;
            default:
                LogMgr.e("串口类型不正确");
                break;
        }
        if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
            mUpgradeStatus = Utils.getStm32UpgradeStatus();
        } else {
            SP.setUpdateState(Utils.STM32_STATUS_NORMAL);
        }
    }

    protected synchronized static ISerial getSerial() {
        return mSerial;
    }


    /**
     * 向串口写数据，同步给出返回值
     *
     * @param data
     * @return
     */
    public synchronized static byte[] request(byte[] data) {
        if (mUpgradeStatus != Utils.STM32_STATUS_NORMAL) {//正在固件升级，不允许使用串口
            LogMgr.e("正在升级固件，不允许使用串口");
            return null;
        }
        LogMgr.d("data:" + Utils.bytesToString(data));
        if (mSPReceiver == null && data.length > 7 && (((data[5] == (byte) 0xA3) && (data[6] == (byte) 0x42)) || ((data[5] == (byte) 0x11) && (data[6] == (byte) 0x0C)))) {
            return null;
        }
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        // M轮子电机保护
        if (mChargeProtect
                && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M && data != null && data.length > 6
                && ((data[0] == 0x55 && data[1] == 'B' && data[2] == 'L' && data[3] == 'E' && data[4] == 'S' && data[5] == 'E' && data[6] == 'T')
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x30) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x40)
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x39) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x41))) {
            LogMgr.e("motor security mode");
            mSPReceiver.write(ProtocolUtils.WHEELBYTE);
            mSPReceiver.write(ProtocolUtils.VACUUM);

            return null;
        }
        // M1轮子电机保护
        if (mChargeProtect && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1 && ((data[5] & 0xff) == 0xA6 && data[6] == 0x3B)) {
            LogMgr.e("motor security mode");
            if (mSerial != null) {
                try {

					/*
                     * 第1字节：0x03代表左右轮电机，0x02代表左轮电机，0x01代表右轮电机， 第2字节代表左轮速度；
					 * 第3字节代表右轮速度；
					 * 第4字节：0x00代表闭环电机速度（速度-60～+60）（发送值+100）,0x01代表位移距离
					 * ，0x02代表占空比（速度-100～+100）（发送值+100）； 第5和6字节代表左轮位移距离(高位在前)；
					 * 第7和8字节代表右轮位移距离(高位在前)；单位cm。（左右为机器人的左右）
					 */

                    byte[] motor_data = {0x03, (byte) 100, (byte) 100, 0x02};
                    byte[] motor_stop = ProtocolBuilder.buildProtocol((byte) ControlInfo.getChild_robot_type(), ProtocolBuilder.CMD_M1_MOTOR_, motor_data);
                    mSPReceiver.write(motor_stop);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        return mSPReceiver.writeAndGetReturn(data);
    }


    /**
     * @param data 写入数据
     * @param time 超时
     * @return
     */
    public synchronized static byte[] request(byte[] data, int time) {
        if (mUpgradeStatus != Utils.STM32_STATUS_NORMAL) {//正在固件升级，不允许使用串口
            LogMgr.e("正在升级固件，不允许使用串口");
            return null;
        }
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        // M轮子电机保护
        if (mChargeProtect
                && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M && data != null && data.length > 6
                && ((data[0] == 0x55 && data[1] == 'B' && data[2] == 'L' && data[3] == 'E' && data[4] == 'S' && data[5] == 'E' && data[6] == 'T')
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x30) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x40)
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x39) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x41))) {
            LogMgr.e("motor security mode");
            mSPReceiver.write(ProtocolUtils.WHEELBYTE);
            mSPReceiver.write(ProtocolUtils.VACUUM);

            return null;
        }
        // M1轮子电机保护
        if (mChargeProtect && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1 && ((data[5] & 0xff) == 0xA6 && data[6] == 0x3B)) {
            LogMgr.e("motor security mode");
            if (mSerial != null) {
                try {

					/*
                     * 第1字节：0x03代表左右轮电机，0x02代表左轮电机，0x01代表右轮电机， 第2字节代表左轮速度；
					 * 第3字节代表右轮速度；
					 * 第4字节：0x00代表闭环电机速度（速度-60～+60）（发送值+100）,0x01代表位移距离
					 * ，0x02代表占空比（速度-100～+100）（发送值+100）； 第5和6字节代表左轮位移距离(高位在前)；
					 * 第7和8字节代表右轮位移距离(高位在前)；单位cm。（左右为机器人的左右）
					 */

                    byte[] motor_data = {0x03, (byte) 100, (byte) 100, 0x02};
                    byte[] motor_stop = ProtocolBuilder.buildProtocol((byte) ControlInfo.getChild_robot_type(), ProtocolBuilder.CMD_M1_MOTOR_, motor_data);
                    mSPReceiver.write(motor_stop);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        return mSPReceiver.writeAndGetReturn(data, time);
    }


    public synchronized static void cancelRequestTimeOut() {
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        mSPReceiver.cancelRequestTimeOut();
    }

    /**
     * 向辅串口写数据，同步给出返回值
     *
     * @param data
     * @return
     */
    public synchronized static byte[] requestVice(byte[] data) {
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        return mSPReceiver.writeAndGetReturnVice(data);
    }


    public synchronized static byte[] requestOnlyUpdate(byte[] data, int time) {
        if (mSPReceiver == null) {
            if (!(data != null && data.length > 6 && data[5] == (byte) 0x11 && data[6] == (byte) 0x06)) {
                LogMgr.e("不是查询版本命令，开启串口循环读取");
                mSPReceiver = new SPReceiver();
            } else {
                LogMgr.e("是查询版本命令，不开启串口循环读取");
                return null;
            }
        }
        return mSPReceiver.writeAndGetReturn(data, time);
    }


    /**
     * 向串口写数据，没有返回值
     *
     * @param data
     * @return
     */
    public synchronized static void write(byte[] data) {
        if (mUpgradeStatus != Utils.STM32_STATUS_NORMAL) {//正在固件升级，不允许使用串口
            LogMgr.e("正在升级固件，不允许使用串口");
            return;
        }
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        // M轮子电机保护
        if (mChargeProtect
                && ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M && data != null && data.length > 6
                && ((data[0] == 0x55 && data[1] == 'B' && data[2] == 'L' && data[3] == 'E' && data[4] == 'S' && data[5] == 'E' && data[6] == 'T')
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x30) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x40)
                || ((data[5] & 0xff) == 0xA3 && data[6] == 0x39) || ((data[5] & 0xff) == 0xA3 && data[6] == 0x41))) {
            LogMgr.e("motor security mode");
            mSPReceiver.write(ProtocolUtils.WHEELBYTE);
            mSPReceiver.write(ProtocolUtils.VACUUM);

            return;
        }
        mSPReceiver.write(data);
    }


    /**
     * F3更新:向串口写数据，没有返回值
     *
     * @param data
     * @return
     */
    public synchronized static void fWrite(byte[] data) {
        /*if (mUpgradeStatus != Utils.STM32_STATUS_NORMAL) {//正在固件升级，不允许使用串口
            LogMgr.e("正在升级固件，不允许使用串口");
            return;
        }*/
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        mSPReceiver.write(data);
    }


    /**
     * 向串口写数据，没有返回值
     *
     * @param data
     */
    public synchronized static void writeVice(byte[] data) {
        if (mSPReceiver == null) {
            LogMgr.i("SPReceiver构建处");
            mSPReceiver = new SPReceiver();
        }
        mSPReceiver.writeVice(data);
    }

    /**
     * 注册主动上报数据
     *
     * @param mPushMsg
     */
    public synchronized static void registerPushEvent(PushMsg mPushMsg) {
        Pusher.createSTM32Pusher().registerPushEvent(mPushMsg);
    }


    /**
     * 反注册
     *
     * @param mPushMsg
     */
    public synchronized static void unRegisterPushEvent(PushMsg mPushMsg) {
        Pusher.createSTM32Pusher().unRegisterPushEvent(mPushMsg);
    }



    /**
     * 关闭串口循环读的功能
     * 谨慎使用
     */
    public synchronized static void destroySP() {
        LogMgr.w("destroySP 关闭串口 不再清空串口已注册的主动上报的回调");
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                LogMgr.w("清空串口主动上报的回调");
//                Pusher.createSTM32Pusher().destory();
//            }
//        });
//        LogMgr.e("destroySP()");
        if (mSPReceiver != null) {
            mSPReceiver.stopReceive();
            mSPReceiver = null;
        }
    }

    /**
     * 设置充电状态
     * @param chargeState
     */
    public static synchronized void setChargeProtectState(boolean chargeState) {
        mChargeProtect = chargeState;
    }

    public static synchronized void setUpdateState(int status) {
        LogMgr.e("设置固件升级状态：" + status);
        mUpgradeStatus = status;
        Utils.setStm32UpgradeStatus(mUpgradeStatus);
    }

    public static int getUpdateState() {
        mUpgradeStatus = Utils.getStm32UpgradeStatus();
        return mUpgradeStatus;
    }
}
