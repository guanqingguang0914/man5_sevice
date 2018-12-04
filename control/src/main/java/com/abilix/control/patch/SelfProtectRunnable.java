package com.abilix.control.patch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.utils.LogMgr;

/**
 * H机器人自我保护，将双手恢复至初始位置，防止手指收到损伤
 *
 * @author Yang
 */
public class SelfProtectRunnable implements Runnable {

    private DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * H机器人舵机个数
     */
    private int mServoCount = 4;
    private byte iCount = (byte) mServoCount;
    private byte[] pID = new byte[mServoCount];
    byte[] pPos = new byte[mServoCount * 2];

    private int mServoCountForSpeed = 23;
    private byte iCountForSpeed = (byte) mServoCountForSpeed;
    private byte[] pIDForSpeed = new byte[mServoCountForSpeed];
    byte[] pPosForSpeed = new byte[mServoCountForSpeed * 2];

    public SelfProtectRunnable() {
        super();
//		for(int n=0; n<mServoCount; n++){
//			pID[n] = (byte)(13);   //0~22
//		}
        pID[0] = (byte) (13);
        pID[1] = (byte) (14);
        pID[2] = (byte) (17);
        pID[3] = (byte) (18);

        pPos[0 * 2] = (byte) 0x00;
        pPos[0 * 2 + 1] = (byte) 0x02;
        pPos[1 * 2] = (byte) 0x00;
        pPos[1 * 2 + 1] = (byte) 0x02;
        pPos[2 * 2] = (byte) 0x00;
        pPos[2 * 2 + 1] = (byte) 0x02;
        pPos[3 * 2] = (byte) 0x00;
        pPos[3 * 2 + 1] = (byte) 0x02;

//		pPos[14*2] = (byte) 0x00;
//		pPos[14*2 + 1] = (byte) 0x02;
//		
//		pPos[17*2] = (byte) 0x00;
//		pPos[17*2 + 1] = (byte) 0x02;
//		
//		pPos[18*2] = (byte) 0x00;
//		pPos[18*2 + 1] = (byte) 0x02;

        pIDForSpeed = new byte[mServoCountForSpeed];
        for (int n = 0; n < mServoCountForSpeed; n++) {
            pIDForSpeed[n] = (byte) (n);   //0~22
        }
    }

    @Override
    public void run() {
        //两手恢复到初始位置 防止手指被损坏
        ProtocolUtils.setEngineSpeed(iCountForSpeed, pIDForSpeed, pPosForSpeed, 0);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ProtocolUtils.sendEngineAngles(iCount, pID, pPos);
        LogMgr.d("now1 = " + df.format(new Date()));
    }

}
