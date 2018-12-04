#include <jni.h>
#include "zmodem/system/zmodem.h"

JNIEXPORT jint JNICALL
Java_com_abilix_zmodem_ZmodemUtils_sendByZmodemWithFD(JNIEnv *env, jclass type, jint fd, jstring path_) {
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);

    // TODO

    //(*env)->ReleaseStringUTFChars(env, path_, path);

    return zms_main_with_fd(fd, path);
}

JNIEXPORT jint JNICALL
Java_com_abilix_zmodem_ZmodemUtils_sendByZmodem(JNIEnv *env, jclass type, jstring path_) {
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);

    // TODO

    //(*env)->ReleaseStringUTFChars(env, path_, path);

    return zms_main(path);
}