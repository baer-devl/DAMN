#include "install_sys_lib.h"

#define TAG "install_sys_lib"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)

int remount_system()
{
	//execute remount
	return system("su -c 'mount -o rw,remount /system'");
}


bool install_sys_lib(const char *path_to_lib, const char **libs, int size)
{
	//remount '/system'
	if(remount_system()) {
		LOGE("remounting '/system' failed");
		return false;
	}

	//copy library into system library path
	char *buf;
	size_t sz;
	int n=0;

	for(n=0;n<size;n++) {
		sz = snprintf(NULL, 0, "su -c 'cp %s/%s /system/lib'", path_to_lib, libs[n]);
		buf = (char *)malloc(sz + 1);
		snprintf(buf, sz+1, "su -c 'cp %s/%s /system/lib'", path_to_lib, libs[n]);

		if(system(buf)) {
			LOGE("copy library into '/system/lib' failed");
			return false;
		}

		//change access permissions
		sz = snprintf(NULL, 0, "su -c 'chmod 644 /system/lib/%s'", libs[n]);
		buf = (char *)malloc(sz + 1);
		snprintf(buf, sz+1, "su -c 'chmod 644 /system/lib/%s'", libs[n]);

		if(system(buf)) {
			LOGE("changing access permissions failed");
			return false;
		}
	}

	return true;
}

bool Java_at_fhooe_mcm14_damn_jni_InstallSysLib_installSysLibJNI(JNIEnv *env, jclass cls, jstring path_to_lib, jobjectArray libs, jint size)
{
	int n;
	const char *libraries[size];

	for (n=0;n<size;n++) {
		jstring string = (jstring) (*env)->GetObjectArrayElement(env, libs, n);
		libraries[n] =  (*env)->GetStringUTFChars(env, string, 0);
		LOGD("lib: %s", libraries[n]);
	}

	return install_sys_lib((*env)->GetStringUTFChars(env, path_to_lib, 0), libraries, size);
}
