package com.abilix.control.uav;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_command_long;
import com.abilix.control.BroadcastResponder;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xiongxin on 2017/9/25.
 * <p>
 * 处理F34 固件升级
 */

public class FBootloaderDataProcess {
    private static FBootloaderDataProcess mFBootDataProcess = null;
    private static final int PROG_MULT_MAX = 240;
    private int bootloaderVersion;
    private int boardId;
    private int boardRevision;
    private int flashSize;
    private long crc;

    private int[] crctab = new int[]{
            0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3,
            0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91,
            0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
            0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5,
            0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
            0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
            0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f,
            0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d,
            0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,
            0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
            0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457,
            0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,
            0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb,
            0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9,
            0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
            0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad,
            0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683,
            0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
            0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7,
            0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
            0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
            0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef, 0x4669be79,
            0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f,
            0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,
            0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
            0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21,
            0x86d3d2d4, 0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,
            0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45,
            0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db,
            0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
            0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf,
            0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d};

    private byte[] crcpad = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    private FBootloaderDataProcess() {
    }

    public static FBootloaderDataProcess getInstance() {
        synchronized (FBootloaderDataProcess.class) {
            if (mFBootDataProcess == null) {
                mFBootDataProcess = new FBootloaderDataProcess();
            }

            return mFBootDataProcess;
        }
    }


    public boolean rebootMavlinkAndUpdateFireware(byte[] firewareBuffer, String filePath, int length, byte[] srcBuffer) {
        byte[] tempReceiver = null;
        boolean result = true;
        try {
            // 升级10步曲，任何一步不对，将跳出循环，升级失败
            loopOut:
            do {
                result = true;
                //第一步：飞控关机，进入bootloader模式
                sendSerialData(packageMAVLinkLong76(246, new float[]{3, 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0}));
                LogMgr.d("===============第一步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(1);
    //            SPReceiver.sIsFUpgrade = true;
                //需要暂停1500等待 重启bootloader
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 第二步： 给bootloader发暂停信号，避免进入mavlink程序
                int count = 0;
                for (; ; ) {
                    tempReceiver = sendSerialDataWithresult(new byte[]{0x21, 0x20 /*33, 32*/});
//                    if (tempReceiver == null) {
//                        continue;
//                    }
                    LogMgr.d("第二步收到的返回数组  : " + Utils.bytesToString(tempReceiver));


                    if (tempReceiver == null || ffByte(tempReceiver[0]) != 0x12 || ffByte(tempReceiver[1]) != 0x10) {
                        if (count < 5) {      //如果boo失tloader没收到，连续发3次，3次没收到， 本次升级败
                            count++;
                            continue;
                        }
                        LogMgr.e("===============第二步返回值为失败===============");
    //                    SPReceiver.sIsFUpgrade = false;
                        result = false;
                        break;
                    } else {
                        break;
                    }
                }
                if (!result){
                    break;
                }
                LogMgr.d("===============第二步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(2);
                Thread.sleep(10);


                //第三步：请求 bootloader 信息
                tempReceiver = sendSerialDataWithresult(new byte[]{0x22, 0x01, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.e("===============第三步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    int bootloaderVersion = Utils.byteArray2IntLH(ver);

                    LogMgr.d("bootloaderVersion  :  " + bootloaderVersion);
                }
                LogMgr.d("===============第三步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(3);
                Thread.sleep(10);

                //第四步:请求 board id 信息
                tempReceiver = sendSerialDataWithresult(new byte[]{0x22, 0x02, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.e("===============第四步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    boardId = Utils.byteArray2IntLH(ver);

                    LogMgr.d("boardId  : " + boardId);
                }
                LogMgr.d("===============第四步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(4);
                Thread.sleep(10);

                //第五步:请求 PCB 修订号 信息
                tempReceiver = sendSerialDataWithresult(new byte[]{0x22, 0x03, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.e("===============第五步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    boardRevision = Utils.byteArray2IntLH(ver);

                    LogMgr.d("boardRevision  :    " + boardRevision);
                }
                LogMgr.d("===============第五步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(5);
                Thread.sleep(10);

                //第六步:请求 flash size 信息
                tempReceiver = sendSerialDataWithresult(new byte[]{0x22, 0x04, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.e("===============第六步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    flashSize = Utils.byteArray2IntLH(ver);

                    LogMgr.d("flashSize :  " + flashSize);
                }

                LogMgr.d("===============第六步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(6);
                Thread.sleep(10);

                //第七步：请求下层固件的CRC
                long curCrc = 0;
                tempReceiver = sendSerialDataWithresult(new byte[]{0x29, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.e("===============第七步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    curCrc = Utils.byteArray2IntLH(ver);

                    LogMgr.d("curCrc  :  " + curCrc);

                }
                //通过将要升级的固件生产CRC 和下层固件对比，不同则进行擦除升级，相同则不升级
                byte[] newFirewareBuffer = new byte[flashSize];
                Arrays.fill(newFirewareBuffer, (byte) 0xff);
                LogMgr.d("length :  " + length);
                LogMgr.d("firewareBuffer :  " + firewareBuffer.length);
                LogMgr.d("newFirewareBuffer :  " + newFirewareBuffer.length);
                System.arraycopy(firewareBuffer, 0, newFirewareBuffer, 0, length);
                crc = uavUtils.crc32(newFirewareBuffer, newFirewareBuffer.length, 0);

                LogMgr.d("crc       :       " + crc);

    //            if (crc == curCrc) {
                if(false){
                    LogMgr.d("-------将要升级的固件生产CRC 和下层固件对比,相同,不升级------");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    break;
                } else {
                    LogMgr.d("-------将要升级的固件生产CRC 和下层固件对比，不同,进行擦除升级-------");
                }

                LogMgr.d("crc (Brain中存放的F更新固件bin的crc):" + crc);
                LogMgr.d("curCrc (当前机器中下层固件的crc):" + curCrc);
                LogMgr.d("===============第七步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(7);
                Thread.sleep(10);

                //第八步:擦除下层固件
                tempReceiver = sendSerialDataWithresult(new byte[]{0x23, 0x20}, 12000);
                if (tempReceiver == null) {
                    LogMgr.e("===============第八步返回值为失败1===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    continue;
                } else {
                    if (tempReceiver[0] != 0x12 | tempReceiver[1] != 0x10) {
                        LogMgr.e("===============第八步返回值为失败2===============");
    //                    SPReceiver.sIsFUpgrade = false;
                        result = false;
                        continue;
                    }
                }
                LogMgr.d("===============第八步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(8);
                Thread.sleep(10);

                //第九步：下载固件，固件按照 PROG_MULT_MAX 大小分包下载
                List<byte[]> multiPackage = __split_len(srcBuffer, PROG_MULT_MAX);
                LogMgr.d("multiPackage.size = " + multiPackage.size());
                LogMgr.d("multiPackage = " + multiPackage.toString());

                for (int i = 0; i < multiPackage.size(); i++) {
                    byte[] signalPackage = multiPackage.get(i);
                    byte[] signalProtocol = new byte[signalPackage.length + 3];
                    signalProtocol[0] = 0x27;
                    signalProtocol[1] = (byte) signalPackage.length;
                    System.arraycopy(signalPackage, 0, signalProtocol, 2, signalPackage.length);
                    signalProtocol[signalProtocol.length - 1] = 0x20;

                    tempReceiver = sendSerialDataWithresult(signalProtocol);
                    LogMgr.d("-发送-    :     " + Utils.bytesToString(signalProtocol));

                    LogMgr.d("                         正在升级    :  " + (100 * (i + 1) / multiPackage.size()) + " %                             ");
                    BroadcastResponder.sendStm32UpdateProgress(8 + 90*(i + 1) / multiPackage.size() );
                    if (tempReceiver == null || tempReceiver[0] != 0x12 || tempReceiver[1] != 0x10) {
                        LogMgr.e("===============第九步返回值为失败===============");
                        //如果发送有问题,需要擦除重新进行升级
    //                    SPReceiver.sIsFUpgrade = false;
                        LogMgr.e("tempReceiver == null is "+(tempReceiver == null));
                        if(tempReceiver != null){
                            LogMgr.e("tempReceiver = "+Utils.bytesToString(tempReceiver));
                        }
                        result = false;
                        continue loopOut;
                    }
                    Thread.sleep(4);
                }
                if (!result){
                    break;
                }
                LogMgr.d("===============第九步成功===============");
                BroadcastResponder.sendStm32UpdateProgress(99);
                Thread.sleep(10);

                //第十步：验证下载固件的CRC
                tempReceiver = sendSerialDataWithresult(new byte[]{0x29, 0x20});
                if (tempReceiver == null || tempReceiver[4] != 0x12 || tempReceiver[5] != 0x10) {
                    LogMgr.d("===============第十步返回值为失败===============");
    //                SPReceiver.sIsFUpgrade = false;
                    result = false;
                    continue;
                } else {

                    byte[] ver = new byte[4];
                    ver[0] = tempReceiver[0];
                    ver[1] = tempReceiver[1];
                    ver[2] = tempReceiver[2];
                    ver[3] = tempReceiver[3];
                    curCrc = Utils.byteArray2IntLH(ver);

                    LogMgr.d("curCrc  :  " + curCrc);

                }
                //通过将要升级的固件生产CRC 和下层固件对比，不同则升级失败，相同则升级成功
                byte[] newFirewareBuffer10 = new byte[flashSize];
                Arrays.fill(newFirewareBuffer10, (byte) 0xff);
                System.arraycopy(firewareBuffer, 0, newFirewareBuffer10, 0, length);
                crc = uavUtils.crc32(newFirewareBuffer10, newFirewareBuffer10.length, 0);

                LogMgr.d("crc (Brain中存放的F更新固件bin的crc):" + crc);
                LogMgr.d("curCrc (当前机器中下层固件的crc):" + curCrc);

                if (crc != curCrc) {
                    LogMgr.d("第十步: 验证更新完的固件版本和升级用的固件版本不一样,升级失败,重新升级");
                    result = false;
                } else {
                    LogMgr.d("第十步: 验证更新完的固件版本和升级用的固件版本一样,升级成功");
                    LogMgr.d("===============第十步成功===============");
                    BroadcastResponder.sendStm32UpdateProgress(100);
                    result = true;

                    //最后一步：重启bootloader
                    sendSerialData(new byte[]{0x30, 0x20});

                    break;
                }


            }
            while (true);
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("升级F固件异常");
            result = false;
        }
        return result;
    }

    private int ffByte(byte b) {
        return b & 0xff;
    }


    public int getFirewareVersion() {
        LogMgr.d("getFirewareVersion");
        if(SP.getUpdateState() == Utils.STM32_STATUS_UPGRADING){
            LogMgr.e("当前飞控正在升级，不执行查询命令");
            return -1;
        }
        byte[] receiver = sendSerialDataWithresult(packageMAVLinkLong76(520, new float[]{1, 0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0}),2*1000);



        if (receiver != null && receiver.length > 25 && receiver[0] == (byte)0xFE) {
            /*int version = (receiver[14] << 24) | (receiver[15] << 16) | (receiver[16] << 8) | receiver[17];

            //TODO 如果不注释,程序虽然不会崩,但是不会往下走了
            LogMgr.d("version  :  " + version + "分别是   :  " + "第1位 :  " + (receiver[14] << 24) + "第2位 :  " + (receiver[15] << 16) + "第3位 :  " + (receiver[16] << 8) + "第4位 :  " + receiver[17]);*/
            LogMgr.d("当前版本号  :  " + Utils.bytesToString(receiver));
            LogMgr.d("receiver[22] = "+receiver[22]+" receiver[23] = "+receiver[23]+" receiver[24] = "+receiver[24]+" receiver[25] = "+receiver[25]);
            //64 64 32(version)
            byte[] ver = new byte[4];
            ver[0] = receiver[22];
            ver[1] = receiver[23];
            ver[2] = receiver[24];
            ver[3] = receiver[25];
            int version = Utils.byteArray2IntLH(ver);

            LogMgr.d("飞控version   :  " + version);

            //  1360212784
            return version;
            //TODO 模拟版本号不一样,进行升级弹框
//            SPReceiver.sIsFUpgrade = true;
//            return 1360212783;
        }else{
            LogMgr.e("receiver is null");
        }
        return -1;
    }

/*    private int __crc32(byte[] bytes, int state) {
        for (byte data : bytes) {
            int index = ((state ^ data) & 0xff);
            state = crctab[index] ^ (state >> 8);
        }
        return state;
    }

    private int genCrc(byte[] fw, int padlen) {
        int state = __crc32(fw, 0);

        for (int i = fw.length; i < (padlen - 1); i += 4) {
            state = __crc32(crcpad, state);
        }
        return state;
    }*/

    private List<byte[]> __split_len(byte[] seq, int length) {
        List<byte[]> answer = new ArrayList<byte[]>();
        int size = length;
        for (int a = 0; a < seq.length; ) {
            byte[] ba = new byte[size];
            System.arraycopy(seq, a, ba, 0, size);
            answer.add(ba);
            a += size;
            if ((seq.length - a) < size){
                size = seq.length - a;
            }
        }
        return answer;
    }


    private byte[] packageMAVLinkLong76(int command, float[] dataFloat, byte[] dataByte) {
        if (dataFloat.length < 7 || dataByte.length < 3) {
            LogMgr.e("MAVLinkDataProcess 发送  长指令长度不够");
            return null;
        }
        msg_command_long mavlinkMessage = new msg_command_long();
        mavlinkMessage.param1 = dataFloat[0];
        mavlinkMessage.param2 = dataFloat[1];
        mavlinkMessage.param3 = dataFloat[2];
        mavlinkMessage.param4 = dataFloat[3];
        mavlinkMessage.param5 = dataFloat[4];
        mavlinkMessage.param6 = dataFloat[5];
        mavlinkMessage.param7 = dataFloat[6];
        mavlinkMessage.command = (short) command;
        mavlinkMessage.target_system = dataByte[0];
        mavlinkMessage.target_component = dataByte[1];
        mavlinkMessage.confirmation = dataByte[2];

        MAVLinkPacket mavlinkPacket = mavlinkMessage.pack();
        byte[] sendbyte0 = mavlinkPacket.encodePacket();//将mavlink包转换成字节数组
        return sendbyte0;

    }

    private byte[] sendSerialDataWithresult(byte[] data) {//发送串口mavlink
        LogMgr.i("sendSerialData 发送串口mavlink");
        if (data != null) {
            return SP.requestOnlyUpdate(data, 5*1000);
        } else {
            LogMgr.i("sendSerialData 发送数据为null");
            return null;
        }
    }

    private byte[] sendSerialDataWithresult(byte[] data, int time) {//发送串口mavlink
        LogMgr.i("sendSerialData 发送串口mavlink");
        if (data != null) {
            return SP.requestOnlyUpdate(data, time);
        } else {
            LogMgr.i("sendSerialData 发送数据为null");
            return null;
        }
    }


    private void sendSerialData(byte[] data) {//发送串口mavlink
        LogMgr.i("sendSerialData 发送串口mavlink   : " + Utils.bytesToString(data));
        if (data != null) {
            SP.fWrite(data);
        } else {
            LogMgr.i("sendSerialData 发送数据为null");
            return;
        }
    }

    public byte[] h16ToByte() {

        String[] ss = {"fe", "21", "01", "ff",
                "00", "4c", "00", "00",
                "40", "40", "00", "03",
                "00", "00", "00", "00",
                "00", "00", "00", "00",
                "00", "00", "00", "00",
                "00", "00", "00", "00",
                "00", "00", "00", "00",
                "00", "00", "F6", "00",
                "01", "01", "00", "70",
                "21"};
        byte[] bs = new byte[41];
        for (int i = 0; i < ss.length; i++) {
            bs[i] = (byte) Integer.parseInt(ss[i], 16);
        }


        LogMgr.i("sendSerialData第一步:  " + Utils.bytesToString(bs));
        LogMgr.i("sendSerialData第一步:  " + Arrays.toString(bs));
        return bs;
    }
}
