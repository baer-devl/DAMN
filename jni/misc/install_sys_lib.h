
#ifndef _INSTALL_SYS_LIB_H
#define _INSTALL_SYS_LIB_H

#include <stdlib.h>
#include <sys/mount.h>
#include <unistd.h>
#include <stdio.h>
#include <jni.h>
#include <stdbool.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
 * Install passed libraries into system library path.
 */
bool install_sys_lib(const char *path_to_lib, const char **libs, int size);

/**
 * Same as install_sys_libs but for jni interface.
 */
bool Java_at_fhooe_mcm14_damn_jni_InstallSysLib_installSysLibJNI(JNIEnv *env, jclass cls, jstring path_to_lib, jobjectArray libs, int size);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* _INSTALL_SYS_LIB_H */
