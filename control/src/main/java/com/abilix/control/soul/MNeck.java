package com.abilix.control.soul;

import com.abilix.control.utils.LogMgr;
public class MNeck {
    private SoulHelper mSoulHelper;
    private NeckSwingThread mNeckSwingThread;
    private NeckNodThread mNeckNodThread;
    private boolean isResetNeck = false;

    public MNeck() {
        mSoulHelper = new SoulHelper();
    }

    public void swingHead() {
        mNeckSwingThread = new NeckSwingThread();
        mNeckSwingThread.start();
    }

    public void nodHead() {
        mNeckNodThread = new NeckNodThread();
        mNeckNodThread.start();
    }

    public void resetNeck() {
        LogMgr.d("reset neck");
        isResetNeck = true;
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LogMgr.d("reset neck up to 0");
                mSoulHelper.SetNeckUPMotor(0);
                mSoulHelper.SetNeckLRMotor(0);
            }
        }.start();
    }

    private class NeckSwingThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                isResetNeck = false;
                if (!isResetNeck) {
                    mSoulHelper.SetNeckLRMotor(-130);
                }
                Thread.sleep(3000);
                if (!isResetNeck) {
                    mSoulHelper.SetNeckLRMotor(130);
                }
                Thread.sleep(3000);
                mSoulHelper.SetNeckLRMotor(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class NeckNodThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                isResetNeck = false;
                if (!isResetNeck) {
                    mSoulHelper.SetNeckUPMotor(30);
                }
                Thread.sleep(3000);
                if (!isResetNeck) {
                    mSoulHelper.SetNeckUPMotor(-15);
                }
                Thread.sleep(3000);
                mSoulHelper.SetNeckUPMotor(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
