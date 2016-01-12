#!/bin/bash
/home/baer/android-ndk-r10e/ndk-build clean
/home/baer/android-ndk-r10e/ndk-build

adb wait-for-device
adb root
adb wait-for-device
adb remount
adb wait-for-device

adb shell 'rm -f /system/lib/libusb-tethering.so'
adb shell 'rm -f /system/lib/libcivetweb.so'
adb shell 'rm -f /system/lib/libdamn-server.so'
adb shell 'rm -f /system/lib/libdaemonize.so'

adb push ./obj/local/armeabi/libusb-tethering.so /system/lib
adb push ./obj/local/armeabi/libcivetweb.so /system/lib
adb push ./obj/local/armeabi/libdamn-server.so /system/lib
adb push ./obj/local/armeabi/libdaemonize.so /system/lib

adb push ./obj/local/armeabi/damn-server-exec /system/bin
