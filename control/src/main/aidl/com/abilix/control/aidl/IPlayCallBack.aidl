package com.abilix.control.aidl;
interface IPlayCallBack {
    void onStart();
    void onPause();
    void onResume();
    void onStop();
    void onSingleMoveStopWhileLoop();
    void onNormalStop();
    void onForceStop();
}
