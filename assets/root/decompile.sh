#!/bin/bash

if [ $# -le 1 ]
then
  echo -e "usage:\tapk2damn <path to apk> <path to app installation root dir>"
  exit 1
fi

adb wait-for-device
adb root
adb wait-for-device

adb pull $1 app.apk

mkdir out
jadx --deobf -d out app.apk

adb push out $2

rm -rf out out.jar app.apk