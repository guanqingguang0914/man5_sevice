package com.abilix.control;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.robot.walktunner.GaitAlgorithm;

import java.io.File;
import java.security.InvalidParameterException;

/**
 * Created by yangz on 2017/8/14.
 */

public class H34Activity extends Activity  implements View.OnClickListener {
    private long lastClickTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_h34);

        findViews();


    }

    private void findViews() {
        findViewById(R.id.startMove1).setOnClickListener(this);
        findViewById(R.id.startMove2).setOnClickListener(this);
        findViewById(R.id.startMove3).setOnClickListener(this);
        findViewById(R.id.startMove4).setOnClickListener(this);
        findViewById(R.id.startMove5).setOnClickListener(this);
        findViewById(R.id.startMove6).setOnClickListener(this);
        findViewById(R.id.startMove7).setOnClickListener(this);
        findViewById(R.id.startMove8).setOnClickListener(this);
        findViewById(R.id.stopMoves).setOnClickListener(this);
        findViewById(R.id.exit).setOnClickListener(this);
        findViewById(R.id.make_zero).setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        if (System.currentTimeMillis() - lastClickTime < 300) {
            LogMgr.w("按键过于频繁");
            return;
        }
        lastClickTime = System.currentTimeMillis();

        switch (v.getId()) {
            case R.id.startMove1:
//                startSMove("move1.bin", "music1.mp3", false);
                GaitAlgorithm.getInstance().startLeftWalk();
                break;
            case R.id.startMove2:
//                startSMove("move2.bin", "music2.mp3", true);
                GaitAlgorithm.getInstance().startRightWalk();
                break;
            case R.id.startMove3:
//                startSMove("move3.bin", "music3.mp3", false);
                GaitAlgorithm.getInstance().startForwardWalk();
                break;
            case R.id.startMove4:
//                startSMove("move4.bin", "music4.mp3", false);
                GaitAlgorithm.getInstance().startBackwardWalk();
                break;
            case R.id.startMove5:
//                startSMove("move5.bin", "music5.mp3", false);
                GaitAlgorithm.getInstance().startTurnLeftWalk();
                break;
            case R.id.startMove6:
//                startSMove("move6.bin", "music6.mp3", false);
                GaitAlgorithm.getInstance().startTurnRightWalk();
                break;
            case R.id.startMove7:
                startSMove("move7.bin", "music7.mp3", false);
                break;
            case R.id.startMove8:
                startSMove("move8.bin", "music8.mp3", false);
                break;
            case R.id.make_zero:
                startSStop();
                break;
            case R.id.stopMoves:
//                PlayMoveOrSoundUtils.getInstance().forceStop(false);
                GaitAlgorithm.getInstance().stopWalk();
                break;
            case R.id.exit:
                GaitAlgorithm.getInstance().stopWalk();
                finish();
                break;



            default:
                break;
        }
    }

    /**
     * 测试s动作文件播放
     * <p>
     * lz 2017-3-23
     */
    private void startSMove(String moveName, String musicName, boolean isLoop) {
        String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + moveName;
        String musicPath = Environment.getExternalStorageDirectory().getPath() + File.separator + musicName;
        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(filePath, musicPath, isLoop, false, 0, false,
                PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
    }

    /**
     * 测试s动作文件播放
     * <p>
     * lz 2017-3-23
     */
    private void startSStop() {
        sendOrder();
    }

    /**
     * 发送指令
     */
    private void sendOrder() {
        try {
            byte[] buffer = new byte[66];

            for (int i = 0; i < 22; i++) {
                buffer[i * 3] = (byte) (i + 1);
                byte[] bs = intToByte(512);
                System.arraycopy(bs, 0, buffer, i * 3 + 1, bs.length);
            }
            byte[] bss = buildProtocol(buffer);
            SP.write(bss);
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * 包封装
     *
     * @param data
     */
    private byte[] buildProtocol(byte[] data) {
        // 校验位
        byte[] bs = addProtocol(data);
        Log.e("lz", "bs:" + Utils.bytesToString(bs));
        byte[] bs2 = new byte[bs.length + 2];
        bs2[0] = (byte) 0xfe;
        bs2[1] = (byte) (bs.length + 1);
        System.arraycopy(bs, 0, bs2, 2, bs.length);
        byte check = 0x00; // 校验位
        for (int n = 0; n < bs2.length; n++) {
            check += bs2[n];
        }

        byte[] bs3 = new byte[bs2.length + 3];
        bs3[0] = (byte) 0xff;
        bs3[1] = (byte) 0xff;
        System.arraycopy(bs2, 0, bs3, 2, bs2.length);
        bs3[bs3.length - 1] = (byte) ~check;

        byte[] length = intToByteHigh(bs3.length);

        // 所有数据
        byte[] bs4 = new byte[bs3.length + 8];
        bs4[0] = (byte) 0xfe;
        bs4[1] = (byte) 0x68;
        bs4[2] = (byte) 0x5a;
        bs4[3] = (byte) 0x00;
        bs4[bs4.length - 2] = (byte) 0xaa;
        bs4[bs4.length - 1] = (byte) 0x16;
        System.arraycopy(length, 0, bs4, 4, length.length);
        System.arraycopy(bs3, 0, bs4, 6, bs3.length);
        Log.e("lz", "bs4:" + Utils.bytesToString(bs4));
        return bs4;
    }

    /**
     * int 转byte 高在前低在后
     *
     * @param value
     */
    private byte[] intToByteHigh(int value) {
        byte[] bs = new byte[2];
        bs[0] = (byte) ((value >> 8) & 0xff);
        bs[1] = (byte) (value & 0xff);
        return bs;
    }

    /**
     * 封装舵机指令
     *
     * @param data
     */
    private byte[] addProtocol(byte[] data) {
        byte[] bs = new byte[data.length + 3];
        bs[0] = (byte) 0x83;
        bs[1] = (byte) 0x1e;
        bs[2] = (byte) 0x02;
        System.arraycopy(data, 0, bs, 3, data.length);
        return bs;
    }

    /**
     * int 转byte 低在前高在后
     *
     * @param value
     */
    private byte[] intToByte(int value) {
        byte[] bs = new byte[2];
        bs[1] = (byte) ((value >> 8) & 0xff);
        bs[0] = (byte) (value & 0xff);
        return bs;
    }
}
