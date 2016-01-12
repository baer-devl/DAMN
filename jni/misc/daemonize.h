
#ifndef _DAEMONIZE_H
#define _DAEMONIZE_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <syslog.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

void skeleton_daemon();

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* _DAEMONIZE_H */
