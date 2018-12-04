package com.abilix.control.scratch;

public interface IScratchExecutor {
    void execute(byte[] data);

    //void execute(byte[] data);
    void clearState();
}
