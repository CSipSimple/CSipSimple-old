
LOCAL_PATH := $(call my-dir)/../


############
# PJSIPJNI #
############
include $(CLEAR_VARS)
LOCAL_MODULE := pjsipjni

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pjsip/include $(LOCAL_PATH)/../pjlib-util/include/ \
			$(LOCAL_PATH)/../pjlib/include/ $(LOCAL_PATH)/../pjmedia/include \
			$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/include/ \
			$(LOCAL_PATH)/../third_party/zrtp4pj/zsrtp/include/
			
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

JNI_SRC_DIR := src/

LOCAL_SRC_FILES := $(JNI_SRC_DIR)/pjsua_wrap.cpp $(JNI_SRC_DIR)/pjsua_jni_addons.c $(JNI_SRC_DIR)/zrtp_android.c


LOCAL_LDLIBS := -llog

	
ifeq ($(MY_ANDROID_DEV),1)
LOCAL_SRC_FILES += $(JNI_SRC_DIR)/android_jni_dev.cpp
endif
ifeq ($(MY_ANDROID_DEV),2)
LOCAL_SRC_FILES += $(JNI_SRC_DIR)/opensl_dev.cpp
endif

ifeq ($(MY_USE_VIDEO),1)
LOCAL_SRC_FILES += $(JNI_SRC_DIR)/opengl_video_dev.c $(JNI_SRC_DIR)/android_capture_dev.c  
LOCAL_LDLIBS += -lGLESv1_CM
endif


ifeq ($(MY_USE_TLS),1)
LOCAL_LDLIBS += -ldl 
endif

ifeq ($(MY_ANDROID_DEV),2)
LOCAL_LDLIBS += -lOpenSLES 
endif


#LOCAL_LDFLAGS := -Wl,-Map=moblox.map,--cref,--gc-section 

LOCAL_STATIC_LIBRARIES := pjsip pjmedia pjnath pjlib-util pjlib resample srtp 
ifeq ($(MY_USE_ILBC),1)
	LOCAL_STATIC_LIBRARIES += ilbc
endif
ifeq ($(MY_USE_GSM),1)
	LOCAL_STATIC_LIBRARIES += gsm
endif
ifeq ($(MY_USE_SPEEX),1)
	LOCAL_STATIC_LIBRARIES += speex
endif
ifeq ($(MY_USE_G729),1)
	LOCAL_STATIC_LIBRARIES += g729
endif
ifeq ($(MY_USE_SILK),1)
	LOCAL_STATIC_LIBRARIES += silk
endif
ifeq ($(MY_USE_CODEC2),1)
	LOCAL_STATIC_LIBRARIES += codec2
endif
ifeq ($(MY_USE_TLS),1)
	LOCAL_STATIC_LIBRARIES += ssl zrtp4pj crypto 
endif
ifeq ($(MY_USE_VIDEO),1)
	LOCAL_LDLIBS += -L$(LOCAL_PATH)/../../../ffmpeg-android/build/ffmpeg/armeabi-v7a/lib/ -lavcodec -lavcore -lavformat -lavutil -lswscale
endif


include $(BUILD_SHARED_LIBRARY)

