
#ifndef _DAMN_SERVER_H
#define _DAMN_SERVER_H

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>
#include <jni.h>
#include <stdbool.h>
#include <android/log.h>
#include "civetweb.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
 * Start server and block till server terminates.
 */
void start_server(const char *docroot, const char *pemfile, const char *cachedir);
/**
 * Stop the server and cause 'start_server' to return.
 * Return true if stopping the server was successful, false otherwise.
 */
bool stop_server(const char *cachedir);
/**
 * Return the connection count od the server.
 */
int get_connection_count();

void Java_at_fhooe_mcm14_damn_jni_DAMNServer_startServerJNI(JNIEnv *env, jclass clazz, jstring docroot, jstring pemfile, jstring cachedir);
void Java_at_fhooe_mcm14_damn_jni_DAMNServer2_startServerJNI(JNIEnv *env, jclass clazz, jstring docroot, jstring pemfile, jstring cachedir);
bool Java_at_fhooe_mcm14_damn_jni_DAMNServer_stopServerJNI();//JNIEnv *env, jclass clazz);
int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getConnectionsJNI();
int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getStateBlockingJNI();
int Java_at_fhooe_mcm14_damn_jni_DAMNServer_getStateJNI();
void Java_at_fhooe_mcm14_damn_jni_DAMNServer_pushJNI(JNIEnv *env, jclass clazz, int code, jstring message, jstring application, long long threadId);
jstring Java_at_fhooe_mcm14_damn_jni_DAMNServer_receiveBlockingJNI(JNIEnv *env, jclass clazz, jstring application, long long threadId);
void Java_at_fhooe_mcm14_damn_jni_DAMNServer_newAppJNI(JNIEnv *env, jclass clazz, jstring application, long long threadId);
void Java_at_fhooe_mcm14_damn_jni_DAMNServer_deleteAppJNI(JNIEnv *env, jclass clazz, jstring application, long long threadId);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* _DAMN_SERVER_H */
