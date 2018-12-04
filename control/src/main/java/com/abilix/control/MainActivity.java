package com.abilix.control;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sensor.MySensor;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.FileUtils;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;
import com.abilix.moco.gaitx.kernal.execute.GaitAlgorithmForH5;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements OnClickListener {



    private long lastClickTime;
    //

    //重复播放一个动作 start
    private Button button_repeat_move;
    private Button button_stop_repeat_move;
    //重复播放一个动作 end

    //H5动作测试 start
    private Button button_move1;
    private Button button_move2;
    private Button button_move3;
    private Button button_move4;
    private Button button_move5;
    private Button button_move6;
    private Button button_stop_mvoe;
    private Button button_exit_move;
    private Button button_start_record;
    private Button button_stop_record,btn_choice_testbin,btn_start_testbin,btn_stop_testbin,btn_back_testbin,btn_zero_testbin,btn_move1_testbin;
    //H5动作测试 end

    private Handler mHandler;
    private static final int MSG_WHAT_TEMP = 3;



    private Button mButton_H5_Gait_move;
    private Button mButton_H5_Gait_stop;
    private Button mButton_H5_Gait_stop_Gait;
    private Button mButton_H5_Gait_reset;
    private Button mButton_H5_Gait_setForward;
    private Button mButton_H5_Gait_setBackward;
    private Button mButton_H5_Gait_setSpeedLimit;
    private Button mButton_H5_Gait_exit;

    private VideoView mVideoView;

    private Button mButton_H5_Temp_Begin;
    private Button mButton_H5_Temp_Clear;
    private Button mButton_H5_Temp_Exit;
    private TextView mTextView_H5_Temp_TextView;

    /**
     * 当前选中的舵机
     */
    private int mSelectedEngineNo = 1;
    private int mEngineIDToSet = 1;

    BroadcastReceiver videoBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case BroadcastResponder.BROADCAST_ACTION_VIDEO_CONTROL:
                    switch (intent.getIntExtra(BroadcastResponder.BROADCAST_EXTRA_VIDEO_CONTROL_STATE,PlayMoveOrSoundUtils.VIDEO_CONTROL_STOP)){
                        case PlayMoveOrSoundUtils.VIDEO_CONTROL_STOP:
                            if(mVideoView.isPlaying()){
                                mVideoView.stopPlayback();
                            }
                            mVideoView.setVisibility(View.GONE);
                            break;
                        case PlayMoveOrSoundUtils.VIDEO_CONTROL_PLAY:
                            mVideoView.setVisibility(View.VISIBLE);
                            if(!mVideoView.isPlaying()){
                                String path = intent.getStringExtra(BroadcastResponder.BROADCAST_EXTRA_VIDEO_CONTROL_PATH);
                                LogMgr.e("path = "+path);
                                mVideoView.setVideoPath(path);
                                mVideoView.start();
                            }
                            break;
                        case PlayMoveOrSoundUtils.VIDEO_CONTROL_PAUSE:
                            if(mVideoView.isPlaying()){
                                mVideoView.pause();
                            }
                            break;
                        case PlayMoveOrSoundUtils.VIDEO_CONTROL_RESUME:
                            if(mVideoView.isPlaying()){
                                mVideoView.start();
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };
    IntentFilter intentFilter = new IntentFilter(BroadcastResponder.BROADCAST_ACTION_VIDEO_CONTROL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        findViews();
        registerReceiver(videoBroadcastReceiver,intentFilter);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_WHAT_TEMP:
                        StringBuilder sb = new StringBuilder();
                        sb.append("最高电流：\n");
                        if(list!=null){
                            for(int i = 0;i<list.size();i++){
                                sb.append(list.get(i)+" ");
                            }
                        }
                        sb.append("\n平均电流："+avgOfTemp);
                        sb.append("\n获取次数："+countOfTemp);
                        mTextView_H5_Temp_TextView.setText(sb.toString());
                        break;
                    default:
                        break;
                }
            }
        };
//        MySensor.obtainMySensor(MainActivity.this).openSensorEventListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(videoBroadcastReceiver);
        if(mVideoView!=null){
            mVideoView.suspend();
        }
        if(scheduledExecutorService!=null && !scheduledExecutorService.isShutdown()){
            scheduledExecutorService.shutdown();
        }
        if(timerTask!=null){
            timerTask.cancel();
            timerTask = null;
        }
    }



    private void findViews() {

        button_repeat_move = (Button) findViewById(R.id.button_repeat_move);
        button_stop_repeat_move = (Button) findViewById(R.id.button_stop_repeat_move);

        button_move1 = (Button) findViewById(R.id.button_move1);
        button_move2 = (Button) findViewById(R.id.button_move2);
        button_move3 = (Button) findViewById(R.id.button_move3);
        button_move4 = (Button) findViewById(R.id.button_move4);
        button_move5 = (Button) findViewById(R.id.button_move5);
        button_move6 = (Button) findViewById(R.id.button_move6);
        button_stop_mvoe = (Button) findViewById(R.id.button_stopmove);
        button_exit_move = (Button) findViewById(R.id.exit_move_h);
        button_start_record = (Button) findViewById(R.id.button_start_record);
        button_stop_record = (Button) findViewById(R.id.button_stop_record);

        btn_choice_testbin = (Button) findViewById(R.id.btn_choice_testbin);
        btn_start_testbin = (Button) findViewById(R.id.btn_start_testbin);
        btn_stop_testbin = (Button) findViewById(R.id.btn_stop_testbin);
        btn_back_testbin = (Button) findViewById(R.id.btn_back_testbin);
        btn_zero_testbin = (Button) findViewById(R.id.btn_zero_testbin);
        btn_move1_testbin = (Button) findViewById(R.id.btn_move1_testbin);

        btn_choice_testbin.setOnClickListener(this);
        btn_start_testbin.setOnClickListener(this);
        btn_stop_testbin.setOnClickListener(this);
        btn_back_testbin.setOnClickListener(this);
        btn_move1_testbin.setOnClickListener(this);
        btn_zero_testbin.setOnClickListener(this);

        mButton_H5_Gait_move = (Button)findViewById(R.id.H5_Gait_move);
        mButton_H5_Gait_stop = (Button)findViewById(R.id.H5_Gait_stop);
        mButton_H5_Gait_stop_Gait = (Button)findViewById(R.id.H5_Gait_stop_Gait);
        mButton_H5_Gait_reset = (Button)findViewById(R.id.H5_Gait_reset);
        mButton_H5_Gait_setForward = (Button)findViewById(R.id.H5_Gait_set_forward);
        mButton_H5_Gait_setBackward = (Button)findViewById(R.id.H5_Gait_set_backward);
        mButton_H5_Gait_setSpeedLimit = (Button)findViewById(R.id.H5_Gait_speed_limit);
        mButton_H5_Gait_exit = (Button)findViewById(R.id.H5_Gait_exit);

        mButton_H5_Temp_Begin = (Button)findViewById(R.id.H5_temp_begin);
        mButton_H5_Temp_Clear = (Button)findViewById(R.id.H5_temp_clear);
        mButton_H5_Temp_Exit = (Button)findViewById(R.id.H5_temp_exit);
        mTextView_H5_Temp_TextView = (TextView)findViewById(R.id.H5_temp_textView);

        mVideoView = (VideoView)findViewById(R.id.video_view);
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                BroadcastResponder.sendVedioControl(PlayMoveOrSoundUtils.VIDEO_CONTROL_STOP,null);
                return true;
            }
        });

        button_repeat_move.setOnClickListener(this);
        button_stop_repeat_move.setOnClickListener(this);

        button_move1.setOnClickListener(this);
        button_move2.setOnClickListener(this);
        button_move3.setOnClickListener(this);
        button_move4.setOnClickListener(this);
        button_move5.setOnClickListener(this);
        button_move6.setOnClickListener(this);
        button_stop_mvoe.setOnClickListener(this);
        button_exit_move.setOnClickListener(this);
        button_start_record.setOnClickListener(this);
        button_stop_record.setOnClickListener(this);

        mButton_H5_Gait_move.setOnClickListener(this);
        mButton_H5_Gait_stop.setOnClickListener(this);
        mButton_H5_Gait_stop_Gait.setOnClickListener(this);
        mButton_H5_Gait_reset.setOnClickListener(this);
        mButton_H5_Gait_setForward.setOnClickListener(this);
        mButton_H5_Gait_setBackward.setOnClickListener(this);
        mButton_H5_Gait_setSpeedLimit.setOnClickListener(this);
        mButton_H5_Gait_exit.setOnClickListener(this);

        mButton_H5_Temp_Begin.setOnClickListener(this);
        mButton_H5_Temp_Clear.setOnClickListener(this);
        mButton_H5_Temp_Exit.setOnClickListener(this);


    }
    private String sendBinName = "";
    private String moveBinF = Environment.getExternalStorageDirectory().getPath() + File.separator;
    private File[] getFilePath(String path) {
        File[] filess;
        try {
            File file = new File(path);
            if (!file.exists()) {
                LogMgr.e("文件不存在");
                Toast.makeText(MainActivity.this, "文件夹不存在:" + path, Toast.LENGTH_SHORT).show();
                try {
                    file.mkdirs();//创建文件夹
//                    file.createNewFile();//创建文件
                } catch (Exception e) {
                    LogMgr.e("创建文件失败 " + e);
                    e.printStackTrace();
                }
                return null;
            }
            filess = file.listFiles();
            LogMgr.e("filess:" + filess.length);
            List<File> fileList = new ArrayList<>();

            if(fileList!= null) fileList.clear();
            for (int j = 0; j < filess.length; j++) {
                if(!filess[j].isDirectory()){
                    fileList.add(filess[j]);
                }
            }
            LogMgr.e("fileList:" + fileList.size());
            File[] filesss = new File[fileList.size()];
            for (int j = 0; j < fileList.size(); j++) {
                filesss[j] = fileList.get(j);
            }
            LogMgr.e("filesss:" + filesss.length);
            return filesss;
        } catch (Exception e) {
            LogMgr.e("path:" + path + "  获取文件出错 " + e);
            return null;
        }
    }
    @Override
    public void onClick(View v) {
        if (System.currentTimeMillis() - lastClickTime < 300) {
            LogMgr.w("按键过于频繁");
            return;
        }
        lastClickTime = System.currentTimeMillis();

        switch (v.getId()) {
            case R.id.button_repeat_move:
                String path0 = Environment.getExternalStorageDirectory().getPath() + File.separator + "Download" + File.separator + "skill.bin";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(path0, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.button_stop_repeat_move:
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                break;
            case R.id.btn_move1_testbin:
            case R.id.button_move1:
                String hpath1 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move1.bin";
                if(hpath1.isEmpty()){
                    Toast.makeText(MainActivity.this,"move1.bin不存在",Toast.LENGTH_SHORT).show();
                }
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath1, null, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.button_move2:
                String hpath2 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move2.bin";
                String hpath_music2 = Environment.getExternalStorageDirectory().getPath() + File.separator + "music2.mp3";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath2, hpath_music2, true, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.button_move3:
                String hpath3 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move3.bin";
                String hpath1_video3 = Environment.getExternalStorageDirectory().getPath() + File.separator + "video3.mp4";
//                String hpath_music3 = Environment.getExternalStorageDirectory().getPath() + File.separator + "music3.mp4";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath3, hpath1_video3, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_VEDIO, false, true, null);
                break;
            case R.id.button_move4:
                String hpath4 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move4.bin";
                String hpath1_video4 = Environment.getExternalStorageDirectory().getPath() + File.separator + "video4.mp4";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath4, hpath1_video4, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_VEDIO, false, true, null);
                break;
            case R.id.button_move5:
                String hpath5 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move5.bin";
                String hpath_music5 = Environment.getExternalStorageDirectory().getPath() + File.separator + "music5.mp3";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath5, hpath_music5, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.button_move6:
                String hpath6 = Environment.getExternalStorageDirectory().getPath() + File.separator + "move6.bin";
                String hpath_music6 = Environment.getExternalStorageDirectory().getPath() + File.separator + "music6.mp3";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(hpath6, hpath_music6, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.btn_choice_testbin:
                File[] fileMove = getFilePath(moveBinF);
                List<String> fileName = new ArrayList<>();
                for (int i = 0; i < fileMove.length; i++) {
                    if(fileMove[i].getName().endsWith(".bin")){
                        fileName.add(fileMove[i].getName());
                    }
                }
                final String[] fileBinName = new String[fileName.size()];
                for (int i = 0; i < fileName.size(); i++) {
                    fileBinName[i] = fileName.get(i);
                }
                new AlertDialog.Builder(this).setTitle("动作选择")
                        .setItems(fileBinName, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendBinName = fileBinName[which];
                                btn_choice_testbin.setText(fileBinName[which]);
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
                break;
            case R.id.btn_start_testbin:
                String moveBinName = Environment.getExternalStorageDirectory().getPath() + File.separator  + sendBinName;
                String musicName = Environment.getExternalStorageDirectory().getPath() + File.separator  + Utils.getFileNameNoEx(sendBinName) + ".mp3";
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(moveBinName, musicName, false, false, 0, false,
                        PlayMoveOrSoundUtils.PLAY_MODE_NORMAL, false, true, null);
                break;
            case R.id.btn_zero_testbin:
                String movePath = null;
                if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H3) {
                    movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_BOOT_RECOVER;
                } else if (ControlInfo.getMain_robot_type() == ControlInitiator.ROBOT_TYPE_H) {
                    movePath = GlobalConfig.MOVE_BIN_PATH + File.separator + GlobalConfig.MOVE_BOOT_RECOVER_H;
                }
                PlayMoveOrSoundUtils.getInstance().handlePlayCmd(movePath, null, false, false, 0, false, PlayMoveOrSoundUtils.PLAY_MODE_DEFAULT, false, true, new PlayMoveOrSoundUtils.PlayCallBack() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onResume() {

                    }

                    @Override
                    public void onStop() {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                PlayMoveOrSoundUtils.getInstance().setEngineSpeed(0);
                            }
                        }, 2500);

                    }

                    @Override
                    public void onSingleMoveStopWhileLoop() {

                    }

                    @Override
                    public void onNormalStop() {

                    }

                    @Override
                    public void onForceStop() {

                    }
                });
                break;
            case R.id.button_start_record:
//                Toast.makeText(MainActivity.this,"开始记录舵机信息",Toast.LENGTH_LONG).show();
//                startGet22EngineLoadTimer();
                Toast.makeText(MainActivity.this,"开始记录陀螺仪信息",Toast.LENGTH_LONG).show();
                startGetGyrInfo();
                break;
            case R.id.button_stop_record:
//                Toast.makeText(MainActivity.this,"停止记录舵机信息",Toast.LENGTH_LONG).show();
//                stopGet22EngineLoadTimer();
                Toast.makeText(MainActivity.this,"停止记录陀螺仪信息",Toast.LENGTH_LONG).show();
//                MySensor.obtainMySensor(MainActivity.this).isOutPutGyr = false;
                break;
            case R.id.btn_stop_testbin:
            case R.id.button_stopmove:
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                break;
            case R.id.btn_back_testbin:
            case R.id.exit_move_h:
                MainActivity.this.finish();
                break;
            case R.id.H5_Gait_move:
                GaitAlgorithmForH5.getInstance().move(0 ,1.1);
                break;
            case R.id.H5_Gait_stop:
                GaitAlgorithmForH5.getInstance().stopMove();
                break;
            case R.id.H5_Gait_stop_Gait:
//                GaitAlgorithmForH5.getInstance().stopGait();
                break;
            case R.id.H5_Gait_reset:
//                GaitAlgorithmForH5.getInstance().reset();
                break;
            case R.id.H5_Gait_set_forward:
                GaitAlgorithmForH5.getInstance().setForward();
                break;
            case R.id.H5_Gait_set_backward:
                GaitAlgorithmForH5.getInstance().setBackward();
                break;
            case R.id.H5_Gait_speed_limit:
                GaitAlgorithmForH5.getInstance().setSpeedLimit(10);
                break;
            case R.id.H5_Gait_exit:
                MainActivity.this.finish();
                break;
            case R.id.H5_temp_begin:
                beginToGetTemp();

                break;
            case R.id.H5_temp_clear:
                PlayMoveOrSoundUtils.getInstance().stopCurrentMove();
                if(timerTask!=null){
                    timerTask.cancel();
                    timerTask = null;
                }
                scheduledExecutorService.shutdown();
                list.clear();
                mTextView_H5_Temp_TextView.setText("最高电流：");
                break;
            case R.id.H5_temp_exit:
                MainActivity.this.finish();
                break;
            default:
                break;
        }
    }

    /**获取舵机数据的保存文件名*/
    public static String mFileNameForGyr;
    private void startGetGyrInfo() {
//        stopGet22EngineLoadTimer();
//        get22EngineLoadCount = 0;
        String currentTime = Utils.getNowDate1();
        mFileNameForGyr = "Gyr_"+currentTime+".csv";
        File outFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + mFileNameForGyr);
        if(!outFile.exists()){
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                LogMgr.e("创建文件失败1 mFileName = "+mFileNameForGyr);
                e.printStackTrace();
                return;
            }
        }else{
            outFile.delete();
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                LogMgr.e("创建文件失败2 mFileName = "+mFileNameForGyr);
                e.printStackTrace();
                return;
            }
        }
//        String s = Utils.getNowDate() + "\n";
//        FileUtils.writeFile(outFile,s);
//        MySensor.obtainMySensor(MainActivity.this).isOutPutGyr = true;
//        mTimerForLoad = new Timer();
//        mTimerTaskForLoad = new TimerTask() {
//            @Override
//            public void run() {
//                get22EngineLoadCount++;
//                if(get22EngineLoadCount > LastTimeForLoadTimer*60*(1000/mTimerPeriodForLoad)){
//                    LogMgr.i("时间到，获取数据结束");
//                    stopGet22EngineLoadTimer();
//                    return;
//                }
//
//                getAndWriteEngineLoad();
//
//            }
//        };
//        mTimerForLoad.schedule(mTimerTaskForLoad, 0, mTimerPeriodForLoad);
    }

    //    private Tim
    private ScheduledExecutorService scheduledExecutorService;
//    private Timer timer;
    private TimerTask timerTask;
    private List<Integer> list = new LinkedList<>();
    private final int MAX_LIST_SIZE = 5;
    private int countOfTemp = 0;
    private int sumOfTemp = 0;
    private int avgOfTemp = 0;

    private void beginToGetTemp() {
        final byte[] cmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),(byte)0xA3,(byte)0x7F,null);
//        list.clear();
//        list.addAll()
        String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "Abilix" + File.separator + "app_store" + File.separator + "Dianliuwu.bin";
        PlayMoveOrSoundUtils.getInstance().handlePlayCmd(path,
                null,false,false,0,
                false,PlayMoveOrSoundUtils.PLAY_MODE_NORMAL,false,false,null);
        if(scheduledExecutorService!=null && !scheduledExecutorService.isShutdown()){
            scheduledExecutorService.shutdown();
        }
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        countOfTemp = 0;
        sumOfTemp = 0;
        avgOfTemp = 0;
        if(timerTask!=null){
            timerTask.cancel();
            timerTask = null;
        }
//        if(timer!=null){
//            timer.cancel();
//            timer = null;
//        }
//        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                byte[] data = SP.request(cmd,10);
                LogMgr.i("获取电流返回 = "+ Utils.bytesToString(data));
                if(data != null && data.length >= 12 && data[5] == (byte)0xF0 && data[6] == (byte)0x6E){
//                    int[] tempData = new int[22];
                    int tempNow = (int)(  ((data[12]&0xFF) << 8) | (data[13]&0xFF)   );
                    LogMgr.i("获取电流返回 tempNow "+ tempNow);
//                    for(int i = 1;i<=22;i++){
//                        tempData[i] = (int)(((data[9 + i*2]&0xFF) << 8) | (data[9 + i*2 + 1]&0xFF));
//                        if(tempData[i] > maxOf22){
//                            maxOf22 = tempData[i];
//                        }
//                    }
//                    if(list.size() == 0){
//                        list.add(tempNow);
//                    }else{
                    countOfTemp++;
                    sumOfTemp+=tempNow;
                    avgOfTemp = sumOfTemp/countOfTemp;
                        for(int i = 0;i<MAX_LIST_SIZE;i++){
//                            if(i>=list.size()){
//                                list.add(tempNow);
//                                break;
//                            }
                            if(i>=list.size() || tempNow > list.get(i)){
                                list.add(i,tempNow);

                                if(list.size()>MAX_LIST_SIZE){
                                    list.remove(MAX_LIST_SIZE);
                                }
                                break;
                            }
                        }
//                    }

                    mHandler.sendEmptyMessage(MSG_WHAT_TEMP);
                }else{
                    LogMgr.e("获取电量返回异常");
                }
            }
        };
//        timer.schedule(timerTask, 100, 2 * 1000);
        scheduledExecutorService.scheduleAtFixedRate(timerTask, 100, 1 * 100, TimeUnit.MILLISECONDS);;
    }


    private static Timer mTimerForLoad;
    private static TimerTask mTimerTaskForLoad;
    /**获取舵机数据的时间间隔 ms*/
    private int mTimerPeriodForLoad = 300;
    /**获取舵机数据的次数*/
    private int get22EngineLoadCount = 0;
    /**获取舵机数据的保存文件名*/
    private String mFileName;
    /**获取舵机数据持续分钟数*/
    private int LastTimeForLoadTimer = 30;
    private void startGet22EngineLoadTimer() {
        stopGet22EngineLoadTimer();
        get22EngineLoadCount = 0;
        String currentTime = Utils.getNowDate1();
        mFileName = "Engine_"+currentTime+".csv";
        File outFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + mFileName);
        if(!outFile.exists()){
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                LogMgr.e("创建文件失败1 mFileName = "+mFileName);
                e.printStackTrace();
                return;
            }
        }else{
            outFile.delete();
            try {
                outFile.createNewFile();
            } catch (IOException e) {
                LogMgr.e("创建文件失败2 mFileName = "+mFileName);
                e.printStackTrace();
                return;
            }
        }
        String s = " ," + Utils.getNowDate() + ",No.1C,No.1T,No.2C,No.2T,No.3C,No.3T,No.4C,No.4T,No.5C,No.5T,No.6C,No.6T,No.7C,No.7T,No.8C,No.8T,No.9C,No.9T,No.10C,No.10T," +
                "No.11C,No.11T,No.12C,No.12T,No.13C,No.13T,No.14C,No.14T,No.15C,No.15T,No.16C," +
                "No.16T,No.17C,No.17T,No.18C,No.18T,No.19C,No.19T,No.20C,No.20T,No.21C,No.21T,No.22C,No.22T\n";
        FileUtils.writeFile(outFile,s);
        mTimerForLoad = new Timer();
        mTimerTaskForLoad = new TimerTask() {
            @Override
            public void run() {
                get22EngineLoadCount++;
                if(get22EngineLoadCount > LastTimeForLoadTimer*60*(1000/mTimerPeriodForLoad)){
                    LogMgr.i("时间到，获取数据结束");
                    stopGet22EngineLoadTimer();
                    return;
                }

                getAndWriteEngineLoad();

            }
        };
        mTimerForLoad.schedule(mTimerTaskForLoad, 0, mTimerPeriodForLoad);
    }

    private void stopGet22EngineLoadTimer(){
        if(mTimerForLoad != null){
            mTimerForLoad.cancel();
        }
        if(mTimerTaskForLoad != null){
            mTimerTaskForLoad.cancel();
        }
    }

    /**
     * 获取舵机的电流病写入文件
     */
    protected synchronized void getAndWriteEngineLoad() {
        if(TextUtils.isEmpty(mFileName)){
            LogMgr.e("TextUtils.isEmpty(mFileName) is true");
            return;
        }
        File outFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + mFileName);
        if(!outFile.exists()){
            LogMgr.e("写数据时文件不存在，stop");
            stopGet22EngineLoadTimer();
            return;
        }
//        byte[] tempCmdForCurrent = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
//                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_CURRENT_FIMRWARE_OUT_CMD_2, null);
//        byte[] tempCmdForTemperature = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
//                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1, GlobalConfig.ENGINE_TEMPERATURE_FIMRWARE_OUT_CMD_2, null);
//        byte[] bufferForCurrent;
//        byte[] bufferForTemperature;

        byte[] engineInfo;
        byte[] getEngineInfoCmd = ProtocolUtils.buildProtocol((byte)ControlInfo.getMain_robot_type(),
                GlobalConfig.ENGINE_FIRMWARE_OUT_CMD_1,GlobalConfig.ENGINE_GET_ENGINE_INFO_OUT_CMD_2,new byte[]{(byte)0x01,(byte)0x02} );

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(""+get22EngineLoadCount+","+Utils.getNowDate());

            //开始获取数据
            engineInfo = SP.request(getEngineInfoCmd,200);
            LogMgr.v("所有舵机信息的命令 获取到的数据 = " + Utils.bytesToString(engineInfo));
            byte[] dataOfEngineInfo = new byte[66];
            if(engineInfo!=null && engineInfo.length>=80 && engineInfo[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1
                    && engineInfo[6] == GlobalConfig.ENGINE_GET_ENGINE_INFO_CMD_IN_CMD_2){
                System.arraycopy(engineInfo, 13, dataOfEngineInfo, 0,dataOfEngineInfo.length);
            }
            //开始获取电流数据
//            bufferForCurrent = SP.request(tempCmdForCurrent,100);
//            LogMgr.v("所有舵机负载的命令 获取到的数据1 = " + Utils.bytesToString(bufferForCurrent));
//
//            byte[] dataOf22EngineCurrent = new byte[44];
//            if(bufferForCurrent!=null && bufferForCurrent.length>=56 && bufferForCurrent[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1
//                    && bufferForCurrent[6] == GlobalConfig.ENGINE_CURRENT_FIMRWARE_IN_CMD_2){
//                System.arraycopy(bufferForCurrent, 11, dataOf22EngineCurrent, 0,dataOf22EngineCurrent.length);
//            }

            //开始获取温度数据
//            bufferForTemperature = SP.request(tempCmdForTemperature,100);
//            LogMgr.v("所有舵机负载的命令 获取到的数据1 = " + Utils.bytesToString(bufferForTemperature));
//            byte[] dataOf22EngineTemperatur = new byte[22];
//            if(bufferForCurrent!=null && bufferForCurrent.length>=34 && bufferForCurrent[5] == GlobalConfig.ENGINE_FIRMWARE_IN_CMD_1
//                    && bufferForCurrent[6] == GlobalConfig.ENGINE_TEMPERATURE_FIMRWARE_IN_CMD_2){
//                System.arraycopy(bufferForTemperature, 11, dataOf22EngineTemperatur, 0,dataOf22EngineTemperatur.length);
//            }

//            int currentOfTheEngine = 0, temperatureOfTheEngine = 0 ;
            for (int d = 1; d <= 22; d++) {
                int current = (int) (((dataOfEngineInfo[20 + d*2] & 0xFF) << 8) | (dataOfEngineInfo[20 + d*2 + 1] & 0xFF));
                int temperature = (int)(dataOfEngineInfo[d - 1]);
                LogMgr.d("舵机号 = " + d + " 电流 = " + current+" 温度 = "+temperature);

                sb.append("," + current + "," + temperature);
//                if(k == mSelectedEngineNo){
//                    currentOfTheEngine = current;
//                    temperatureOfTheEngine = temperature;
//                }
            }
            sb.append("\n");
            LogMgr.v("次数 = "+get22EngineLoadCount+" 写入数据 = "+sb.toString());
            //更新页面
//            Message message_loadInfo = new Message();
//            message_loadInfo.what = MESSAGE_REFRESH_ENGINE_LOAD_INFO;
//            Bundle bundle = new Bundle();
//            bundle.putInt(MESSAG_KEY_COUNT, get22EngineLoadCount);
//            bundle.putInt(MESSAG_KEY_CURRENT, currentOfTheEngine);
//            bundle.putInt(MESSAG_KEY_TEMPERATURE, temperatureOfTheEngine);
//            message_loadInfo.setData(bundle);
//            mHandler.sendMessage(message_loadInfo);

            FileUtils.writeFile(outFile,sb.toString());
//            FileWriter fileWriter = new FileWriter(outFile, true);
//            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//            bufferedWriter.write(sb.toString());
//            bufferedWriter.close();
//            fileWriter.close();
        } catch (Exception e) {
            LogMgr.e("读写负载数据写入文件时异常");
            stopGet22EngineLoadTimer();
            e.printStackTrace();
        }
    }

}




