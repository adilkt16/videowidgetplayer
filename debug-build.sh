#!/bin/bash

# Debug Build Script - Enhanced APK with detailed logging
echo "🐛 Creating debug build with enhanced logging..."

# Add debug logging to key components
echo "📝 Adding debug outputs..."

# Build with verbose output
./gradlew assembleDebug --debug --stacktrace > build-debug.log 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Debug build successful!"
    echo "📱 APK: app/build/outputs/apk/debug/app-debug.apk"
    echo "📋 Build log: build-debug.log"
    
    # Show APK details
    ls -lah app/build/outputs/apk/debug/app-debug.apk
    
    echo ""
    echo "🔍 To debug issues:"
    echo "1. Install: ./test-dev.sh install"
    echo "2. Run with logs: ./test-dev.sh logs"
    echo "3. Check permissions: adb shell dumpsys package com.videowidgetplayer"
    
else
    echo "❌ Build failed! Check build-debug.log for details"
    tail -20 build-debug.log
fi
