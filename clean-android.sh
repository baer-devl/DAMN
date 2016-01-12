#!/bin/bash
adb wait-for-device
adb root
adb wait-for-device
adb remount
adb wait-for-device

adb shell 'rm /system/lib/libusb-tethering.so'
adb shell 'rm /system/lib/libcivetweb.so'
adb shell 'rm /system/lib/libdamn-server.so'
adb shell 'rm /system/lib/libdaemonize.so'

adb shell 'rm /data/local/tmp/damn-server-exec'
