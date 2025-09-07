# 🎯 Widget Configuration Testing Guide

## ✅ **Fixed Issues:**
1. **Empty button listeners** → Now functional ✅
2. **Video queue vs single URI mismatch** → Fixed ✅ 
3. **Missing video title extraction** → Added ✅
4. **Configuration not saving properly** → Fixed ✅

---

## 🧪 **Testing Steps:**

### **Step 1: Test Updated App**
```bash
# App is already installed - try this:
```

**On your device:**
1. Open **VideoWidget Player** app
2. Try **"Browse Videos"** button - should open video picker
3. Try **"Setup Widget"** button - should show instructions

### **Step 2: Add Widget to Home Screen**
1. **Long-press** empty area on home screen
2. Select **"Widgets"**
3. Find **"Video Widget Player"** or **"VideoWidget"**
4. **Drag widget** to home screen
5. Configuration screen should open

### **Step 3: Select Videos**
1. In configuration screen, tap **"Select Video"**
2. Choose one or more videos from gallery
3. Tap **"Add Widget"** or **"Confirm"**
4. Widget should appear with video thumbnail

### **Step 4: Check Widget Functionality**
- Widget should show video title
- Should display video thumbnail
- Play/pause button should work
- No "Can't load widget" error

---

## 🔧 **Debug Commands:**

### **Real-time Widget Logs:**
```bash
adb logcat | grep -i "VideoWidget\|Configure\|Provider"
```

### **Check Widget Preferences:**
```bash
adb shell dumpsys package com.videowidgetplayer.debug | grep -A 5 "widget"
```

### **Test Widget Update:**
```bash
# Force widget refresh
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

---

## 🐛 **If Still Having Issues:**

### **Check Video Permissions:**
```bash
adb shell dumpsys package com.videowidgetplayer.debug | grep -A 2 "READ_MEDIA"
```

### **Clear App Data (last resort):**
```bash
adb shell pm clear com.videowidgetplayer.debug
```

---

## 📱 **Expected Behavior:**

✅ **Working:** Video selection opens photo picker  
✅ **Working:** Widget configuration saves selections  
✅ **Working:** Widget displays selected video  
✅ **Fixed:** "Can't load widget" should be resolved  

**Try adding the widget now and let me know what happens!**
