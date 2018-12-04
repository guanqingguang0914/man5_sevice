package com.abilix.control.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Control implements Parcelable {
    /*****************************************
     * mControlFuncType参数说明
     * 0:代表brain传给control的是老协议数据 mSendByte[] 保存命令数据
     * 1:代表brain传给control的是新协议数据已经解析完成只包含 命令字1 命令字2 以及 参数
     * 2:代表soul开启或关闭  mModeState; //模式状态 0:关闭 1:开启
     * 3:代表要求执行 AbilixChart 文件 fileFullPath 是完整文件路径名   mModeState; //模式状态 0:关闭 1:开启
     * 4:代表要求执行 AbilixScratch 命令 mSendByte[] 保存命令数据  mModeState; //模式状态 0:关闭 1:开启
     * 5:代表要求执行SkillPlayer 文件 fileFullPath 是完整文件路径名 mModeState; //模式状态 0:关闭 1:开启
     * 6:代表告知当前是哪个系列 mSendByte[] 1个字节代表是哪个系列的 0x01:C系列 0x02:M系列 0x03:H系列 0x04:F系列
     * 7:休息状态切换 mModeState; //模式状态 0:关闭 1:开启
     * 8:代表要求执行 项目编程文件 文件 fileFullPath 是完整文件路径名   mModeState; //模式状态 0:关闭 1:开启
     * 9:代表要求获取固件stm32版本号 control 和当前机器人类型  获取到后 发送广播消息告知版本号
     * 10:代表要求升级stm32固件  mFileFullPath代表升级固件的文件名  文件传输成功后发送广播消息告知
     *****************************************/
    private int mControlFuncType;
    private byte[] mSendByte; // 需要发送的数据
    private String mFileFullPath; // 文件完整路径名
    private int mModeState; //模式状态 0:关闭 1:开启
    private int mCmd;//默认是0，获取AI是1，2否是3.

    public Control(int mControlFuncType, byte[] sendByte) {
        this.mControlFuncType = mControlFuncType;
        this.mSendByte = sendByte;
    }

    public int getmCmd() {
        return mCmd;
    }

    public void setmCmd(int mCmd) {
        this.mCmd = mCmd;
    }

    public int getControlFuncType() {
        return mControlFuncType;
    }

    public void setControlFuncType(int mControlFuncType) {
        this.mControlFuncType = mControlFuncType;
    }

    public byte[] getSendByte() {
        return mSendByte;
    }

    public void setSendByte(byte[] mbyte) {
        this.mSendByte = mbyte;
    }

    public String getFileFullPath() {
        return this.mFileFullPath;
    }

    public void setFileFullPath(String fileFullPath) {
        this.mFileFullPath = fileFullPath;
    }

    public int getModeState() {
        return this.mModeState;
    }

    public void setModeState(int state) {
        this.mModeState = state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mControlFuncType);
        if (mSendByte != null) {
            dest.writeInt(mSendByte.length);
            dest.writeByteArray(mSendByte);
        }
        dest.writeString(mFileFullPath);
        dest.writeInt(mModeState);
        dest.writeInt(mCmd);
    }

    public static final Parcelable.Creator<Control> CREATOR = new Creator<Control>() {

        @Override
        public Control[] newArray(int size) {
            return new Control[size];
        }

        @Override
        public Control createFromParcel(Parcel source) {
            return new Control(source);
        }
    };

    private Control(Parcel source) {
        mControlFuncType = source.readInt();
        mSendByte = new byte[source.readInt()];
        source.readByteArray(mSendByte);
        mFileFullPath = source.readString();
        mModeState = source.readInt();
        mCmd = source.readInt();
    }

}
