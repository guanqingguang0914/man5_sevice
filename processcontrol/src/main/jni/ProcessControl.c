#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>
#include <unistd.h>
#include <sys/prctl.h>

#include "android/log.h"
static const char *TAG="ProcessControl";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

// This prctl is only available in Android kernels.
#define PR_SET_TIMERSLACK_PID 41

#define TIMERSLACK 50000

JNIEXPORT void JNICALL Java_com_abilix_control_process_ProcessControl_timerslack
  (JNIEnv *env, jobject thiz,jint tid,jint period)
{
    LOGE("=================================================================================================================================>exec ProcessControl");
    prctl(period,TIMERSLACK, tid);
    LOGE("=================================================================================================================================>exec ProcessControl");
}