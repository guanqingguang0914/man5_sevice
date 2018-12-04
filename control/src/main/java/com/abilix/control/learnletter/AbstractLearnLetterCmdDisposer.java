package com.abilix.control.learnletter;

import android.os.Handler;

import com.abilix.control.patch.PlayMoveOrSoundUtils;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;

public abstract class AbstractLearnLetterCmdDisposer implements ILearnLetterCmdDisposer {

    protected static final int MODE_STATE_STOP = 0;
    protected static final int MODE_STATE_PLAY = 1;
    protected static final int MODE_STATE_PAUSE = 2;
    protected static final int MODE_STATE_RESUME = 3;
    protected static final int MODE_STATE_LOOP = 4;
    protected static final int MODE_STATE_PLAY_AUTO_BALANCE = 5;


    protected static final int MODE_STATE_EXPLAIN_STOP = 0x10;
    protected static final int MODE_STATE_EXPLAIN_PLAY = 0x11;
    protected static final int MODE_STATE_EXPLAIN_PAUSE = 0x12;
    protected static final int MODE_STATE_EXPLAIN_RESUME = 0x13;

    protected Handler mHandler;

    public AbstractLearnLetterCmdDisposer(Handler mHandler) {
        this.mHandler = mHandler;
    }

    protected PlayMoveOrSoundUtils.PlayCallBack zhaohui_callback=new PlayMoveOrSoundUtils.PlayCallBack() {
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
            LogMgr.i("SP.destroySP()");
            SP.destroySP();
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
    };

}
