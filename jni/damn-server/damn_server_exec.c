
#ifndef _WS_SERVER_EXEC_H
#define _WS_SERVER_EXEC_H

#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <pthread.h>
#include <android/log.h>
#include "damn_server.h"
#include "daemonize.h"

#define TAG "damn_server_exec"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


void* start(void *cmd)
{
	printf("command: %s\n", (char*)cmd);
	system(cmd);
	printf("end command\n");
}

int main(int argc, const char* argv[])
{
    LOGI("My process ID : %d\n", getpid());
    LOGI("My parent's ID: %d\n", getppid());
    LOGI("path %s\n", argv[0]);


	//check if we got 'docroot' as parameter passed
	if(argc!=4) {
		printf("usage ws-server-exec path/to/docroot path/to/pemfile /path/to/tmp\n");
		LOGE("wrong amounts of parameters passed [%d]", argc-1);
		return -1;
	}

	//daemonize
//	skeleton_daemon();

	uid_t uid=getuid(), euid=geteuid();
	LOGI("uid=%d, euid=%d\n", uid, euid);

//	if (uid!=0 || uid!=euid) {
//		//restart with root privileges
//		char cmd[256];
//		LOGI("params: %s %s %s", argv[0], argv[1], argv[2]);
//		sprintf(cmd, "su -c '%s %s %s' &", argv[0], argv[1], argv[2]);
//
//		pthread_t id;
//		if (pthread_create(&id, NULL, &start, (void *)&cmd))
//		{
//			LOGE("pthread_create failed :(");
//			return -2;
//		}
//
//		pthread_join(id, NULL);
//		LOGD("join of server finished");
//		return 0;
//	}

	LOGI("docroot: %s, pemfile: %s, tmp: %s", argv[1], argv[2], argv[3]);

	//start ws_server
	LOGD("start server");
	start_server(argv[1], argv[2], argv[3]);

	LOGD("exiting damn server");
	pthread_exit(NULL);
}

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* _WS_SERVER_EXEC_H */
