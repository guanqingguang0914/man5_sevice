package com.abilix.control.upgrade;

import com.abilix.control.sp.SPReceiver;
import com.abilix.control.uav.FBootloaderDataProcess;
import com.abilix.control.utils.LogMgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by use162 on 2017/9/25.
 */

public class FFirewareUpgrade extends AbstractFirmwareUpgrade {
    private FBootloaderDataProcess bootUpdateProcess = FBootloaderDataProcess.getInstance();

    @Override
    public int getVersion(byte type) {
        return bootUpdateProcess.getFirewareVersion();
    }

    @Override
    public boolean upgrade(byte type, String filePath) {
        LogMgr.i("upgrade type = "+type+" filePath = "+filePath);
//        SPReceiver.sIsFUpgrade = true;

        File updateFile = new File(filePath);
        if (!updateFile.exists()){
            return false;
        }

        try {
            int length = 0;
            //固件大小一般是1M以下
            byte[] buffer = new byte[1024 * 1024];
            FileInputStream fis = new FileInputStream(updateFile);
            length = fis.read(buffer);

            LogMgr.d("length   :   " + length);
            LogMgr.d("buffer.length   :   " + buffer.length);
            byte[] srcBuffer = new byte[length];
            srcBuffer = Arrays.copyOf(buffer, srcBuffer.length);
            return bootUpdateProcess.rebootMavlinkAndUpdateFireware(buffer, filePath ,length ,srcBuffer);
        } catch (FileNotFoundException e) {
            LogMgr.d("升级文件没找到");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getSERVOVersion(byte type) {
        return -1;
    }
}
