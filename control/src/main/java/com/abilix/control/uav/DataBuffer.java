package com.abilix.control.uav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.abilix.control.utils.LogMgr;

public class DataBuffer {
    public static int UAVType = 1;//机器人类型 1、F，2、AF1许，3、AF2林
    public static long spLastSendTime = 0;//串口最后一次发送数据，发现串口可能会死掉

    private static int dataMinLen = 12;
    private static int limit = 1024 * 2;
    public static byte[] cacheData = new byte[limit];//循环缓冲区
    public static final Lock lockCacheData = new ReentrantLock(); // 锁对象
    private static boolean throwNewData = false;//过limit限制时丢掉新还是旧数据数据
    private static boolean writeLuck = false;//过limit限制
    private static int writeIndex = 0;
    private static int readIndex = 0;

    public static void writeData(byte[] data) {
        if (data == null) return;
        lockCacheData.lock();
        try {
            writeData0(data);
        } catch (Exception e) {
            LogMgr.e("sendData DataBuffer writeData 缓冲区写数据出错   " + e);
        }
        lockCacheData.unlock();
    }

    public static byte[] readData() {
        if (writeIndex - readIndex < dataMinLen) return null;
        byte[] data = null;
        lockCacheData.lock();
        try {
            data = readData0();
        } catch (Exception e) {
            LogMgr.e("sendData DataBuffer readData 缓冲区读数据出错  " + e);
        }
        lockCacheData.unlock();
        return data;
    }

    public static int readAvail() {
        return (writeIndex - readIndex);
    }

    public static int writeAvail() {
        if (writeIndex >= readIndex) return (limit + readIndex - writeIndex);
        else return -1;
    }

    public static void clearBuf() {
        readIndex = 0;
        writeIndex = 0;
    }

    private static void writeData0(byte[] data) {
        int len = data.length;
        if (throwNewData) {//暂时不写入
            if (writeIndex + len - readIndex > limit - 200) {/*//过limit限制可以暂时不写入， 也可以将读指针相应后移，丢掉部分失去时效的数据*/
                if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55) {
                    writeLuck = true;
                }
                if (writeLuck) {
                    LogMgr.e("sendData DataBuffer writeData 缓冲区长度不够数据超出  暂时不写入 ");
                    return;
                }
            } else {
                if (writeLuck) {
                    if (data[0] == (byte) 0xAA && data[1] == (byte) 0x55) writeLuck = false;
                    else return;
                }
            }
        } else {//丢掉部分失去时效的数据
            if (writeIndex + len - readIndex >= limit) {//过limit限制 将读指针相应后移，丢掉部分失去时效的数据
                LogMgr.e("sendData DataBuffer writeData 缓冲区长度不够数据超出  将读指针相应后移，丢掉部分失去时效的数据");
                readIndex = readIndex + len;
            }
        }
        if ((writeIndex % limit + len <= limit)) {
            System.arraycopy(data, 0, cacheData, writeIndex % limit, len);
//			LogMgr.i( " DataBuffer writeData 缓冲区写数据成功  "+11);
            writeIndex = writeIndex + len;
        } else if ((writeIndex % limit < limit) && (writeIndex % limit + len > limit)) {
            System.arraycopy(data, 0, cacheData, writeIndex % limit, (limit - writeIndex % limit));
            System.arraycopy(data, (limit - writeIndex % limit) - 1, cacheData, 0, len - (limit - writeIndex % limit));
            writeIndex = len + writeIndex;
//			LogMgr.i( " DataBuffer writeData 缓冲区写数据成功  "+22);
        }
    }

    private static byte[] readData0() {
        if (writeIndex < readIndex || writeIndex - readIndex > limit) {//指针有问题，只能重置指针
            readIndex = writeIndex;
            return null;
        }
        if (readIndex >= limit && writeIndex >= limit) {
            readIndex = readIndex - limit;
            writeIndex = writeIndex - limit;
        }
        if (writeIndex - readIndex >= dataMinLen) {//协议最小长度
            int i = 0;
            for (i = readIndex; i < writeIndex - 1; i++) {
                if (cacheData[(i) % limit] == (byte) 0xAA && cacheData[(i + 1) % limit] == (byte) 0x55) {
                    if (writeIndex - i < dataMinLen) {
                        readIndex = i;
                        return null;
                    }
                    int len = (((int) 0xff & cacheData[(i + 3) % limit]) | (((int) 0xff & cacheData[(i + 2) % limit]) << 8));
//    				LogMgr.i(len+ " DataBuffer readData 缓冲区读数据成功  "+21+" writeIndex  "+writeIndex+" readIndex "+readIndex+"  i "+i);
                    if (len + 4 <= writeIndex - i) {//长度够
                        byte[] data = new byte[len + 4];
                        if ((i) % limit < (i + len + 4) % limit) {
                            System.arraycopy(cacheData, (i) % limit, data, 0, len + 4);
//    						LogMgr.i( " DataBuffer readData 缓冲区读数据成功  "+31);
                        } else {
                            System.arraycopy(cacheData, (i) % limit, data, 0, limit - (i) % limit);
                            System.arraycopy(cacheData, 0, data, limit - (i) % limit, (len + 4) - (limit - (i) % limit));
//    						LogMgr.i( " DataBuffer readData 缓冲区读数据成功  "+42);
                        }
                        readIndex = i + len + 4;
                        return data;
                    } else {
                        readIndex = i;
                        return null;
                    }
                }
            }
            if ((i - readIndex) > 0)
                LogMgr.e("sendData DataBuffer readData0  缓冲区有部分数据混乱，丢掉数据长度：" + (i - readIndex));
            readIndex = i;
        }
        return null;
    }
}
