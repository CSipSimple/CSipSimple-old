
##########
# FFMPEG #
##########

LOCAL_PATH := $(call my-dir)/../../ffmpeg

include $(CLEAR_VARS)
LOCAL_MODULE    := ffmpeg

LOCAL_C_INCLUDES += $(LOCAL_PATH)/

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DHAVE_AV_CONFIG_H -Wno-sign-compare -Wno-switch -Wno-pointer-sign -DTARGET_CONFIG=\"config-$(TARGET_ARCH).h\"
PJLIB_SRC_DIR := 

# libavcodec
LOCAL_SRC_FILES := libavcodec/allcodecs.c \
       libavcodec/audioconvert.c \
       libavcodec/avpacket.c \
       libavcodec/bitstream.c \
       libavcodec/bitstream_filter.c \
       libavcodec/dsputil.c \
       libavcodec/eval.c \
       libavcodec/faanidct.c \
       libavcodec/imgconvert.c \
       libavcodec/jrevdct.c \
       libavcodec/opt.c \
       libavcodec/options.c \
       libavcodec/parser.c \
       libavcodec/raw.c \
       libavcodec/resample.c \
       libavcodec/resample2.c \
       libavcodec/simple_idct.c \
       libavcodec/utils.c

#commons
LOCAL_SRC_FILES += libavcodec/mpegvideo_enc.c libavcodec/error_resilience.c libavcodec/mpegvideo.c \
	libavcodec/motion_est.c libavcodec/ratecontrol.c \
	libavcodec/mpeg12data.c

#h261
LOCAL_SRC_FILES += libavcodec/h261dec.c libavcodec/h261.c libavcodec/h261enc.c  

#h263
LOCAL_SRC_FILES += libavcodec/h263dec.c libavcodec/h263.c libavcodec/h263_parser.c 

include $(BUILD_STATIC_LIBRARY)
