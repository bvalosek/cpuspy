# makefile for use in developing Android applications 
# last modified: march 10 2011
# (c) Brandon Valosek 2011

# app info
APPNAME=cpuspy
NAMESPACE=com.bvalosek.cpuspy
START_ACTIVITY=ui.HomeActivity

# keystore info, only needed if signing a release version of the app
KEYSTORE=../keystore/my-release-key.keystore
KEY_ALIAS=my-keystore

# default target is to install the debug APK on whatever device/emu
# we currently have
all : build-debug install-debug

# clean bin and gen
clean :
	ant clean

# build APK and sign with debug key
build-debug :
	ant -q -e debug

# push the debug APK to the emulator or phone and run it
install-debug :
	adb install -rt bin/$(APPNAME)-debug.apk
	adb shell am start -a android.intent.action.MAIN \
		-n $(NAMESPACE)/$(NAMESPACE).$(START_ACTIVITY)

# same as install-debug but will use a device even if an emulator is running
install-debug-device:
	adb -d install -rt bin/$(APPNAME)-debug.apk
	adb -d shell am start -a android.intent.action.MAIN \
		-n $(NAMESPACE)/$(NAMESPACE).$(START_ACTIVITY)

# create the release APK
build-release :
	ant release
	jarsigner -verbose -keystore $(KEYSTORE) \
		bin/$(APPNAME)-unsigned.apk $(KEY_ALIAS)
	mv bin/$(APPNAME)-unsigned.apk bin/$(APPNAME)-unaligned.apk
	jarsigner -verify -verbose -certs bin/$(APPNAME)-unaligned.apk
	zipalign -vf 4 bin/$(APPNAME)-unaligned.apk bin/$(APPNAME).apk

# install release APK on the first devices available on adb
install-release :
	adb uninstall $(NAMESPACE)
	adb install -rt bin/$(APPNAME).apk
	adb shell am start -a android.intent.action.MAIN \
		-n $(NAMESPACE)/$(NAMESPACE).$(START_ACTIVITY)

# install release APK on the device (ignoring an emulator if present)
install-release-device :
	adb -d uninstall $(NAMESPACE)
	adb -d install -rt bin/$(APPNAME).apk
	adb -d shell am start -a android.intent.action.MAIN \
		-n $(NAMESPACE)/$(NAMESPACE).$(START_ACTIVITY)
