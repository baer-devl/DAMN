
#ifndef _USB_TETHERING_H
#define _USB_TETHERING_H

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <assert.h>
#include <pthread.h>
#include <jni.h>
#include <stdbool.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
 * Execute a bunch of system commands and if a client is connected, return it's ip address, null otherwise.
 */
char* start_tethering();
/**
 * Stop tethering and set everything back.
 */
int stop_tethering();
/**
 * Check if tethering is activated.
 */
bool is_tethering_active();

/**
 * Same as start_tethering but for jni interface.
 */
jstring Java_at_fhooe_mcm14_damn_jni_USBTethering_startUSBTetheringJNI(JNIEnv* env);
/**
 * Same as stop_tethering but for jni interface.
 */
bool Java_at_fhooe_mcm14_damn_jni_USBTethering_stopUSBTetheringJNI();
/**
 * Same as tethering_active but for jni interface.
 */
bool Java_at_fhooe_mcm14_damn_jni_USBTethering_checkUSBTetheringJNI();
/**
 * Block until status changed and return state value as integer.
 */
int Java_at_fhooe_mcm14_damn_jni_USBTethering_getStateBlockingJNI();
/**
 * Return state as integer.
 */
int Java_at_fhooe_mcm14_damn_jni_USBTethering_getStateJNI();

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* _USB_TETHERING_H */
