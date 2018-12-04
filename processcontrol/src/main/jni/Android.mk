LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

TARGET_PLATFORM := android-14
LOCAL_MODULE    := process_control
LOCAL_SRC_FILES := ProcessControl.c
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)