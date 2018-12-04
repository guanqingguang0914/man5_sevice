package com.abilix.control.pad;

import android.os.Handler;
import android.os.Message;

public interface IProtocolDisposer {
    void DisposeProtocol(Message msg);

    void stopDisposeProtocol();
}
