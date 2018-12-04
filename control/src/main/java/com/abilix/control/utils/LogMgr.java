package com.abilix.control.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntRange;
import android.text.TextUtils;
import android.util.Log;

/**
 * 日志工具类
 */
public class LogMgr {

    public static final String SEPARATOR = ",";
    private static final String FILE_PREFIX = "Control";
    private static final String LOG_PREFIX = "Control";
    public static final String LOG_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" +
            File.separator + "system-apps" + File.separator + FILE_PREFIX;
    private static final String LOG_FILE_1 = LOG_FILE_PATH + File.separator + FILE_PREFIX + "Log1.log";
    private static final String LOG_FILE_2 = LOG_FILE_PATH + File.separator + FILE_PREFIX + "Log2.log";

    private static final String MESSAGE_KEY_LOG_LEVEL = "log_levle";
    private static final String MESSAGE_KEY_LOG_HAS_CUSTOM_TAG = "log_has_custom_tag";
    private static final String MESSAGE_KEY_LOG_CUSTOM_TAG = "log_custom_tag";
    private static final String MESSAGE_KEY_LOG_MESSAGE = "log_message";
    private static final String MESSAGE_KEY_LOG_MESSAGE_STACKTRACKELEMENT = "log_stacktrace_element";

    /**
     * 允许开始写log的最小存储剩余空间
     */
    private static final int MIN_AVAILABLE_STORAGE_FOR_LOG = 20;

    public final static int NOLOG = 0;
    public final static int ERROR = 1;
    public final static int WARN = 2;
    public final static int INFO = 3;
    public final static int DEBUG = 4;
    public final static int VERBOSE = 5;


    /**
     * 写入log文件的最低等级
     */
    private static final int LOG_LEVEL_TO_STORE = INFO;

    public static int LOG_LEVEL = VERBOSE;
    public static void setLogLevel(int logLevel) {
        LOG_LEVEL = logLevel;
    }
    public static int getLogLevel() {
        return LOG_LEVEL;
    }
    private static HandlerThread mLogHanlerThread;
    private static Handler mLogHandler;


    /**
     * 单个log文件的最大size，超过后更换另一个log文件
     */
    private final static int MAX_LOG_MB = 5;
    /**
     * 每次写入的log条数，之后读取当前log文件的大小判断是否需要更换log文件
     */
    private final static int MAX_LOG_SIZE_PER_TIME = 5000;


    private static boolean isExportLog = false;
    private static FileOutputStream fosLog;
    private static int logCount = 0;
    private static int currentFileNum;
    private static File logFile1;
    private static File logFile2;

    public synchronized static void startExportLog() {
        if (Utils.getExternalAvailableSize() < MIN_AVAILABLE_STORAGE_FOR_LOG) {
            LogMgr.w("机器剩余大小不足 " + MIN_AVAILABLE_STORAGE_FOR_LOG + " MB,不进行log写入");
            return;
        }
        File logFilePath = new File(LOG_FILE_PATH);
        if (!logFilePath.exists() || (logFilePath.exists() && logFilePath.isFile())) {
            logFilePath.mkdirs();
        }
        logFile1 = new File(LOG_FILE_1);
        logFile2 = new File(LOG_FILE_2);
        try {
            if (!logFile1.exists()) {
                logFile1.createNewFile();
            }
            if (!logFile2.exists()) {
                logFile2.createNewFile();
            }

            if (logFile1.length() < MAX_LOG_MB * 1024 * 1024) {
                fosLog = new FileOutputStream(logFile1, true);
                currentFileNum = 1;
            } else if (logFile1.length() > MAX_LOG_MB * 1024 * 1024 && logFile2.length() < MAX_LOG_MB * 1024 * 1024) {
                fosLog = new FileOutputStream(logFile2, true);
                currentFileNum = 2;
            } else {
                fosLog = new FileOutputStream(logFile1, false);
                currentFileNum = 1;
            }

            mLogHanlerThread = new HandlerThread("Log_HandlerThread");
            mLogHanlerThread.start();
            mLogHandler = new LogHandler(mLogHanlerThread.getLooper());

            isExportLog = true;
        } catch (Exception e) {
            LogMgr.e("开启log输出异常");
            isExportLog = false;
            e.printStackTrace();
        }
    }

    static class LogHandler extends Handler{
        public LogHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            int logLevel = bundle.getInt(MESSAGE_KEY_LOG_LEVEL, VERBOSE);
            boolean hasCustomTag = bundle.getBoolean(MESSAGE_KEY_LOG_HAS_CUSTOM_TAG, false);
            String customTag = null;
            if(hasCustomTag){
                customTag = bundle.getString(MESSAGE_KEY_LOG_CUSTOM_TAG,"no_tag");
            }
            String logMessage = bundle.getString(MESSAGE_KEY_LOG_MESSAGE,"no_message");
            StackTraceElement stackTraceElement = (StackTraceElement) bundle.getSerializable(MESSAGE_KEY_LOG_MESSAGE_STACKTRACKELEMENT);
            writeLog(logLevel,hasCustomTag,customTag,logMessage, stackTraceElement);
        }
    }

    public synchronized static void stopExportLog() {
        if (null != fosLog) {
            try {
                fosLog.close();
            } catch (IOException e) {
                LogMgr.e("关闭log输出异常");
                e.printStackTrace();
            }
        }
        isExportLog = false;
    }

    private synchronized static void exportLog(String logMessage) {
        if (isExportLog == false) {
            return;
        }
        if (null == fosLog) {
            return;
        }
        try {
            if (logCount > MAX_LOG_SIZE_PER_TIME) {
                boolean isNeedToChangeLogFile = false;
                if (currentFileNum == 1) {
                    if (logFile1.length() > MAX_LOG_MB * 1024 * 1024) {
                        isNeedToChangeLogFile = true;
                    }
                } else if (currentFileNum == 2) {
                    if (logFile2.length() > MAX_LOG_MB * 1024 * 1024) {
                        isNeedToChangeLogFile = true;
                    }
                } else {
                    throw new Exception("当前log文件的序号错误1");
                }
                if (isNeedToChangeLogFile) {
                    fosLog.close();
                    if (currentFileNum == 1) {
                        fosLog = new FileOutputStream(logFile2, false);
                        currentFileNum = 2;
                    } else if (currentFileNum == 2) {
                        fosLog = new FileOutputStream(logFile1, false);
                        currentFileNum = 1;
                    } else {
                        throw new Exception("当前log文件的序号错误2");
                    }
                }

                logCount = 0;
            }
            fosLog.write(logMessage.getBytes("UTF-8"));
            logCount++;
        } catch (Exception e) {
            isExportLog = false;
            stopExportLog();
            LogMgr.e("写入log时异常");
            e.printStackTrace();
        }
    }

    /**
     *
     * @param logLevle log等级
     * @param hasCustomTag 是否使用自定义tag
     * @param customTag 自定义tag
     * @param logMessage log信息
     */
    private static void writeLog(int logLevle, boolean hasCustomTag, String customTag, String logMessage, StackTraceElement stackTraceElement){
        if(logLevle <= LOG_LEVEL){
            String tag;
            if(hasCustomTag){
                tag = "abilix-" + customTag;
                if (TextUtils.isEmpty(tag)) {
                    tag = getDefaultTag(stackTraceElement);
                }
            }else{
                tag = getDefaultTag(stackTraceElement);
            }
            switch (logLevle){
                case VERBOSE:
                    if(logLevle <= LOG_LEVEL_TO_STORE){
                        exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " V " + getLogInfo(stackTraceElement) + logMessage + "\n");
                    }
                    Log.v(tag, getLogInfo(stackTraceElement) + logMessage);
                    break;
                case DEBUG:
                    if (logLevle <= LOG_LEVEL_TO_STORE) {
                        exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " D " + getLogInfo(stackTraceElement) + logMessage + "\n");
                    }
                    Log.d(tag, getLogInfo(stackTraceElement) + logMessage);
                    break;
                case INFO:
                    if (logLevle <= LOG_LEVEL_TO_STORE) {
                        exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " I " + getLogInfo(stackTraceElement) + logMessage + "\n");
                    }
                    Log.i(tag, getLogInfo(stackTraceElement) + logMessage);
                    break;
                case WARN:
                    if (logLevle <= LOG_LEVEL_TO_STORE) {
                        exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " W " + getLogInfo(stackTraceElement) + logMessage + "\n");
                    }
                    Log.w(tag, getLogInfo(stackTraceElement) + logMessage);
                    break;
                case ERROR:
                    if (logLevle <= LOG_LEVEL_TO_STORE) {
                        exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " E " + getLogInfo(stackTraceElement) + logMessage + "\n");
                    }
                    Log.e(tag, getLogInfo(stackTraceElement) + logMessage);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 发送消息到Log线程 以打印log
     * @param logLevel
     * @param hasCustomTag
     * @param customTag
     * @param logMessage
     */
    private static void sendMessageToLogThread(@IntRange(from = ERROR, to = VERBOSE) int logLevel, boolean hasCustomTag,
                                               String customTag, String logMessage, StackTraceElement stackTraceElement){
        if(mLogHandler == null){
            return;
        }
        Message message = mLogHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt(MESSAGE_KEY_LOG_LEVEL, logLevel);
        bundle.putBoolean(MESSAGE_KEY_LOG_HAS_CUSTOM_TAG, hasCustomTag);
        bundle.putString(MESSAGE_KEY_LOG_CUSTOM_TAG, customTag);
        bundle.putString(MESSAGE_KEY_LOG_MESSAGE, logMessage);
        bundle.putSerializable(MESSAGE_KEY_LOG_MESSAGE_STACKTRACKELEMENT, stackTraceElement);
        message.setData(bundle);
        mLogHandler.sendMessage(message);
    }

    public static void v(String message) {
//        if (VERBOSE <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            String tag = getDefaultTag(stackTraceElement);
//            if (VERBOSE <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " V " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.v(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(VERBOSE, false, null, message, stackTraceElement);
    }


    public static void v(String tag, String message) {
//        if (VERBOSE <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            tag = "abilix-" + tag;
//            if (TextUtils.isEmpty(tag)) {
//                tag = getDefaultTag(stackTraceElement);
//            }
//            if (VERBOSE <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " V " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.v(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(VERBOSE, true, tag, message, stackTraceElement);
    }

    public static void d(String message) {
//        if (DEBUG <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            String tag = getDefaultTag(stackTraceElement);
//            if (DEBUG <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " D " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.d(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(DEBUG, false, null, message, stackTraceElement);
    }

    public static void d(String tag, String message) {
//        if (DEBUG <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            tag = "abilix-" + tag;
//            if (TextUtils.isEmpty(tag)) {
//                tag = getDefaultTag(stackTraceElement);
//            }
//            if (DEBUG <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " D " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.d(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(DEBUG, true, tag, message, stackTraceElement);
    }

    public static void i(String message) {
//        if (INFO <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            String tag = getDefaultTag(stackTraceElement);
//            if (INFO <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " I " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.i(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(INFO, false, null, message, stackTraceElement);
    }

    public static void i(String tag, String message) {
//        if (INFO <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            tag = "abilix-" + tag;
//            if (TextUtils.isEmpty(tag)) {
//                tag = getDefaultTag(stackTraceElement);
//            }
//            if (INFO <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " I " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.i(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(INFO, true, tag, message, stackTraceElement);
    }

    public static void w(String message) {
//        if (WARN <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            String tag = getDefaultTag(stackTraceElement);
//            if (WARN <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " W " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.w(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(WARN, false, null, message, stackTraceElement);
    }

    public static void w(String tag, String message) {
//        if (WARN <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            tag = "abilix-" + tag;
//            if (TextUtils.isEmpty(tag)) {
//                tag = getDefaultTag(stackTraceElement);
//            }
//            if (WARN <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " W " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.w(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(WARN, true, tag, message, stackTraceElement);
    }

    public static void e(String message) {
//        if (ERROR <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            String tag = getDefaultTag(stackTraceElement);
//            if (ERROR <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " E " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.e(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(ERROR, false, null, message, stackTraceElement);
    }

    public static void e(String tag, String message) {
//        if (ERROR <= LOG_LEVEL) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            if (TextUtils.isEmpty(tag)) {
//                tag = getDefaultTag(stackTraceElement);
//            }
//            if (ERROR <= LOG_LEVEL_TO_STORE) {
//                exportLog(Utils.getDateTimeFromMillisecond2(System.currentTimeMillis()) + " " + tag + " E " + getLogInfo(stackTraceElement) + message + "\n");
//            }
//            Log.e(tag, getLogInfo(stackTraceElement) + message);
//        }
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        sendMessageToLogThread(ERROR, true, tag, message, stackTraceElement);
    }

    /**
     * Get default tag name
     */
    private static String getDefaultTag(StackTraceElement stackTraceElement) {
        String fileName = stackTraceElement.getFileName();
        String stringArray[] = fileName.split("\\.");
        String tag = stringArray[0];
        return LOG_PREFIX + "-" + tag;
    }

    /**
     * get stack info
     */
    private static String getLogInfo(StackTraceElement stackTraceElement) {
        StringBuilder logInfoStringBuilder = new StringBuilder();
        // thread name
        String threadName = Thread.currentThread().getName();
        // thread ID
        long threadID = Thread.currentThread().getId();
        // file name
        String fileName = stackTraceElement.getFileName();
        // class name
        String className = stackTraceElement.getClassName();
        // method
        String methodName = stackTraceElement.getMethodName();
        // code line
        int lineNumber = stackTraceElement.getLineNumber();

        logInfoStringBuilder.append("[ ");
        /*
         * logInfoStringBuilder.append("threadID=" + threadID).append(SEPARATOR);
         * logInfoStringBuilder.append("threadName=" + threadName).append(SEPARATOR);
         * logInfoStringBuilder.append("fileName=" + fileName).append(SEPARATOR);
         * logInfoStringBuilder.append("className=" + className).append(SEPARATOR);
         * logInfoStringBuilder.append("methodName=" + methodName).append(SEPARATOR);
         */
        logInfoStringBuilder.append("lineNumber =" + lineNumber);
        logInfoStringBuilder.append(" ] ");
        methodName = logInfoStringBuilder.toString();
        logInfoStringBuilder = null;
        return methodName;
    }
}
