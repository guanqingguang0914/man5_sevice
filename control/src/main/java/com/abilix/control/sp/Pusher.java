package com.abilix.control.sp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.abilix.control.GlobalConfig;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.ArrayList;

/**
 * STM32主动上报接口
 * 运行在主线程的Handler
 * Created by jingh on 2017/7/7.
 */

public class Pusher extends Handler {

    public interface PushListener {
        /**
         * 处理主动上报的数据
         * @param pushData
         */
        void onPush(byte[] pushData);
    }

    public static int COMMON_PUSH_EVENT = 0;

    public static int MAVLINK_PUSH_EVENT=1;

    public static Pusher instance;

    public ArrayList mList;

    private int eventType = -1;
    private static Object mLock=new Object();

    private Pusher() {
        super(Looper.getMainLooper());
        mList = new ArrayList();
    }

    public static Pusher createSTM32Pusher() {
        if (instance == null) {
            synchronized (mLock) {
                if (instance == null) {
                    instance = new Pusher();
                }
            }
        }
        return instance;
    }

    public synchronized void destory() {
        mList = null;
        instance = null;
    }


    /**
     * 注册需要关注的上报事件
     *
     * @param mPushMsg
     */
    public void registerPushEvent(PushMsg mPushMsg) {
        LogMgr.e("注册上报事件监听");
        mList.add(mPushMsg);
    }



    /**
     * 反注册
     *
     * @param mPushMsg
     */
    public void unRegisterPushEvent(PushMsg mPushMsg) {
        LogMgr.d("反注册上报事件监听");
        for (int i = 0; i < mList.size(); i++) {
            PushMsg mMsg = (PushMsg) mList.get(i);
            if (mMsg == mPushMsg) {
                mList.remove(i);
                LogMgr.d("反注册上报事件监听成功");
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        byte[] pushByte = (byte[]) msg.obj;
        LogMgr.d("收到上报事件："+ Utils.bytesToString(pushByte) + "mList.size = " + mList.size());
        for (int i = 0; i < mList.size(); i++) {
            PushMsg mPushMsg = (PushMsg) mList.get(i);
//            if (mPushMsg!=null&&(mPushMsg.getEventType()==getPushMsgType(pushByte))) {
            if ( mPushMsg!=null && (mPushMsg.getEventType() == PushMsg.PUSH_TYPE_DEFAULT || mPushMsg.getEventType() == getPushMsgType(pushByte)) ) {
                LogMgr.d("推送上报数据");
                mPushMsg.onPush(pushByte);
            }
        }
    }

    /**
     * 根据主动上报获得的数据判断此次上报的类型
     * @param data
     * @return
     */
    private int getPushMsgType(byte[] data){
        int result = PushMsg.PUSH_TYPE_DEFAULT;

        if( data!=null && data.length>6 && data[0] == GlobalConfig.CMD_0 && data[1] == GlobalConfig.CMD_1_REPORT
                && data[5] == (byte)0xF0 && data[6] == (byte)0x2E){
            //获取到201平衡车主动上报信息
            result = PushMsg.PUSH_TYPE_C201_BLANCE_INFO;
        }

        return result;
    }
}
