# General ADB Debugging Commands

Replace placeholders such as `PACKAGE`, `ACTIVITY`, `SERIAL`, and `FILE` with real values. Use `adb -s SERIAL ...` whenever more than one device is connected.

## Device discovery and connection

```bash
adb version
adb devices -l
adb get-state
adb -s SERIAL get-state
adb reconnect
adb kill-server
adb start-server
```

Wireless debugging on supported Android versions:

```bash
adb pair HOST:PAIRING_PORT
adb connect HOST:ADB_PORT
adb disconnect HOST:ADB_PORT
```

## Device and application identity

```bash
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell pm list packages
adb shell pm list packages | grep PACKAGE
adb shell dumpsys package PACKAGE
adb shell pm path PACKAGE
```

## Launching and stopping an application

```bash
adb shell monkey -p PACKAGE -c android.intent.category.LAUNCHER 1
adb shell am start -n PACKAGE/ACTIVITY
adb shell am force-stop PACKAGE
adb shell pidof PACKAGE
```

Launch an activity while reporting startup timing:

```bash
adb shell am start -W -n PACKAGE/ACTIVITY
```

## Logcat

Clear old logs and begin a readable stream:

```bash
adb logcat -c
adb logcat -v threadtime
```

Filter by application process:

```bash
adb logcat --pid=$(adb shell pidof PACKAGE)
```

Common focused views:

```bash
adb logcat '*:E'
adb logcat AndroidRuntime:E ActivityManager:I '*:S'
adb logcat | grep -i PACKAGE
adb logcat -d -v threadtime > logcat.txt
```

## Activity, window, and process state

```bash
adb shell dumpsys activity activities
adb shell dumpsys activity top
adb shell dumpsys window windows
adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
adb shell ps -A
adb shell top -o PID,CPU,RES,NAME
adb shell dumpsys meminfo PACKAGE
adb shell dumpsys cpuinfo | grep PACKAGE
```

## Network diagnostics

```bash
adb shell dumpsys connectivity
adb shell dumpsys wifi
adb shell ip addr
adb shell ip route
adb shell ping -c 4 HOST
adb shell settings get global http_proxy
```

Forward or reverse local ports:

```bash
adb forward tcp:LOCAL_PORT tcp:DEVICE_PORT
adb forward --list
adb reverse tcp:DEVICE_PORT tcp:HOST_PORT
adb reverse --list
adb forward --remove tcp:LOCAL_PORT
adb reverse --remove tcp:DEVICE_PORT
```

## Files and application storage

```bash
adb shell ls -la /sdcard/
adb push FILE /sdcard/Download/
adb pull /sdcard/Download/FILE .
adb shell run-as PACKAGE pwd
adb shell run-as PACKAGE ls -la files
adb shell run-as PACKAGE ls -la databases
```

`run-as` works only for debuggable application builds.

## Screenshots and screen recording

```bash
adb exec-out screencap -p > screenshot.png
adb shell screenrecord /sdcard/recording.mp4
adb pull /sdcard/recording.mp4 .
```

Stop `screenrecord` with `Ctrl+C`.

## Input and UI inspection

```bash
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_BACK
adb shell input tap X Y
adb shell input swipe X1 Y1 X2 Y2 DURATION_MS
adb shell input text 'TEXT'
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml .
```

## Installation and permissions

```bash
adb install PATH_TO_APK
adb install -r PATH_TO_APK
adb shell pm list permissions -g
adb shell pm grant PACKAGE PERMISSION
adb shell pm revoke PACKAGE PERMISSION
adb shell dumpsys package PACKAGE | grep permission
```

Uninstalling or clearing data is destructive and removes local application data:

```bash
adb uninstall PACKAGE
adb shell pm clear PACKAGE
```

## System reports

```bash
adb bugreport bugreport.zip
adb shell dumpsys > dumpsys.txt
adb shell dumpsys battery
adb shell dumpsys jobscheduler
adb shell dumpsys alarm
adb shell dumpsys notification
```

## Useful multi-device pattern

```bash
adb devices -l
adb -s SERIAL shell pidof PACKAGE
adb -s SERIAL logcat --pid=$(adb -s SERIAL shell pidof PACKAGE)
```
