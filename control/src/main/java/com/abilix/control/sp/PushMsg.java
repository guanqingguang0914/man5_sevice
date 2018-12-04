package com.abilix.control.sp;

/**
 * Created by jingh on 2017/7/11.
 * 注册主动上报数据监廷时，对上传数据的处理类。在onPush中对获取的上报数据进行处理。
 */
public class PushMsg implements Pusher.PushListener {
    public static final int PUSH_TYPE_DEFAULT = 0;
    public static final int PUSH_TYPE_F34 = 1;
    public static final int PUSH_TYPE_C201_BLANCE_INFO = 2;

    private int eventType = PUSH_TYPE_DEFAULT;

    /**
     * 生成
     */
    public PushMsg() {
        this(PUSH_TYPE_DEFAULT);
    }

    public PushMsg(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }


    @Override
    public void onPush(byte[] pushData) {

    }
}
