package com.abilix.control.vedio;

import java.io.File;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.text.TextUtils;

import com.abilix.control.ControlApplication;
import com.abilix.control.ControlInfo;
import com.abilix.control.ControlInitiator;
import com.abilix.control.GlobalConfig;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

public class Player {
    private final String audioPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix"
            + File.separator + "AbilixMusic" + File.separator;
    private MediaPlayer mediaPlayer;
    private int playingMediaIndex;
    // private List<Audio> mAudios;
    private boolean isPaused = false;
    private AudioManager mAudioManager;
    private AssetManager mAssetManager;
    private final static String S_MUSIC = "music";
    private String musicFilePath;
    private int MaxVolume;
    protected String[] random_music = {"mifeng.mp3", "gangqin.mp3", "xiaohao.mp3", "gudian.mp3", "jita.mp3", "niu.mp3"};

    public Player() {
        mediaPlayer = new MediaPlayer();
        mAudioManager = (AudioManager) ControlApplication.instance.getSystemService(Context.AUDIO_SERVICE);
        mAssetManager = ControlApplication.instance.getAssets();
        MaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_BRIANC
                || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C9 || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_CU) {
            musicFilePath = audioPath + "music_c/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M
                || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_M1) {
            musicFilePath = audioPath + "music_m/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H
                || ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
            musicFilePath = audioPath + "music_h/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_C1_2) {
            musicFilePath = audioPath + "music_c/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_S) {
            musicFilePath = audioPath + "music_s/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_F) {
            musicFilePath = audioPath + "music_f/";
        } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_AF) {
            musicFilePath = audioPath + "music_af/";
        }
    }

    private synchronized void play(int sourceIndex) {

        if (sourceIndex > random_music.length - 1 || sourceIndex < 0) {
            playingMediaIndex = sourceIndex = 0;
        }
        if (sourceIndex < random_music.length - 1) {
            playingMediaIndex = sourceIndex;
        }
        LogMgr.d("music playingMediaIndex is : " + playingMediaIndex);
        String musicName = random_music[playingMediaIndex];
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(musicFilePath + musicName);
            LogMgr.d("music path::" + musicFilePath + musicName);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // LogMgr.e("musicName is:  "+musicName);
        // LogMgr.e("mediaPlayer==null: " +(mediaPlayer==null));
        mediaPlayer.start();
    }

    public synchronized void play() {
        LogMgr.d("play music");
        if (isPaused) {
            mediaPlayer.start();
            isPaused = false;
        } else {
            if (random_music.length > 0) {
                int i = new Random().nextInt(random_music.length);
                play(i);
            }
        }
    }

    public synchronized void play(String name) {
        try {
            stop();
            if (name != null && !"".equals(name)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                if (name.contains("changedansw")) {
                    if (Utils.isZh()) {
                        name = "en_" + name;
                    }
                }
                // AssetFileDescriptor openFd = mAssetManager.openFd(S_MUSIC
                // + File.separator + name);
                mediaPlayer.reset();
                // mediaPlayer.setDataSource(openFd.getFileDescriptor(),
                // openFd.getStartOffset(), openFd.getLength());
                if (name.endsWith(".wav")) {
                    name = name.substring(0, name.lastIndexOf("."));
                    name = name + ".mp3";
                }
                LogMgr.d("music is playing,music path:" + musicFilePath + name);
                mediaPlayer.setDataSource(musicFilePath + name);
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < MaxVolume) {
                    //TODO 2017-5-4 16:00:16 gqg 解决C1播放声音音量变为最大的问题；
//					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, MaxVolume, 0);
//					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 14, 0);
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    public synchronized void play(String name, final IPlayStateListener iPlayStateListener) {
        try {
            stop();
            if (name != null && !"".equals(name)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                if (name.contains("changedansw")) {
                    if (Utils.isZh()) {
                        name = "en_" + name;
                    }
                }
                mediaPlayer.reset();
                if (name.endsWith(".wav")) {
                    name = name.substring(0, name.lastIndexOf("."));
                    name = name + ".mp3";
                }
                LogMgr.d("music is playing,music path:" + musicFilePath + name);
                mediaPlayer.setDataSource(musicFilePath + name);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (iPlayStateListener != null) {
                            iPlayStateListener.onFinished();
                        }
                    }
                });
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        if (iPlayStateListener != null) {
                            iPlayStateListener.onFinished();
                        }
                        return false;
                    }
                });
                mediaPlayer.prepare();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    public synchronized void playRecord(String path, final IPlayStateListener iPlayStateListener) {
        try {
            stop();
            if (!TextUtils.isEmpty(path)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                mediaPlayer.reset();
                mediaPlayer.setDataSource(path);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (iPlayStateListener != null) {
                            iPlayStateListener.onFinished();
                        }
                    }
                });
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        if (iPlayStateListener != null) {
                            iPlayStateListener.onFinished();
                        }
                        return false;
                    }
                });
                mediaPlayer.prepare();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    // 这里是播放录音文件，要修改播放路径。
    public synchronized void playRecord(String path) {
        try {
            stop();
            if (!TextUtils.isEmpty(path)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                // AssetFileDescriptor openFd = mAssetManager.openFd(S_MUSIC +
                // File.separator + name);
                mediaPlayer.reset();
                // String filePath = FileUtils.KNOW_ROBOT_PATH+ File.separator+path;

                mediaPlayer.setDataSource(path);
                // mediaPlayer.setDataSource(openFd.getFileDescriptor(),
                // openFd.getStartOffset(), openFd.getLength());
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < MaxVolume) {
                    //TODO 2017-5-4 16:00:16 gqg 解决C1播放声音音量变为最大的问题；
//					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, MaxVolume, 0);
//					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 14, 0);
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    public synchronized void playFileInDownload(String soundFileName) {
        try {
            stop();
            if (!TextUtils.isEmpty(soundFileName)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                // AssetFileDescriptor openFd = mAssetManager.openFd(S_MUSIC +
                // File.separator + name);
                mediaPlayer.reset();
                String filePath = GlobalConfig.DOWNLOAD_PATH + File.separator + soundFileName;

                mediaPlayer.setDataSource(filePath);
                // mediaPlayer.setDataSource(openFd.getFileDescriptor(),
                // openFd.getStartOffset(), openFd.getLength());
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < MaxVolume) {
                    //TODO 2017-5-4 16:00:16 gqg 解决C1播放声音音量变为最大的问题；
//					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, MaxVolume, 0);
//					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 14, 0);
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    /**
     * 播放音频
     *
     * @param soundFileName 完整路径
     */
    public synchronized void playSoundFile(String soundFileName) {
        try {
            stop();
            if (!TextUtils.isEmpty(soundFileName)) {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                // AssetFileDescriptor openFd = mAssetManager.openFd(S_MUSIC +
                // File.separator + name);
                mediaPlayer.reset();
                // String filePath = GlobalConfig.DOWNLOAD_PATH + File.separator
                // + soundFileName;

                mediaPlayer.setDataSource(soundFileName);
                // mediaPlayer.setDataSource(openFd.getFileDescriptor(),
                // openFd.getStartOffset(), openFd.getLength());
                // if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                // < MaxVolume) {
                // mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                // MaxVolume, 0);
                // mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 14,
                // 0);
                // }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogMgr.e("play music error::" + e);
        }
    }

    /**
     * 暂停播放
     */
    public synchronized void pause() {
        LogMgr.v("Player pause()");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
        }
    }

    /**
     * 继续播放
     */
    public synchronized void resume() {
        LogMgr.v("Player resume()");
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPaused = false;
        }
    }

    public void playNext() {
        play(playingMediaIndex + 1);
    }

    public void playPrevious() {
        play(playingMediaIndex - 1);
    }

    public void setMusicVolume(int index) {
        mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
    }

    public int getMusicVolume() {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 停止播放
     */
    public synchronized void stop() {
        try {
            // mediaPlayer.isPlaying() 还有问题，会出现停止不掉。
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                LogMgr.e("停止播放音频");
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            LogMgr.e("停止播放错误::" + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 设置mediaPlayer的播放完成回调
     *
     * @param onCompletionListener
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

}
