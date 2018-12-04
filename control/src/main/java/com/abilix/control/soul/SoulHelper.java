package com.abilix.control.soul;

import android.media.AudioManager;
import android.os.Environment;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.control.vedio.Player;
//import com.wlhb.voice.VoiceProvider;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.abilix.control.protocol.ProtocolSender.sendProtocol;

public class SoulHelper {
    private final String audioPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "animals" + File.separator;

    private int STREAM_MAX_VOLUMN = 0;
    private byte[] writebyte4 = new byte[20];
    public byte readbuffer1[] = new byte[40];
    // public byte temp[] = new byte[40];
    private byte[] read = new byte[40];
    private String strInfo;
    private float[] gyro_value;
    private float[] ori_value;

    // 添加这个。
    protected MySensor mSensor;

    //private VoiceProvider mProvider;
    private final double todegree = 180 / Math.PI;
    private float[] acc_value;
    private AudioManager am;
    // private MediaPlayer mediaPlayer;
    private int maxAudio;
    protected int current;
    // private List<Audio> mAudios;
    protected int PlayerStuas = -1;
    protected AudioManager mAudioManager;
    // M系列
    public static byte[] mWheelByte = new byte[20];
    private byte[] mHeadByte = new byte[20];
    private byte[] EYE_COLOR = new byte[20];
    private byte[] EYE_COUNT = new byte[20];
    private byte[] COLOR = new byte[20];
    private byte[] LUMINANCE = new byte[20];
    private byte[] WAVEMODE = new byte[20];
    private byte[] VACUUM = new byte[20];
    private Player mPlayer;

    public SoulHelper() {
        System.arraycopy(ProtocolUtils.VACUUM, 0, VACUUM, 0, ProtocolUtils.VACUUM.length);
        System.arraycopy(ProtocolUtils.WAVEMODE, 0, WAVEMODE, 0, ProtocolUtils.WAVEMODE.length);
        System.arraycopy(ProtocolUtils.LUMINANCE, 0, LUMINANCE, 0, ProtocolUtils.LUMINANCE.length);
        System.arraycopy(ProtocolUtils.COLOR, 0, COLOR, 0, ProtocolUtils.COLOR.length);
        System.arraycopy(ProtocolUtils.EYE_COUNT, 0, EYE_COUNT, 0, ProtocolUtils.EYE_COUNT.length);
        System.arraycopy(ProtocolUtils.EYE_COLOR, 0, EYE_COLOR, 0, ProtocolUtils.EYE_COLOR.length);
        System.arraycopy(ProtocolUtils.WHEELBYTE, 0, mWheelByte, 0, ProtocolUtils.WHEELBYTE.length);
        System.arraycopy(ProtocolUtils.HEADBYTE, 0, mHeadByte, 0, ProtocolUtils.HEADBYTE.length);
    }


    /**
     * @param count 眼睛亮灭个数
     * @param r
     * @param g
     * @param b
     */
    public void setEyeColor(int count, int r, int g, int b) {
        try {
            EYE_COLOR[4] = (byte) r;
            EYE_COLOR[5] = (byte) g;
            EYE_COLOR[6] = (byte) b;

            if (count == 0) {
                Arrays.fill(EYE_COUNT, 4, EYE_COUNT.length, (byte) 0);
                EYE_COLOR[4] = (byte) 0;
                EYE_COLOR[5] = (byte) 0;
                EYE_COLOR[6] = (byte) 0;
            } else if (count == 16) {
                Arrays.fill(EYE_COUNT, 4, EYE_COUNT.length, (byte) 1);
            } else {
                for (int i = 0; i < count; i++) {
                    EYE_COUNT[4 + i] = 1;
                }
                Arrays.fill(EYE_COUNT, 4 + count, EYE_COUNT.length, (byte) 0);
            }
            SP.write(EYE_COUNT);
            TimeUnit.MILLISECONDS.sleep(10);
            SP.write(EYE_COLOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 按新协议发眼睛协议
    public void setEyeColor_new(int count, int r, int g, int b) {
        // 48字节：分别代表眼睛1~16号LED的红绿蓝值。
        byte[] data = new byte[48];
        for (int i = 0; i < count; i++) {
            data[i * 3 + 0] = (byte) r;
            data[i * 3 + 1] = (byte) g;
            data[i * 3 + 2] = (byte) b;
        }
        sendProtocol((byte) 0x02, (byte) 0xA3, (byte) 0x31, data);
    }

    public void turnOutEyeLights() {
        byte[] turn_out_eye_byte = new byte[20];
        System.arraycopy(ProtocolUtils.EYE_COLOR, 0, turn_out_eye_byte, 0, ProtocolUtils.EYE_COLOR.length);
        try {
            LogMgr.e("deng::" + Utils.bytesToString(turn_out_eye_byte));
            SP.request(turn_out_eye_byte);
            SP.request(ProtocolUtils.EYE_COUNT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param mode 0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param r
     * @param g
     * @param b
     */
    public void setColor(int mode, int r, int g, int b) {
        try {
            COLOR[6] = (byte) mode;
            COLOR[7] = (byte) r;
            COLOR[8] = (byte) g;
            COLOR[9] = (byte) b;
            // LogMgr.e("color led value is : "+Utils.bytesToString(COLOR,
            // COLOR.length));
            for (int i = 0; i < 10; i++) {
                SP.write(COLOR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param mode     0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param progress
     */
    public void setLuminance(int mode, int progress) {
        try {
            LUMINANCE[10] = (byte) mode;
            LUMINANCE[11] = (byte) progress;
            for (int i = 0; i < 10; i++) {
                SP.write(LUMINANCE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param mode     0x1 脖子 0x2|0x3 轮子 0x4底部
     * @param wavemode 0x1 正弦波 0x2 宽波 0x3 高电平 0x4 低电平
     */
    public void setWave(int mode, int wavemode) {
        try {
            WAVEMODE[9] = (byte) mode;
            WAVEMODE[10] = (byte) wavemode;
            // LogMgr.e("wave buff is: "+Utils.bytesToString(WAVEMODE,
            // WAVEMODE.length));
            for (int i = 0; i < 10; i++) {
                SP.write(WAVEMODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnOutLights() {
        byte[] turn_out_light_byte = new byte[20];
        System.arraycopy(ProtocolUtils.COLOR, 0, turn_out_light_byte, 0, ProtocolUtils.COLOR.length);
        // for (int i = 0; i < 3; i++) {
        try {
            turn_out_light_byte[6] = 1;
            SP.write(turn_out_light_byte);
            TimeUnit.MILLISECONDS.sleep(8);
            turn_out_light_byte[6] = 2;
            SP.write(turn_out_light_byte);
            TimeUnit.MILLISECONDS.sleep(8);
            turn_out_light_byte[6] = 4;
            SP.write(turn_out_light_byte);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // }
    }

    public void turnOutWave() {
        byte[] turn_out_wavemode_byte = new byte[20];
        System.arraycopy(ProtocolUtils.WAVEMODE, 0, turn_out_wavemode_byte, 0, ProtocolUtils.WAVEMODE.length);
        try {
            // for (int i = 0; i < 6; i++) {
            WAVEMODE[9] = (byte) 1;
            WAVEMODE[10] = (byte) 3;
            SP.write(WAVEMODE);
            TimeUnit.MILLISECONDS.sleep(8);
            WAVEMODE[9] = (byte) 2;
            WAVEMODE[10] = (byte) 3;
            SP.write(WAVEMODE);
            TimeUnit.MILLISECONDS.sleep(8);
            WAVEMODE[9] = (byte) 4;
            WAVEMODE[10] = (byte) 3;
            SP.write(WAVEMODE);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上下脖子电机 progress = -15~30
     */
    public void SetNeckUPMotor(int progress) {
        mHeadByte[8] = 1;
        progress += 15;
        // 角度
        mHeadByte[9] = (byte) Math.abs(progress);
        mHeadByte[10] = 0;
        for (int i = 0; i < 3; i++) {
            try {
                SP.request(mHeadByte);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 左右脖子电机 progress =-130~130
     */
    public void SetNeckLRMotor(int progress) {
        mHeadByte[8] = 0;
        progress += 130;
        if (progress >= 256) {
            if (progress == 256) {
                mHeadByte[10] = 1;
                mHeadByte[9] = 0;
            } else {
                mHeadByte[10] = 1;
                mHeadByte[9] = (byte) (progress - 256);
            }
        } else {
            mHeadByte[9] = (byte) progress;
            mHeadByte[10] = 0;
        }
        for (int i = 0; i < 3; i++) {
            try {
                SP.request(mHeadByte);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止电机
     *
     * @param count 重复次数 默认三次
     */
    protected void stopWheelMoto(int count) {
        try {
            mWheelByte[8] = 100;
            mWheelByte[9] = 100;
            if (count == 0) {
                count = 3;
            }
            // for (int i = 0; i < count; i++) {
            SP.write(mWheelByte);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void resetNeck() {
        byte[] leftAndRight_byte = new byte[20];
        System.arraycopy(ProtocolUtils.HEADBYTE, 0, leftAndRight_byte, 0, ProtocolUtils.HEADBYTE.length);
        byte[] upAndDown_byte = new byte[20];
        System.arraycopy(ProtocolUtils.HEADBYTE, 0, upAndDown_byte, 0, ProtocolUtils.HEADBYTE.length);
        leftAndRight_byte[8] = 0;
        leftAndRight_byte[9] = (byte) 130;
        upAndDown_byte[8] = 1;
        upAndDown_byte[9] = 3;

        try {
            // for (int i = 0; i < 3; i++) {
            SP.write(leftAndRight_byte);
            TimeUnit.MILLISECONDS.sleep(8);
            // }
            // for (int i = 0; i < 3; i++) {
            SP.write(upAndDown_byte);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



