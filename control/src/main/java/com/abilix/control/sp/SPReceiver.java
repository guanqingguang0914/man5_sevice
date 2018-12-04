package com.abilix.control.sp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.Arrays;


/**
 * 接收串口数据并同步给出返回值
 * Created by jingh on 2017/7/7.
 */

public class SPReceiver {
    /**
     * 主串口读数据线程
     */
    private HandlerThread mReceiveThread;
    /**
     * 主串口读数据线程的Handler
     */
    private ReceiveHandler mReceiveHandler;
    /**
     * 副串口读数据线程
     */
    private HandlerThread mReceiveViceThread;
    /**
     * 副串口读数据线程的Handler
     */
    private ReceiveViceHandler mReceiveViceHandler;
    /**
     * 主串口读数据标志量
     */
    private boolean isReceive = true;
    /**
     * 副串口读数据标志量
     */
    private boolean isReceiveVice = true;
    /**
     * 读串口等待返回的超时时间
     */
    private final int TIMEOUT = 500;
    /**
     * 直接写串口异常时，串口初始化最大次数
     */
    private final int TRY_TIMES_MAX = 15;

    /**
     * 串口数据头
     */
    private byte[] read_head;

    /**
     * bootloader 返回数据
     */
    private byte[] read_bootloader;


    /**
     * 副串口数据头
     */
    private byte[] read_vice_head;

    /**
     * 串口数据体
     */
    private byte[] read_remain;
    /**
     * 副串口数据体
     */
    private byte[] read_vice_remain;

    /**
     * 串口完整数据，用于主动上报
     */
    private byte[] receiveData;
    /**
     * 副串口完整数据，用于主动上报
     */
    private byte[] receiveViceData;

    private byte[] err_buf;

    /**
     * 主串口异步返回完整数据
     */
    private byte[] synResponseData;
    /**
     * 副串口异步返回完整数据
     */
    private byte[] synResponseViceData;
    private Pusher mPusher;
    private ISerial mSerial;
    /**
     * 主串口读写锁
     */
    private final Object mLock = new Object();
    /**
     * 副串口读写锁
     */
    private Object mViceLock = new Object();
    private Object functionLock = new Object();
    /**
     * 发生异常时重新初始化串口的次数
     */
    private int tryTimes = 0;

    private final int START_RECEIVE_DATA = 0;
    private final Object mInitLock = new Object();
//    public static boolean sIsFUpgrade = false;  //是否是F升级流程

    /**
     * 构造函数
     * 直到把串口中无用数据读出后才算构建完成（超时2秒）
     */
    protected SPReceiver() {
        LogMgr.d("SPReceiver开始构建");
        init();
        synchronized (mInitLock) {
            try {
                mInitLock.wait(2000);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LogMgr.d("SPReceiver构建完成!!!");
    }

    /**
     * 初始化串口循环读取功能
     */
    private void init() {
        mPusher = Pusher.createSTM32Pusher();
        SP.initSerial(ControlInfo.getMain_serialport_type());
        mSerial = SP.getSerial();
        //接收主串口数据
        mReceiveThread = new HandlerThread("ReceiveThread");
        mReceiveThread.start();
        mReceiveHandler = new ReceiveHandler(mReceiveThread.getLooper());
        mReceiveHandler.sendEmptyMessage(START_RECEIVE_DATA);
        if (mSerial.getViceOs() != null) {//接收辅串口数据
            mReceiveViceThread = new HandlerThread("ReceiveViceThread");
            mReceiveViceThread.start();
            mReceiveViceHandler = new ReceiveViceHandler(mReceiveViceThread.getLooper());
            mReceiveViceHandler.sendEmptyMessage(START_RECEIVE_DATA);
        }
    }

    /**
     * 重新初始化串口
     */
    private void reInit() {
        if (mSerial != null) {
            mSerial.destorySerialPort();
            SP.initSerial(ControlInfo.getMain_serialport_type());
            mSerial = SP.getSerial();
        }
    }


    /**
     * 写入并读取主串口数据
     *
     * @param data 写入串口的请求数据
     * @return
     */
    protected synchronized byte[] writeAndGetReturn(byte[] data) {
        LogMgr.d("writeAndGetReturn===>");
        synchronized (mLock) {
            synResponseData = null;
            write(data);
            try {
                mLock.wait(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (synResponseData != null) {
            LogMgr.d("串口异步返回数据结束 = " + Utils.bytesToString(synResponseData));
        }
        return synResponseData;
    }


    /**
     * @param data 写入的数据
     * @param time 超时时间
     * @return stm32返回值
     */
    protected synchronized byte[] writeAndGetReturn(byte[] data, int time) {
        synchronized (mLock) {
            synResponseData = null;
            write(data);
            try {
//                LogMgr.e("开始等待锁唤醒");
                mLock.wait(time);
//                LogMgr.e("锁被唤醒");
            } catch (InterruptedException e) {
                LogMgr.e("writeAndGetReturn 锁异常");
                e.printStackTrace();
            }
        }
        if (synResponseData != null) {
            LogMgr.d("串口返回值：" + Utils.bytesToString(synResponseData));
        }else{
            LogMgr.d("串口返回值: null");
        }


        return synResponseData;
    }

    /**
     * @param data 写入的数据
     * @param time 超时时间
     * @return stm32返回值
     */
    protected synchronized byte[] writeAndGetReturnNotWait(byte[] data, int time) {
        byte[] bytes;
        synchronized (mLock) {
            synResponseData = null;
            bytes = writeForResult(data);
        }
        return bytes;
    }

    /**
     * 取消超时
     */
    protected synchronized void cancelRequestTimeOut() {
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }


    /**
     * 写入并读取辅串口数据
     *
     * @param data 写入串口的请求数据
     * @return
     */
    protected synchronized byte[] writeAndGetReturnVice(byte[] data) {
        synchronized (mViceLock) {
            synResponseViceData = null;
            writeVice(data);
            try {
                mViceLock.wait(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (synResponseViceData == null) {
            //LogMgr.d("串口返回值：" + Utils.bytesToString(synResponseViceData, synResponseViceData.length));
            LogMgr.e("串口读取超时");
        }
        return synResponseViceData;
    }

    /**
     * 写入并读取串口数据,无返回值
     *
     * @param data
     */
    public synchronized void write(byte[] data) {
        try {
            LogMgr.i("串口写入数据 = " + Utils.bytesToString(data));
            mSerial.getOs().write(data);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("串口写错误 e:" + e.toString());
            if (tryTimes < TRY_TIMES_MAX) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                reInit();
                tryTimes++;
                LogMgr.e("重新初始化串口次数：" + tryTimes);
                write(data);
            }
        }
    }


    /**
     * 写入并读取串口数据,有返回值
     *
     * @param data
     */
    public synchronized byte[] writeForResult(byte[] data) {
        byte[] bytes = new byte[2];
        try {
            LogMgr.e("串口写入数据 = " + Utils.bytesToString(data));
            mSerial.getOs().write(data);
            int read = mSerial.getIs().read(bytes);
            LogMgr.e("串口读出数据 = " + Utils.bytesToString(bytes));
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("串口写错误 e:" + e.toString());
        }


        return null;
    }

    /**
     * 写入并读取辅串口数据,无返回值
     *
     * @param data
     * @return
     */
    public synchronized void writeVice(byte[] data) {
        try {
            if (mSerial.getViceOs() == null) {
                LogMgr.e("副串口为空");
                return;
            }
            mSerial.getViceOs().write(data);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("串口写错误 e:" + e.toString());
            if (tryTimes < TRY_TIMES_MAX) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                reInit();
                tryTimes++;
                LogMgr.e("重新初始化串口次数：" + tryTimes);
                writeVice(data);
            }
        }
    }


    private class ReceiveHandler extends Handler {
        public ReceiveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_RECEIVE_DATA:
                    isReceive = true;
                    startReceive();
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * 开启无限循环接收串口数据功能
     */
    private void startReceive() {
        LogMgr.e("开启无限循环接收串口数据功能");

        read_head = new byte[4];
        read_bootloader = new byte[60];
        int len = 0;
        try {
            if (mSerial.getIs().available() > 0) {//初始化串口之后，先读掉缓存里的无用数据，避免影响到后面的读取操作
                byte[] read_buf = new byte[mSerial.getIs().available()];
                mSerial.getIs().read(read_buf);
                LogMgr.e("开启无限循环接收数据功能第一步，把无效数据全部读出：" + Utils.bytesToString(read_buf));
            }
            synchronized (mInitLock) {
                //这个时候才认为SPReceiver初始化成功
                LogMgr.i("开启无限循环接收数据无效数据全部读出结束");
                mInitLock.notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (isReceive) {
            try {
                LogMgr.d("串口开始读取数据isReceive = " + isReceive);

                if (SP.getUpdateState() == Utils.STM32_STATUS_UPGRADING
                        && ControlInfo.getMain_robot_type()== ControlInitiator.ROBOT_TYPE_F) {
//                    if(true){
                    //第二步： 给bootloader发暂停信号，避免进入mavlink程序
                    //8.擦除下层固件  发送“擦除”命令以后飞控擦除片内flash需要时间，大概5s以后才会给你擦除成功回复，留意一下
                    //第九步：下载固件，固件按照 PROG_MULT_MAX 大小分包下载
                    int read = mSerial.getIs().read(read_bootloader);
                    if (read_bootloader[0] == (byte) 0x12 && read_bootloader[1] == (byte) 0x10) {
                        LogMgr.d("第 二/八/九 步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));

//                        printStream.printf(Utils.bytesToString(read_bootloader, read_bootloader.length));
//                        LogMgr.d("输出日志打印成功");

//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
//                        LogMgr.d("接收到第二/八/九步  成功的消息,解锁");
                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //第三步：请求 bootloader 信息
                    } else if ((read_bootloader[0] >= (byte) 2 && read_bootloader[0] <= (byte) 5) && read_bootloader[4] == (byte) 0x12 && read_bootloader[5] == (byte) 0x10) {
                        LogMgr.d("第三步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第三步  请求 bootloader 信息  成功的消息,解锁");
                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //第四步  1:  飞控  请求 board id 信息
                    } else if ((read_bootloader[0] == (byte) 9) && read_bootloader[4] == (byte) 0x12 && read_bootloader[5] == (byte) 0x10) {
                        LogMgr.d("第四步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第四步   飞控 请求 board id 信息  成功的消息,解锁");

                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //第四步  1:  光流  请求 board id 信息
                    } else if ((read_bootloader[0] == 6) && read_bootloader[4] == (byte) 0x12 && read_bootloader[5] == (byte) 0x10) {
                        LogMgr.d("第四步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第四步  光流 请求 board id 信息  成功的消息,解锁");

                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //5.请求 PCB 修订号 信息  TODO 判断 rev 是否在需要的board rev，否则异常退出
                    } else if (read_bootloader[0] == 0
                            && read_bootloader[1] == 0
                            && read_bootloader[2] == 0
                            && read_bootloader[3] == 0
                            && read_bootloader[4] == (byte) 0x12
                            && read_bootloader[5] == (byte) 0x10) {
                        LogMgr.d("第五步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第五步  请求 PCB 修订号 信息成功的消息,解锁");

                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //6.请求 flash size 信息
                    } else if (read_bootloader[0] == 0
                            && read_bootloader[1] == (byte) 0xC0
                            && read_bootloader[2] == (byte) 0x0F
                            && read_bootloader[3] == 0
                            && read_bootloader[4] == (byte) 0x12
                            && read_bootloader[5] == (byte) 0x10) {
                        LogMgr.d("第六步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
//                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第六步  请求 flash size 信息成功的消息,解锁");

                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                        //7.请求下层固件的CRC
                    } else if (read_bootloader[4] == 0x12 && read_bootloader[5] == 0x10) {
                        LogMgr.d("第七步读到的串口信息  :  " + Utils.bytesToString(read_bootloader));
                        synResponseData = read_bootloader;
                        synResponseData = Arrays.copyOf(read_bootloader, 60);
                        LogMgr.d("接收到第七步  请求下层固件的CRC 信息成功的消息,解锁");

                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        synchronized (mLock) {
                            mLock.notify();
                            LogMgr.d("解锁");
                        }
                        continue;

                    } else {
                        LogMgr.d("无效的F升级串口信息 : " + Utils.bytesToString(read_bootloader));
                        len = 0;
                        //清空串口信息
                        Arrays.fill(read_bootloader, (byte) 0);
                        continue;
                    }

                }
                while (isReceive && len < 4) {
//                    if(SP.getUpdateState()&&ControlInfo.getMain_robot_type()== ControlInitiator.ROBOT_TYPE_F){
//                        LogMgr.i("当前开始F固件升级，跳出读头循环");
//                        break;
//                    }
                    len = len + mSerial.getIs().read(read_head, len, 4 - len);
                    LogMgr.i("不停地读取协议头1 len = " + len);
                    LogMgr.i("协议头内容 : " + Utils.bytesToString(read_head));
                }
//                if(SP.getUpdateState()&&ControlInfo.getMain_robot_type()== ControlInitiator.ROBOT_TYPE_F){
//                    LogMgr.i("当前开始F固件升级，跳出正常循环");
//                    break;
//                }
                LogMgr.i("线程ID:" + Thread.currentThread().getId() + " 读取4字节数据头 = " + Utils.bytesToString(read_head));
                if ((read_head != null) && (read_head[0] == (byte) 0xAA)
                        && (read_head[1] == (byte) 0x55 || read_head[1] == (byte) 0x66)) {
                    int remain_length = Utils.byte2int_2byteHL(read_head, 2);
                    if(remain_length <= 0){
                        LogMgr.e("后续数据的长度异常 放弃此条数据 remain_length = "+remain_length);
                        len = 0;
                        continue;
                    }
                    read_remain = new byte[remain_length];
                    int l = 0;
                    while (isReceive && l < remain_length) {
                        l = l + mSerial.getIs().read(read_remain, l, remain_length - l);
                    }
                    //    LogMgr.d("剩余buff接收完成："+Utils.bytesToString(read_remain,read_remain.length));
                    // mSerial.getIs().read(read_remain);
                    if (read_head[1] == (byte) 0x55) {
                        if (read_remain.length > 2 && ((read_remain[0] == (byte) 0x01 && read_remain[2] == (byte) 0x25)
                                || (read_remain[0] == (byte) 0x01 && read_remain[2] == (byte) 0xB4))) {//C系列巡线模块上报
                            LogMgr.i("C系列巡线模块上报");
                            receiveData = Utils.byteMerger(read_head, read_remain);
                            Message msg = mPusher.obtainMessage();
                            msg.obj = receiveData;
                            mPusher.sendMessage(msg);
                            //yhd U系列添加协议:主动上报查询串口是否正常连接
                        } else if (read_remain[0] == (byte) 0x51
                                && read_remain[1] == (byte) 0xf8
                                && read_remain[2] == (byte) 0xf1
                                && read_remain[7] == (byte) 0x01
                                && read_remain[8] == (byte) 0x55) {

                            //yhd U 系列添加 BRAIN B串口检测功能 需要注册
                            //aa 55 00 0a  51 f8 f1 00 00 00 00 01 55 校验位

/*                            LogMgr.i("U系列 Brain B 串口检测 == 主动上报");
                            receiveData = Utils.byteMerger(read_head, read_remain);
                            Message msg = mPusher.obtainMessage();
                            msg.obj = receiveData;
                            mPusher.sendMessage(msg);*/

                            //yhd U 系列添加 BRAIN B串口检测,收到直接回复    看不懂Pusher,不会用
                            byte[] return_bytes = ProtocolBuilder.buildProtocol((byte) 0x51, (byte) 0xA8, (byte) 0xF1, new byte[]{(byte) 0x01, (byte) 0x55});
                            write(return_bytes);
                        } else {
                            synResponseData = Utils.byteMerger(read_head, read_remain);
                            LogMgr.d("循环读取的完整数据 命令回复 = " + Utils.bytesToString(synResponseData));
                            synchronized (mLock) {
                                mLock.notify();
                            }
                            if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9
                                    && read_remain.length > 2 && ((read_remain[1] == (byte) 0xF0 && read_remain[2] == (byte) 0x05))) {
                                LogMgr.i("K9升级:成功跳转到bootloader，ReceiveThread sleep 5000ms;等待销毁线程!");
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    LogMgr.e("K9升级:ReceiveThread sleep 中断!");
                                    e.printStackTrace();
                                }
                            }
                        }
                        len = 0;
                        continue;
                    } else if (read_head[1] == (byte) 0x66) {
                        receiveData = Utils.byteMerger(read_head, read_remain);
                        LogMgr.d("循环读取的完整数据 主动上报 = " + Utils.bytesToString(receiveData));
                        Message msg = mPusher.obtainMessage();
                        msg.obj = receiveData;
                        mPusher.sendMessage(msg);
                        len = 0;
                        continue;
                    } else {
                        err_buf = Utils.byteMerger(read_head, read_remain);
                        LogMgr.e("接收到的STM32反馈异常：" + Utils.bytesToString(err_buf));
                        len = 0;
                        continue;
                    }

                } else if ((read_head != null) && (
                        (read_head[0] == (byte) 'C' && (read_head[1] == (byte) 'S' || read_head[1] == (byte) 'G')) //兼容C老协议
                                || ((read_head[0] == (byte) 0x55 || read_head[0] == (byte) 0x56)
                                && (read_head[1] == (byte) 'A' || read_head[1] == (byte) 'B'
                                || read_head[1] == (byte) 'C' || read_head[1] == (byte) 'G'
                                || read_head[1] == (byte) 'I' || read_head[1] == (byte) 'L'
                                || read_head[1] == (byte) 'N')))) {//兼容C、M老协议
                    read_remain = new byte[16];
                    int l = 0;
                    while (isReceive && l < 16) {
                        l = l + mSerial.getIs().read(read_remain, l, 16 - l);
                    }
                    //mSerial.getIs().read(read_remain);
                    synResponseData = Utils.byteMerger(read_head, read_remain);
                    LogMgr.d("循环读取的完整数据 兼容C、M老协议 命令回复 = " + Utils.bytesToString(synResponseData));
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    len = 0;
                    continue;
                } else if ((read_head != null) && (read_head[0] == (byte) 0xFF) && (read_head[1] == (byte) 0xFF)) {//兼容H、S舵机协议
                    int remain_length = read_head[3];
                    if(remain_length <= 0){
                        LogMgr.e("后续数据的长度异常 放弃此条数据 remain_length = "+remain_length);
                        len = 0;
                        continue;
                    }
                    read_remain = new byte[remain_length];
                    int l = 0;
                    while (isReceive && l < remain_length) {
                        l = l + mSerial.getIs().read(read_remain, l, remain_length - l);
                    }
                    //mSerial.getIs().read(read_remain);
                    synResponseData = Utils.byteMerger(read_head, read_remain);
                    LogMgr.d("循环读取的完整数据 兼容H、S舵机协议 命令回复 = " + Utils.bytesToString(synResponseData));
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    len = 0;
                    continue;
                } else if ((read_head != null) && (read_head[0] == (byte) 0xFE)) {//MAVLink协议
                    int remain_length = (read_head[1] & 0xff) + 4;
                    if(remain_length <= 0){
                        LogMgr.e("后续数据的长度异常 放弃此条数据 remain_length = "+remain_length);
                        len = 0;
                        continue;
                    }
                    read_remain = new byte[remain_length];
                    int l = 0;
                    while (isReceive && l < remain_length) {
                        l = l + mSerial.getIs().read(read_remain, l, remain_length - l);
                    }
                    receiveData = Utils.byteMerger(read_head, read_remain);
                    LogMgr.d("获取到MAVLink的回复数据1 receiveData = "+ Utils.bytesToString(receiveData));
                    // 通过msgid 分离非主动上报消息
                    if (receiveData.length > 5 && (receiveData[5] & 0xFF) == 148) { /*94*/
                        LogMgr.i("获取到MAVLink的回复数据 = " + Utils.bytesToString(receiveData));
                        synResponseData = receiveData;
                        synchronized (mLock) {
                            mLock.notify();
                        }
                        len = 0;
                        continue;
                    }
//                    LogMgr.i("mavlink返回数据：" + Utils.bytesToString(receiveData, receiveData.length));
                    Message msg = mPusher.obtainMessage();
                    msg.obj = receiveData;
                    mPusher.sendMessage(msg);
                    len = 0;
                    continue;
                }
//                else if ((read_head !=null) && (len ==2 || len ==4)){  //F34 固件升级协议
//                    if (len ==2 && read_head[0] == 0x12 && read_head[1] == 0x10){
//                        synResponseData = new byte[len];
//                        synResponseData[0] = read_head[0];
//                        synResponseData[1] = read_head[1];
//                    }else if (len ==4){
//                        byte[] temp = new byte[6-len];
//                        int l = mSerial.getIs().read(temp, 0,  temp.length);
//                        if (l ==2 &&  temp[0] == 0x12 && temp[1] == 0x10){
//                            synResponseData = Utils.byteMerger(read_head, temp);
//                        }else LogMgr.e("F34 固件升级协议错误");
//                    }else LogMgr.e("F34 固件升级协议错误");
//                    len = 0;
//                    continue;
//                }
                else {//无效的数据
                    LogMgr.e("无效的返回数据：" + Utils.bytesToString(read_head));
                    if (read_head == null) {
                        LogMgr.e("read_head为null");
                        len = 0;
                        return;
                    } else {//数据包内丢、增、错数据情况，如果不容许丢帧应逐一寻头
                        LogMgr.w("数据头错误，丢掉第一个字节数据");
                        System.arraycopy(read_head, 1, read_head, 0, 3);//丢掉第一个字节数据
                        if (len > 0){
                                len--;
                        }
                    }
                }
            } catch (Exception e) {
                len = 0;
                e.printStackTrace();
                LogMgr.e("串口读错误 e:" + e.toString());
                if (isReceive && tryTimes < 5) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    reInit();
                    tryTimes++;
                    LogMgr.e("重新初始化串口次数：" + tryTimes);
                } else {
                    LogMgr.e("重新打开串口失败");
                    isReceive = false;
                    //重新初始化串口后仍然有问题话，这里后续可以加上其它措施
                }
            }
        }
        LogMgr.e("startReceive() end:关闭无限循环接收串口数据功能>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }


    private class ReceiveViceHandler extends Handler {
        public ReceiveViceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_RECEIVE_DATA:
                    isReceiveVice = true;
                    startReceiveVice();
                    break;
                default:
                    break;
            }
        }
    }

    private void startReceiveVice() {
        LogMgr.e("开启无限循环接收辅助串口数据");
        read_vice_head = new byte[4];
        int len = 0;
        try {
            if (mSerial.getViceIs().available() > 0) {//初始化串口之后，先读掉缓存里的无用数据，避免影响到后面的读取操作
                byte[] read_buf = new byte[mSerial.getViceIs().available()];
                mSerial.getViceIs().read(read_buf);
                LogMgr.e("无效数据：" + Utils.bytesToString(read_buf));
            }
            synchronized (mInitLock) {
                //这个时候才认为SPReceiver初始化成功
                mInitLock.notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (isReceiveVice) {
            try {
                while (isReceiveVice && len < 4) {
                    len = len + mSerial.getViceIs().read(read_vice_head, len, 4 - len);
                }
                LogMgr.e("read_vice_head:" + Utils.bytesToString(read_vice_head));
                if ((read_vice_head != null) && (read_vice_head[0] == (byte) 0xAA)
                        && (read_vice_head[1] == (byte) 0x55 || read_vice_head[1] == (byte) 0x66)) {
                    int remain_length = Utils.byte2int_2byteHL(read_vice_head, 2);
//                    int remain_length = Utils.byte2int_2byteLH(read_vice_head, 2);//这里需要注意的是H5 433发送协议高低位；
                    read_vice_remain = new byte[remain_length];
                    int l = 0;
                    while (isReceiveVice && l < remain_length) {
                        l = l + mSerial.getViceIs().read(read_vice_remain, l, remain_length - l);
                    }
                    // mSerial.getIs().read(read_vice_remain);
                    if (read_vice_head[1] == (byte) 0x55) {
                        if (read_vice_remain.length > 2 && ((read_vice_remain[0] == (byte) 0x01 && read_vice_remain[2] == (byte) 0x25)
                                || (read_vice_remain[0] == (byte) 0x01 && read_vice_remain[2] == (byte) 0xB4))) {//C系列巡线模块上报
                            receiveViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
                            Message msg = mPusher.obtainMessage();
                            msg.obj = receiveViceData;
                            mPusher.sendMessage(msg);
                        } else if(read_vice_remain.length > 2 && read_vice_remain[1] == (byte)0xC0){
                            //H5系列辅助串口返回数据，群控使用
                            receiveViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
                            LogMgr.d("receiveViceData = " + Utils.bytesToString(receiveViceData));
                            Message msg = mPusher.obtainMessage();
                            msg.obj = receiveViceData;
                            mPusher.sendMessage(msg);
                        } else {
                            synResponseViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
                            synchronized (mViceLock) {
                                mViceLock.notify();
                            }
                        }
                        len = 0;
                        continue;
                    } else if (read_vice_head[1] == (byte) 0x66) {
                        receiveViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
                        Message msg = mPusher.obtainMessage();
                        msg.obj = receiveViceData;
                        mPusher.sendMessage(msg);
                        len = 0;
                        continue;
                    } else {
                        err_buf = Utils.byteMerger(read_vice_head, read_vice_remain);
                        LogMgr.e("接收到的STM32反馈异常：" + Utils.bytesToString(err_buf));
                        len = 0;
                        continue;
                    }

                }
//                else if ((read_vice_head != null) && (
//                        (read_vice_head[0] == (byte) 'C' && (read_vice_head[1] == (byte) 'S' || read_vice_head[1] == (byte) 'G')) //兼容C老协议
//                                || ((read_vice_head[0] == (byte) 0x55 || read_vice_head[0] == (byte) 0x56)
//                                && (read_vice_head[1] == (byte) 'A' || read_vice_head[1] == (byte) 'B'
//                                || read_vice_head[1] == (byte) 'C' || read_vice_head[1] == (byte) 'G'
//                                || read_vice_head[1] == (byte) 'I' || read_vice_head[1] == (byte) 'L'
//                                || read_vice_head[1] == (byte) 'N')) //兼容M老协议
//                )) {//兼容C、M老协议
//                    read_vice_remain = new byte[16];
//                    int l = 0;
//                    while (isReceiveVice && l < 16) {
//                        l = l + mSerial.getViceIs().read(read_vice_remain, l, 16 - l);
//                    }
//                    //mSerial.getIs().read(read_vice_remain);
//                    synResponseViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
//                    synchronized (mViceLock) {
//                        mViceLock.notify();
//                    }
//                    len = 0;
//                    continue;
//                } else if ((read_vice_head != null) && (read_vice_head[0] == (byte) 0xFF) && (read_vice_head[1] == (byte) 0xFF)) {//兼容H、S舵机协议
//                    int remain_length = read_vice_head[3];
//                    read_vice_remain = new byte[remain_length];
//                    int l = 0;
//                    while (isReceiveVice && l < remain_length) {
//                        l = l + mSerial.getViceIs().read(read_vice_remain, l, remain_length - l);
//                    }
//                    //mSerial.getIs().read(read_vice_remain);
//                    synResponseViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
//                    synchronized (mViceLock) {
//                        mViceLock.notify();
//                    }
//                    len = 0;
//                    continue;
//                } else if ((read_vice_head != null) && (read_vice_head[0] == (byte) 0xFF)) {//MAVLink协议
//                    int remain_length = read_vice_head[1] + 4;
//                    read_vice_remain = new byte[remain_length];
//                    int l = 0;
//                    while (isReceiveVice && l < remain_length) {
//                        l = l + mSerial.getViceIs().read(read_vice_remain, l, remain_length - l);
//                    }
//                    receiveViceData = Utils.byteMerger(read_vice_head, read_vice_remain);
//                    Message msg = mPusher.obtainMessage();
//                    msg.obj = receiveViceData;
//                    mPusher.sendMessage(msg);
//                    len = 0;
//                    continue;
//                }
                else {
                    if (read_vice_head == null) {
                        LogMgr.e("数据为null");
                        len = 0;
                        return;
                    } else {//数据包内丢、增、错数据情况，如果不容许丢帧应逐一寻头
                        System.arraycopy(read_vice_head, 1, read_vice_head, 0, 3);//丢掉第一个字节数据
                        if (len > 0) len--;
                    }
                    //无效的数据
                    //   LogMgr.e("无效的返回数据：" + Utils.bytesToString(read_vice_head, read_vice_head.length));
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogMgr.e("串口读错误 e:" + e.toString());
                if (isReceiveVice && tryTimes < 5) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    reInit();
                    tryTimes++;
                    LogMgr.e("重新初始化串口次数：" + tryTimes);
                } else {
                    LogMgr.e("重新打开串口失败");
                    isReceiveVice = false;
                    //重新初始化串口后仍然有问题话，这里后续可以加上其它措施
                }
            }
        }
        LogMgr.e("startReceiveVice() end:关闭无限循环接收串口数据功能>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }


    public synchronized void stopReceive() {
        LogMgr.e("stopReceive串口循环读取关闭 开始");
        isReceive = false;
        isReceiveVice = false;
        if (ControlInfo.getChild_robot_type() == ControlInitiator.ROBOT_TYPE_C9) {
            //K9跳转到bootloader后 直接进入zmodem接收模式 不能处理任何其他指令
            byte[] finish_bytes = ProtocolBuilder.buildProtocol((byte) 0x00, ProtocolBuilder.CMD_GET_STM32_VERSION, null);
            write(finish_bytes);
        } else {
            //发送这条协议是为了让 退出串口读阻塞
            byte[] finish_bytes = ProtocolBuilder.buildProtocol((byte) 0x00, ProtocolBuilder.CMD_GET_STM32_VERSION, null);
            write(finish_bytes);
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mSerial != null) {
            mSerial.destorySerialPort();
        }
        if (mReceiveThread != null) {
            LogMgr.e("interrupt thread");
            mReceiveThread.interrupt();
            mReceiveThread.quitSafely();
            mReceiveThread = null;
        }
        if (mPusher != null) {
            mPusher.removeCallbacksAndMessages(null);
            mPusher = null;
        }
        if (mReceiveHandler != null) {
            mReceiveHandler.removeCallbacksAndMessages(null);
            mReceiveHandler = null;
        }
        if (mReceiveViceThread != null) {
            mReceiveViceThread.interrupt();
            mReceiveViceThread.quitSafely();
            mReceiveViceThread = null;
        }
        if (mReceiveViceHandler != null) {
            mReceiveViceHandler.removeCallbacksAndMessages(null);
            mReceiveViceHandler = null;
        }
        LogMgr.e("stopReceive串口循环读取关闭 结束");
    }

}
