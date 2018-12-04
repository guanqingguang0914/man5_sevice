package com.abilix.control.aidl;
interface IPushListener{
    oneway void onPush(in byte[] data);
}