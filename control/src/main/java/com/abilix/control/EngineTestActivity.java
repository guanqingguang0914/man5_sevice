package com.abilix.control;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.abilix.control.protocol.ProtocolUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;


import java.util.Timer;
import java.util.TimerTask;

public class EngineTestActivity extends Activity implements View.OnClickListener{

    private static final int MODE_UNKNOWN = 0;
    private static final int MODE_RUNNINGIN = 1;
    private static final int MODE_ANGLE_CHECK = 2;
    private static final int MODE_SET_ID = 3;

    public static final int STATE_HAS_ENGINE = 1;
    public static final int STATE_NO_ENGINE = 2;
    public static final int STATE_UNKNOWN = 3;

    private LinearLayout mLinearLayout1,mLinearLayout2,mLinearLayout3;

    private Button mButtonStartRunningIn,mButtonAngleTest,mButtonSetEngineID;

    private Spinner mSpinnerRunningInTime;
    private TextView mTextViewRestTime;

    private Button mButtonEngineLoop,mButtonForward90,mButtonBackward90,mButtonGoZero;

    private Spinner getmSpinnerEngineIDCurrent;
    private Spinner mSpinnerEngineIDToSet;

    private int mCountDownPeroid = 1;
    private int mCurrentngineNo = 1;
    private int mEngineIDToSet = 1;



    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    mTextViewRestTime.setText(String.valueOf(msg.arg1));
                    break;
                case 1:
                    Toast.makeText(EngineTestActivity.this, (String)msg.obj, Toast.LENGTH_LONG).show();
                    break;
            }

        }
    };

    private int mMode = MODE_RUNNINGIN;
    private Timer mTimerDetect;
    private TimerTask mTimerTaskDetect;
    private int mHasEngine = STATE_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_enginetest);

        findViews();
        startTimerDetectionEngine();
    }

    private void startTimerDetectionEngine() {
        stopTimerDetectionEngine();
        mTimerDetect = new Timer();
        mTimerTaskDetect = new TimerTask() {
            @Override
            public void run() {
                int hasEngine = ProtocolUtils.hasEngine();
                if(hasEngine == STATE_UNKNOWN){
                    LogMgr.e("检测有无舵机失败");
                }else if(hasEngine == STATE_HAS_ENGINE){
                    LogMgr.i("当前有舵机");
                    if(mHasEngine == STATE_NO_ENGINE){
                        if(mMode == MODE_RUNNINGIN){
                            LogMgr.i("自动开始磨合");
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startRunningIn();
                                }
                            },1000);
                        }else if(mMode == MODE_ANGLE_CHECK){
                            LogMgr.i("自动开始标零");
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ProtocolUtils.adjustZeroS(1);
                                }
                            },1000);
                        }else if(mMode == MODE_SET_ID){
                            LogMgr.i("自动开始设定ID");
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setEngineAndCheck(mCurrentngineNo,mEngineIDToSet);
                                }
                            },1000);
                        }
                    }
                    mHasEngine = hasEngine;
                }else if(hasEngine == STATE_NO_ENGINE){
                    LogMgr.i("当前无舵机");
                    if(mHasEngine == STATE_HAS_ENGINE){
                        if(mMode == MODE_RUNNINGIN){
                            stopTimerRunningIn();
                        }else if(mMode == MODE_ANGLE_CHECK){
                            stopLoopTimer();
                        }else if(mMode == MODE_SET_ID){

                        }
                    }
                    mHasEngine = hasEngine;
                }
            }
        };
        mTimerDetect.schedule(mTimerTaskDetect,2000,2000);
    }
    private void stopTimerDetectionEngine() {
        if(mTimerDetect != null){
            mTimerDetect.cancel();
        }
        if(mTimerTaskDetect != null){
            mTimerTaskDetect.cancel();
        }
    }

    private void findViews(){
        mLinearLayout1 = (LinearLayout)findViewById(R.id.linearlayout13);
        mLinearLayout2 = (LinearLayout)findViewById(R.id.linearlayout14);
        mLinearLayout3 = (LinearLayout)findViewById(R.id.linearlayout15);

        mTextViewRestTime = (TextView)findViewById(R.id.textViewCountdownTime);

        mSpinnerRunningInTime = (Spinner)findViewById(R.id.spinner_runningin_time);
        getmSpinnerEngineIDCurrent = (Spinner)findViewById(R.id.spinner_cunnent_id);
        mSpinnerEngineIDToSet = (Spinner)findViewById(R.id.spinner_set_engineNo2);

        mButtonStartRunningIn = (Button)findViewById(R.id.button_beginToRunningin);
        mButtonAngleTest = (Button)findViewById(R.id.button_angle_test);
        mButtonSetEngineID = (Button)findViewById(R.id.button_set_engine_id_S_2);

        mButtonEngineLoop = (Button)findViewById(R.id.button_loop);
        mButtonForward90 = (Button)findViewById(R.id.button_forward_90);
        mButtonBackward90 = (Button)findViewById(R.id.button_backward_90);
        mButtonGoZero = (Button)findViewById(R.id.button_zero);

        mButtonStartRunningIn.setOnClickListener(this);
        mButtonAngleTest.setOnClickListener(this);
        mButtonSetEngineID.setOnClickListener(this);
        mButtonEngineLoop.setOnClickListener(this);
        mButtonForward90.setOnClickListener(this);
        mButtonBackward90.setOnClickListener(this);
        mButtonGoZero.setOnClickListener(this);

        mSpinnerRunningInTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCountDownPeroid = position + 1;
                LogMgr.d("当前周期为 = " + mCountDownPeroid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCountDownPeroid = 1;
                LogMgr.d("默认设定周期1");
            }
        });

        getmSpinnerEngineIDCurrent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentngineNo = position + 1;
                LogMgr.d("当前舵机ID为 = " + mCurrentngineNo);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentngineNo = 1;
                LogMgr.d("默认设定当前舵机ID1");
            }
        });

        mSpinnerEngineIDToSet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mEngineIDToSet = position + 1;
                LogMgr.d("目标舵机ID为 = " + mEngineIDToSet);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mEngineIDToSet = 1;
                LogMgr.d("默认设定目标舵机ID1");
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_beginToRunningin:
//                startRunningIn();
                mLinearLayout1.setVisibility(View.VISIBLE);
                mLinearLayout2.setVisibility(View.GONE);
                mLinearLayout3.setVisibility(View.GONE);
                mMode = MODE_RUNNINGIN;
                break;
            case R.id.button_angle_test:
//                ProtocolUtils.adjustZeroS(1);
                mLinearLayout1.setVisibility(View.GONE);
                mLinearLayout2.setVisibility(View.VISIBLE);
                mLinearLayout3.setVisibility(View.GONE);
                mMode = MODE_ANGLE_CHECK;
                break;
            case R.id.button_set_engine_id_S_2:
//                setEngineAndCheck(mCurrentngineNo,mEngineIDToSet);
                mLinearLayout1.setVisibility(View.GONE);
                mLinearLayout2.setVisibility(View.GONE);
                mLinearLayout3.setVisibility(View.VISIBLE);
                mMode = MODE_SET_ID;
                break;

            case R.id.button_loop:
                startLoop();
                break;
            case R.id.button_forward_90:
                stopLoopTimer();
                moveForwardS(1);
                break;
            case R.id.button_zero:
                stopLoopTimer();
                goToZeroS(1);
                break;
            case R.id.button_backward_90:
                stopLoopTimer();
                moveBackS(1);
                break;
            default:
                break;
        }
    }

    private int totalStep;
    private int currentStep;
    private int totalTime;
    private int restTime;
    private int totalRestTime;
    private byte[] cmdForward,cmdBackward,cmdStop,cmdOpenWheel,cmdCloseWheel;
    /**单次磨合时间*/
    private int singleRunninginTime = 5;
//    private int singleRestTime = 2;
    /**
     * 开始磨合
     */
    private void startRunningIn() {
        totalStep = mCountDownPeroid*2;
        currentStep = 0;
        totalTime = mCountDownPeroid*singleRunninginTime*2;
        totalRestTime = totalTime;
        restTime = 0;
        cmdOpenWheel = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_OPEN_WHEEL_MODE,new byte[]{(byte)0x02,(byte)0x01,(byte)0x01});
        cmdCloseWheel = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_OPEN_WHEEL_MODE,new byte[]{(byte)0x02,(byte)0x01,(byte)0x00});
        cmdForward = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_SET_WHEEL_MODE,new byte[]{(byte)0x03,(byte)0x01,(byte)0x01,(byte)0x03,(byte)0x00});
        cmdBackward = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_SET_WHEEL_MODE,new byte[]{(byte)0x03,(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x00});
        cmdStop = ProtocolUtils.buildProtocol((byte)ControlInitiator.ROBOT_TYPE_S,GlobalConfig.ENGINE_TEST_S_OUT_CMD_1,
                GlobalConfig.ENGINE_TEST_S_OUT_CMD_2_SET_WHEEL_MODE,new byte[]{(byte)0x03,(byte)0x01,(byte)0x00,(byte)0x03,(byte)0x00});
        startTimerRunningIn();
    }

    private Timer mTimerRunningIn;
    private TimerTask mTimerTaskRunningIn;
    private void startTimerRunningIn() {
        SP.write(cmdOpenWheel);
        stopTimerRunningIn();
        mTimerRunningIn = new Timer();
        mTimerTaskRunningIn = new TimerTask() {
            @Override
            public void run() {
                LogMgr.v("totalStep = "+totalStep +" currentStep = "+currentStep+" totalTime = "+totalTime+"  restTime = "+restTime+" totalRestTime = "+totalRestTime);
                mHandler.obtainMessage(0,totalRestTime,0).sendToTarget();
                totalRestTime--;
                if(restTime == 0){
                    currentStep++;
                    if(currentStep > totalStep){
                        LogMgr.i("磨合结束");
                        stopTimerRunningIn();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        SP.write(cmdCloseWheel);
                        return;
                    }
                    if(currentStep%2 == 1){
                        LogMgr.i("下发正转命令");
                        SP.write(cmdForward);
                        restTime = singleRunninginTime - 1;
                    }else if(currentStep%2 == 0){
                        LogMgr.i("下发反转命令 = "+ Utils.bytesToString(cmdBackward));
                        SP.write(cmdBackward);
                        restTime = singleRunninginTime - 1;
                    }
//                    else{
//                        LogMgr.i("下发停止命令");
//                        SP.write(cmdStop);
//                        restTime = 9;
//                    }
                }else{
                    restTime--;
                }
            }
        };
        mTimerRunningIn.schedule(mTimerTaskRunningIn,2000,1*1000);
    }
    private void stopTimerRunningIn(){
        if(cmdStop != null){
            SP.write(cmdStop);
        }
        if(mTimerRunningIn != null){
            mTimerRunningIn.cancel();
        }
        if(mTimerTaskRunningIn != null){
            mTimerTaskRunningIn.cancel();
        }
    }

    /**
     * 开始循环
     */
    private void startLoop() {
        startLoopTimer();
    }

    private Timer mTimerLoop;
    private TimerTask mTimerTaskLoop;
    private int loopCount;
    private void startLoopTimer(){
        stopLoopTimer();
        loopCount = 0;
        mTimerLoop = new Timer();
        mTimerTaskLoop = new TimerTask() {
            @Override
            public void run() {
                if(loopCount%4 == 0|| loopCount%4 == 2){
                    goToZeroS(1);
                }else if(loopCount%4 == 1){
                    moveForwardS(1);
                }else if(loopCount%4 == 3){
                    moveBackS(1);
                }
                loopCount++;
            }
        };
        mTimerLoop.schedule(mTimerTaskLoop,200,2000);
    }
    private void stopLoopTimer(){
        if(mTimerLoop != null){
            mTimerLoop.cancel();
        }
        if(mTimerTaskLoop != null){
            mTimerTaskLoop.cancel();
        }
    }

    /**
     * 设定舵机ID并检测
     * @param mCurrentngineNo
     * @param mEngineIDToSet
     */
    private void setEngineAndCheck(int mCurrentngineNo, int mEngineIDToSet) {
        LogMgr.d("setEngineAndCheck()");
        ProtocolUtils.setEngineID(mCurrentngineNo,mEngineIDToSet);
        boolean result = false;
        for(int i = 0 ; i<3; i++){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int realAngle = ProtocolUtils.getEngineAngle(mEngineIDToSet);
            LogMgr.d("setEngineAndCheck realAngle = " + realAngle);
            if(realAngle <= 0 || realAngle > 1000){
                LogMgr.e("获取当前角度失败 校准失败");
            }else{
                result = true;
                break;
            }
        }
        if(result){
            mHandler.obtainMessage(1,"设定舵机ID为"+mEngineIDToSet+"成功");
//            Toast.makeText(EngineTestActivity.this, "设定舵机ID为"+mEngineIDToSet+"成功", Toast.LENGTH_LONG).show();
        }else{
            mHandler.obtainMessage(1,"设定舵机ID为"+mEngineIDToSet+"失败");
//            Toast.makeText(EngineTestActivity.this, "设定舵机ID为"+mEngineIDToSet+"失败", Toast.LENGTH_LONG).show();
        }
    }

    private void moveForwardS(int engineID){
        ProtocolUtils.goToAngleS(819, engineID);
    }

    private void moveBackS(int engineID){
        ProtocolUtils.goToAngleS(204, engineID);
    }

    private void goToZeroS(int engineID){
        ProtocolUtils.goToAngleS(512, engineID);
    }
}