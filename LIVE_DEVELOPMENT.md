# 📱 VideoWidget Live Development Setup

## 🚀 **Current Status: Device Connected & App Running**

**Connected Device:** `00055342M003541`  
**Package Name:** `com.videowidgetplayer.debug`  
**App Status:** ✅ Installed and Running

---

## 🔄 **Live Development Workflow**

### **Terminal 1: File Watcher (Auto-rebuild on changes)**
```bash
./watch-build.sh
```
*This will automatically rebuild the APK whenever you save code changes*

### **Terminal 2: Live App Logs**
```bash
adb logcat | grep -i videowidget
```
*Shows real-time app logs and errors*

### **Terminal 3: Quick Development Commands**
```bash
# Quick reinstall after code changes
./test-dev.sh reinstall

# Install and launch app
./test-dev.sh run

# Check app permissions
adb shell dumpsys package com.videowidgetplayer.debug | grep permission
```

---

## 🛠️ **Development Commands Reference**

### **Instant App Testing**
- `./test-dev.sh install` - Install latest APK to device
- `./test-dev.sh run` - Install and launch app
- `./test-dev.sh logs` - Show app logs in real-time
- `./test-dev.sh reinstall` - Clean reinstall

### **Build Commands**  
- `./gradlew assembleDebug` - Build APK manually
- `./watch-build.sh` - Auto-build on file changes
- `./gradlew clean` - Clean build cache

### **Device Interaction**
- `adb devices` - List connected devices
- `adb logcat -c && adb logcat | grep VideoWidget` - Clear and watch logs
- `adb shell am start -n com.videowidgetplayer.debug/com.videowidgetplayer.ui.MainActivity` - Launch app

---

## 🐛 **Debug Common Issues**

### **Video Browse/Setup Issues:**
1. **Check Permissions:**
   ```bash
   adb shell dumpsys package com.videowidgetplayer.debug | grep permission
   ```

2. **Grant Storage Permission:**
   ```bash
   adb shell pm grant com.videowidgetplayer.debug android.permission.READ_MEDIA_VIDEO
   ```

3. **Test File Access:**
   ```bash
   adb logcat | grep -E "(VideoWidget|Permission|Storage)"
   ```

### **Widget Setup Issues:**
1. **Add Widget:** Long-press home screen → Widgets → VideoWidget
2. **Check Logs:** `adb logcat | grep Widget`
3. **Reset Widget:** Remove and re-add widget

---

## 📝 **Current Development Focus**

### Issues to Fix:
- [ ] Video browsing not working
- [ ] Widget setup problems  
- [ ] File permission issues

### Testing Checklist:
- [ ] App launches successfully ✅
- [ ] Main activity opens ✅  
- [ ] Video browsing works
- [ ] Widget configuration works
- [ ] Video playback works
- [ ] Gesture controls work

---

## 🔥 **Quick Start Development Session**

1. **Open 3 terminals** in the project directory
2. **Terminal 1:** `./watch-build.sh` (auto-rebuild)
3. **Terminal 2:** `adb logcat | grep -i videowidget` (live logs)  
4. **Terminal 3:** Keep free for quick commands
5. **Edit code** in your editor - changes auto-rebuild!
6. **Test immediately** on device

---

*📱 Your device is ready for live development! Start coding and see changes instantly.*
