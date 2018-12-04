package com.abilix.control.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;

import com.abilix.control.ControlApplication;
import com.abilix.control.utils.LogMgr;

import android.os.Environment;
import android.util.Log;

public class FileUtils {
    public final static String TAG = FileUtils.class.getSimpleName();
    public final static String DATA_PATH = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "Abilix"
            + File.separator + "RobotInfo";

    public final static String DATA_UPDATE = Environment
            .getExternalStorageDirectory() + File.separator + "Podcasts";
    public static final String SSID_PATH = DATA_PATH + File.separator
            + "ssid.txt";
    public static final String LOG_PATH = DATA_PATH + File.separator
            + "log.txt";
    public static final String STMVERSION_PATH = DATA_PATH + File.separator
            + "stmversion.txt";
    public static final String AI_ENVIRONMENT1 = DATA_PATH + File.separator
            + "environment1.txt";
    public static final String AI_ENVIRONMENT2 = DATA_PATH + File.separator
            + "environment2.txt";
    public static final String IO_CONFIG = DATA_PATH + File.separator
            + "ioconfig.txt";
    public static final String ROBOTY_TYPE = DATA_PATH + File.separator
            + "robotType.txt";

    public static void saveFile(String data, String path) {
        File file = new File(path);
        FileOutputStream fo = null;
        try {
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            fo = new FileOutputStream(file);
            fo.write(data.getBytes());
            fo.flush();
            fo.close();
        } catch (IOException e) {
            if (fo != null)
                try {
                    fo.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            e.printStackTrace();
        }

    }

    public static void writeData(String data, String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                LogMgr.e("file not exist ,create new file");
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(osw);
            LogMgr.e("写文件");
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            LogMgr.e(" delete file::" + filePath);
            file.delete();
        }
    }

    public static String readFile(String path) {
        String content = ""; // 文件内容字符串
        File file = new File(path);

        // 如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isDirectory()) {
            Log.d(TAG, "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(
                            instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    while ((line = buffreader.readLine()) != null) {
                        content += line + "\n";
                    }
                    instream.close();
                }
            } catch (java.io.FileNotFoundException e) {
                Log.e(TAG, "The File doesn't not exist.");
            } catch (IOException e) {
                Log.e(TAG, "IOException::" + e.getMessage());
            }
        }
        return content;
    }

    public static void writeLog(String data) {
        try {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File file = new File(sdCardDir.getCanonicalPath() + "/" + "log.txt");
            if (!file.exists()) {
                LogMgr.e("file not exist ,create new file");
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(osw);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /*
    * 判断当前语言环境是否是中文
    */
    public static boolean isCH() {
        Locale locale = ControlApplication.instance.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language != null) {
            if (language.contains("zh"))
                return true;
            else
                return false;
        }
        return false;
    }

    /**
     * 往指定文件写入字符串
     * @param file
     * @param s
     */
    public static void writeFile(File file,String s){
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(s);
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
