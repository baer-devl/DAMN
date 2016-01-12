LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= usb-tethering
LOCAL_CFLAGS    		:= -pthread -lz
LOCAL_SRC_FILES 		:= usb-tethering/usb_tethering.c
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/usb-tethering
LOCAL_LDLIBS 			:= -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= daemonize
LOCAL_SRC_FILES 		:= misc/daemonize.c
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/misc
LOCAL_LDLIBS 			:= -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= install-sys-lib
LOCAL_SRC_FILES 		:= misc/install_sys_lib.c
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/misc
LOCAL_LDLIBS 			:= -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    		:= civetweb
LOCAL_C_INCLUDES		:= civetweb
LOCAL_CFLAGS    		:= -std=c99 -O2 -W -Wall -pthread -DUSE_WEBSOCKET
LOCAL_SRC_FILES 		:= civetweb/civetweb.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_CFLAGS			:= -std=c99 -O2 -W -Wall -pthread -ldl
LOCAL_MODULE			:= damn-server
LOCAL_C_INCLUDES 		:= $(LOCAL_PATH)/damn-server \
						   $(LOCAL_PATH)/civetweb
LOCAL_SHARED_LIBRARIES	:= civetweb
LOCAL_SRC_FILES			:= damn-server/damn_server.c
LOCAL_LDLIBS 			:= -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE			:= damn-server-exec
LOCAL_C_INCLUDES 		:= $(LOCAL_PATH)/damn-server \
						   $(LOCAL_PATH)/civetweb \
						   $(LOCAL_PATH)/misc
LOCAL_SHARED_LIBRARIES	:= damn-server \
						   daemonize
LOCAL_SRC_FILES			:= damn-server/damn_server_exec.c
LOCAL_LDLIBS 			:= -llog
include $(BUILD_EXECUTABLE)