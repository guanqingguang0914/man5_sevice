package com.abilix.control.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.media.AudioManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.abilix.control.ControlApplication;
import com.abilix.control.GlobalConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Utils {
    public static byte[] float2byte(float f) {

        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        int len = b.length;
        byte[] dest = new byte[len];
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;

    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);

        return Float.intBitsToFloat(l);
    }

    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24) | ((src[offset + 1] & 0xFF) << 16) | ((src[offset + 2] & 0xFF) << 8) | (src[offset + 3] & 0xFF));
        return value;
    }

    // 低位在前高位在后
    public static int byte2int_2byte(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= (0 << 16);
        l &= 0xffffff;
        l |= (0 << 24);
        return l;
    }
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }
    // 低位在后高位在前
    public static int byte2int_2byteHL(byte[] b, int index) {
        int l;
        l = b[index + 1];
        l &= 0xff;
        l |= ((long) b[index + 0] << 8);
        l &= 0xffff;
        l |= (0 << 16);
        l &= 0xffffff;
        l |= (0 << 24);
        return l;
    }

    public static int byteAray2Int(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0XFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static int byteAray2IntLH(byte[] b) {
        return ((b[0] & 0xFF) | ((b[1] << 8) & 0xFF00) | ((b[2] << 16) & 0xFF0000) | ((b[3] << 24) & 0xFF000000));
    }

    public static int byteArray2IntLH(byte[] b) {
        return ((b[0] & 0xFF)
                | ((b[1] & 0xff) << 8)
                | ((b[2] & 0xff) << 16)
                | ((b[3] & 0xff) << 24));
    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        if (LogMgr.getLogLevel() == LogMgr.NOLOG || bytes.length >100) {
            return "len = " + bytes.length;
        }
        return bytesToHexString(bytes, false);
    }

    public static String intsToString(int[] ints) {
        if(ints == null ){
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<ints.length; i++){
            sb.append(ints[i] + " ");
        }
        return sb.toString();
    }

    /**
     * The digits for every supported radix.
     */
    private static final char[] DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final char[] UPPER_CASE_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    public static String bytesToHexString(byte[] bytes, boolean upperCase) {
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        char[] buf = new char[bytes.length * 3];
        int c = 0;
        for (byte b : bytes) {
            buf[c++] = digits[(b >> 4) & 0xf];
            buf[c++] = digits[b & 0xf];
            buf[c++] = ' ';
        }
        return new String(buf);
    }

    public static Long getCRC32(String fileUri) {
        CRC32 crc32 = new CRC32();
        FileInputStream fileinputstream = null;
        CheckedInputStream checkedinputstream = null;
        Long crc = null;
        try {
            fileinputstream = new FileInputStream(new File(fileUri));
            checkedinputstream = new CheckedInputStream(fileinputstream, crc32);
            while (checkedinputstream.read() != -1) {
            }
            // crc = Long.toHexString(crc32.getValue()).toUpperCase();
            crc = crc32.getValue();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileinputstream != null) {
                try {
                    fileinputstream.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            if (checkedinputstream != null) {
                try {
                    checkedinputstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return crc;
    }

    /**
     * 将int数值转换为占两个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }
    public static byte[] intToBytesLH(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intTo4Bytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public static byte[] readFile(String fileName) throws IOException {
        byte[] buf = null;
        try {
            FileInputStream fin = new FileInputStream(new File(fileName));
            int length = fin.available();
            buf = null;
            buf = new byte[length];
            fin.read(buf);
            fin.close();
        } catch (Exception e) {
            LogMgr.e("write file error::" + e);
            e.printStackTrace();
        }
        return buf;
    }

    public static byte[] floatsToByte(float[] values) {
        byte[] byteX = Utils.float2byte(values[0]);
        byte[] byteY = Utils.float2byte(values[1]);
        byte[] byteZ = Utils.float2byte(values[2]);
        byte[] byte2 = Utils.byteMerger(byteX, byteY);
        byte[] byte3 = Utils.byteMerger(byte2, byteZ);
        return byte3;
    }

    /**
     * 获取bin文件的帧数 老格式的bin文件
     *
     * @param fileName
     * @return
     */
    public static int getFrameNumOfOldBinFile(String fileName) {
        File file = new File(GlobalConfig.DOWNLOAD_PATH + File.separator + fileName);
        long fileLength = file.length();
        // FileInputStream fileRead = new FileInputStream(file);
        // byte[] m_pFileBuff = new byte[(int) fileLength];
        // int nReadLen = fileRead.write(m_pFileBuff);

        return (int) (fileLength / 112);
    }

    /**
     * 获取bin文件的时间 单位 秒
     *
     * @param fileName
     * @return
     */
    public static int getTimeOfBinFile(String fileName) {
        int FrameNum = getFrameNumOfOldBinFile(fileName);
        double time = 1.0 * FrameNum * 20 / 1000;
        int timeInt = (int) Math.ceil(time);
        LogMgr.d("time = " + timeInt + " FrameNum = " + FrameNum);
        return timeInt + 2;
    }

    /*
     * 判断当前语言环境是否是英语
     */
    public static boolean isEn() {
        Locale locale = ControlApplication.instance.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language != null) {
            if (language.endsWith("en"))
                return true;
            else
                return false;
        }
        return false;
    }

    /*
     * 判断当前语言环境bu
     */
    //这函数谁写的？ 是中文返回false？！！！！！！！！！
    public static boolean isZh() {
        Locale locale = ControlApplication.instance.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language != null) {
            if (!language.endsWith("zh"))
                return true;
            else
                return false;
        }
        return false;
    }

    /**
     * 从字节数据指定4字节中获取int
     *
     * @param source 数据源
     * @param start  开始位置
     * @return
     * @throws Exception
     */
    public static int getIntFromByteArray(byte[] source, int start) throws Exception {
        if (start + 4 > source.length) {
            throw new Exception();
        }
        int result;
        result = (int) ((source[start] << 24) | (source[start + 1] << 16) | (source[start + 2] << 8) | (source[start + 3]));
        return result;
    }

    /**
     * 获取现在时间
     *
     * @return 返回时间类型 yyyy-MM-dd HH:mm:ss
     */
    public static String getNowDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        String dateString = formatter.format(currentTime);
        return dateString;
        // ParsePosition pos = new ParsePosition(8);
        // Date currentTime_2 = formatter.parse(dateString, pos);
        // return currentTime_2;
    }

    /**
     * 获取现在时间
     *
     * @return 返回时间类型 MM_dd_HH_mm
     */
    public static String getNowDate1() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_HH_mm");
        String dateString = formatter.format(currentTime);
        return dateString;
        // ParsePosition pos = new ParsePosition(8);
        // Date currentTime_2 = formatter.parse(dateString, pos);
        // return currentTime_2;
    }

    /**
     * 将毫秒转化成固定格式的时间 时间格式: yyyy-MM-dd HH:mm:ss
     *
     * @param millisecond
     * @return
     */
    public static String getDateTimeFromMillisecond(Long millisecond) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(millisecond);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    /**
     * 将毫秒转化成固定格式的时间 时间格式: yyyy-MM-dd HH:mm:ss.SSS
     *
     * @param millisecond
     * @return
     */
    public static String getDateTimeFromMillisecond2(Long millisecond) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date(millisecond);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    /**
     * 获取 SDCard 总容量大小
     *
     * @return
     */
    public static long getTotalSize() {
        String sdcard = Environment.getExternalStorageState();
        String state = Environment.MEDIA_MOUNTED;
        if (sdcard.equals(state)) {
            File file = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(file.getPath());
            // 获得sdcard上 block的总数
            long blockCount = statFs.getBlockCountLong();
            // 获得sdcard上每个block 的大小
            long blockSize = statFs.getBlockSizeLong();
            // 计算标准大小使用：1024，当然使用1000也可以
            long blockTotalSize = blockCount * blockSize / 1024 / 1024;
            LogMgr.i("getTotalSize() 总容量大小 blockTotalSize = " + blockTotalSize + " MB");
            return blockTotalSize;
        } else {
            LogMgr.e("getTotalSize() 存储状态异常 sdcard = " + sdcard + " state = " + state);
            return -1;
        }
    }

    /**
     * 获取 SDCard 剩余容量大小
     *
     * @return
     */
    public static long getExternalAvailableSize() {
        String sdcard = Environment.getExternalStorageState();
        String state = Environment.MEDIA_MOUNTED;
        if (sdcard.equals(state)) {
            File file = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(file.getPath());
            // 获得可供程序使用的Block数量
            long blockAvailable = statFs.getAvailableBlocksLong();
            // 获得sdcard上每个block 的大小
            long blockSize = statFs.getBlockSizeLong();
            // 计算标准大小使用：1024，当然使用1000也可以
            long blockAvailableSize = blockAvailable * blockSize / 1024 / 1024;
            LogMgr.i("getExternalAvailableSize() 剩余容量大小 blockAvailableSize = " + blockAvailableSize + " MB");
            return blockAvailableSize;
        } else {
            LogMgr.e("getExternalAvailableSize() 存储状态异常 sdcard = " + sdcard + " state = " + state);
            return -1;
        }
    }

    /**
     * 校验
     */
    public static byte check(byte[] bs) {
        byte b = 0;
        for (int i = 8; i < bs.length - 3; i++) {
            b += bs[i];
        }
        b = (byte) ~b;
        b = (byte) (b & 0xFF);
        return b;
    }

    /**
     * 获取数据位
     *
     * @param receiveData
     * @return
     */
    public static byte[] getData(byte[] receiveData) {
        if (receiveData[0] != (byte) 0xAA || receiveData[1] != (byte) 0x55) {
            Log.e("lz", "老协议");
            byte[] bs = new byte[56];
            System.arraycopy(receiveData, 0, bs, 0, bs.length);
            return bs;
        }
        byte[] bs = new byte[2];
        System.arraycopy(receiveData, 2, bs, 0, 2);
        int leng = byteToInt(bs);
        byte[] data = new byte[leng - 8];
        System.arraycopy(receiveData, 11, data, 0, data.length);
        return data;
    }

    /**
     * 获取包括数据位的整个协议
     *
     * @param receiveData
     * @return
     */
    public static byte[] getFullData(byte[] receiveData) {
        if (receiveData[0] != (byte) 0xAA || receiveData[1] != (byte) 0x55) {
            Log.e("lz", "老协议");
            byte[] bs = new byte[56];
            System.arraycopy(receiveData, 0, bs, 0, bs.length);
            return null;
        }
        byte[] bs = new byte[2];
        System.arraycopy(receiveData, 2, bs, 0, 2);
        int leng = byteToInt(bs);
        byte[] data = new byte[leng + 4];
        System.arraycopy(receiveData, 0, data, 0, data.length);
        return data;
    }

    /**
     * 2字节byte转int 高位在前 低位在后
     *
     * @param bytes
     * @return
     */
    public static int byteToInt(byte[] bytes) {
        return (int) (((bytes[0] & 0xFF) << 8) | bytes[1] & 0xFF);
    }

    /**
     * 获取 srcArray 中包含的最后一个 objectArray的位置
     *
     * @param objectArray
     * @param srcArray
     * @return srcArray 中包含的最后一个 objectArray的位置 -1代表不存在
     */
    public static int getLastPositionOfArrayInArray(byte[] objectArray, byte[] srcArray) {
        if (objectArray == null || srcArray == null || objectArray.length <= 0 || srcArray.length <= 0 || objectArray.length > srcArray.length) {
            return -1;
        }

        int result = -1;
        for (int i = srcArray.length - objectArray.length; i >= 0; i--) {
            for (int j = 0; j < objectArray.length; j++) {
                if (srcArray[i + j] == objectArray[j]) {
                    if (j == objectArray.length - 1) {
                        result = i;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (result != -1) {
                break;
            }
        }

        return result;

    }
    public static int getSystemStreamVolume(){
        AudioManager mAudioManager = (AudioManager) ControlApplication.instance.getSystemService(Context.AUDIO_SERVICE);
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static boolean appIsDebugable(Application application) {

        try {
            ApplicationInfo info = application.getApplicationInfo();

            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取产品系列号
     *
     * @return
     */
    public static String getProductSerial(String buildDisplay) {
        String result = "";
        try {
            String[] splitResult = buildDisplay.split("_");
            if (splitResult != null && splitResult.length >= 3) {
                result = splitResult[0];
            } else {
                result = buildDisplay.charAt(0) + "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = buildDisplay.charAt(0) + "";
        }
        LogMgr.i("产品系列号 = " + result);
        return result;
    }

    /**
     * 获取核心版本号
     *
     * @return
     */
    public static int getCoreVersionNumber(String buildDisplay) {
        int result = -1;
        try {
            String[] splitResult = buildDisplay.split("_");
            if (splitResult != null && splitResult.length >= 3) {
                String middleResult = splitResult[1];
                String[] splitResult2 = middleResult.split("\\.");
                if (splitResult2 != null && splitResult2.length >= 3) {
                    result = Integer.valueOf(splitResult2[0]);
                } else {
                    result = -1;
                }
            } else {
                result = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = -1;
        }
        LogMgr.i("核心版本号 = " + result);
        return result;
    }

    /**
     * 获取功能版本号
     *
     * @return
     */
    public static int getFunctionVersionNumber(String buildDisplay) {
        int result = -1;
        try {
            String[] splitResult = buildDisplay.split("_");
            if (splitResult != null && splitResult.length >= 3) {
                String middleResult = splitResult[1];
                String[] splitResult2 = middleResult.split("\\.");
                if (splitResult2 != null && splitResult2.length >= 3) {
                    result = Integer.valueOf(splitResult2[1]);
                } else {
                    result = -1;
                }
            } else {
                result = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = -1;
        }
        LogMgr.i("功能版本号 = " + result);
        return result;
    }

    /**
     * 获取升级版本号
     *
     * @return
     */
    public static int getUpdateVersionNumber(String buildDisplay) {
        int result = -1;
        try {
            String[] splitResult = buildDisplay.split("_");
            if (splitResult != null && splitResult.length >= 3) {
                String middleResult = splitResult[1];
                String[] splitResult2 = middleResult.split("\\.");
                if (splitResult2 != null && splitResult2.length >= 3) {
                    result = Integer.valueOf(splitResult2[2]);
                } else {
                    result = -1;
                }
            } else {
                result = -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = -1;
        }
        LogMgr.i("升级版本号 = " + result);
        return result;
    }

    /**
     * 获取编译版本号
     *
     * @return
     */
    public static String getCompileVersionNumber(String buildDisplay) {
        String result = "";
        try {
            String[] splitResult = buildDisplay.split("_");
            if (splitResult != null && splitResult.length >= 3) {
                result = splitResult[2];
            } else {
                result = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        LogMgr.i("编译版本号 = " + result);
        return result;
    }

    //byte 转int  低位在前高位在后
    public static int byte2int_2byteLH(byte[] b, int index) {
        return b[index] & 0xFF | (b[index + 1] & 0XFF) << 8;
    }

    /**
     * 2字节byte转int 高位在前 低位在后
     *
     * @param bytes
     * @return
     */
    public static int byteToIntHL(byte[] bytes, int index) {
        return (int) (((bytes[index] & 0xFF) << 8) | bytes[index + 1] & 0xFF);
    }

    //获取stm32版本号
    public static long getStmVersion(){//得到储存的固件版本号；
        long StmVersion = 0;
        String stm = FileUtils.readFile(FileUtils.STMVERSION_PATH).trim();
        if(!TextUtils.isEmpty(stm) && !stm.equals("-1")){
            StmVersion = Long.parseLong(stm);
        }
        return StmVersion;
    }

    //stm32升级状态
    public final static int STM32_STATUS_NORMAL = 0x00;
    public final static int STM32_STATUS_UPGRADING = 0x01;
    public final static int STM32_STATUS_BOOTLOADER = 0x02;

    private static SharedPreferences mSp = null;
    private static final String SP_CONTROL = "SP_CONTROL";
    private static final String STM32_UPGRADE_STATUS = "stm32_upgrade_status";

    public static void setStm32UpgradeStatus(int status) {
        try {
            if (mSp == null) {
                mSp = ControlApplication.instance.getSharedPreferences(SP_CONTROL, Context.MODE_PRIVATE);
            }
            mSp.edit().putInt(STM32_UPGRADE_STATUS, status).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogMgr.d("setStm32UpgradeStatus() ==> " + status);
    }

    public static int getStm32UpgradeStatus() {
        int status = STM32_STATUS_NORMAL;
        try {
            if (mSp == null) {
                mSp = ControlApplication.instance.getSharedPreferences(SP_CONTROL, Context.MODE_PRIVATE);
            }
            status = mSp.getInt(STM32_UPGRADE_STATUS, STM32_STATUS_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogMgr.d("getStm32UpgradeStatus() ==> " + status);
        return status;
    }


}
