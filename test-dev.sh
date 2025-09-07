#!/bin/bash

# Quick Development Testing Script
# Provides multiple options for testing the VideoWidget app

echo "🎬 VideoWidget Development Testing Options"
echo "=========================================="
echo ""

# Check for connected devices
devices=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ "$devices" -gt 0 ]; then
    echo "📱 Found $devices connected device(s)"
    echo ""
    echo "Available commands:"
    echo "1. ./test-dev.sh install     - Install APK to connected device"
    echo "2. ./test-dev.sh run         - Install and run app"
    echo "3. ./test-dev.sh logs        - Show real-time app logs"
    echo "4. ./test-dev.sh reinstall   - Uninstall and reinstall app"
    echo "5. ./test-dev.sh watch       - Install and watch logs continuously"
    echo ""
else
    echo "⚠️  No devices connected via ADB"
    echo ""
    echo "Setup options:"
    echo "1. Connect Android device with USB debugging enabled"
    echo "2. Use Android Studio emulator"
    echo "3. Use manual APK installation"
    echo ""
fi

echo "🔧 Development commands:"
echo "• ./watch-build.sh          - Auto-rebuild on file changes"
echo "• ./gradlew assembleDebug   - Manual build"
echo "• ./gradlew clean           - Clean build"
echo ""

case "$1" in
    "install")
        echo "📦 Installing APK..."
        ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
        ;;
    "run")
        echo "🚀 Installing and running app..."
        ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "Starting app..."
        adb shell am start -n com.videowidgetplayer/.ui.MainActivity
        ;;
    "logs")
        echo "📋 Showing app logs (Ctrl+C to stop)..."
        adb logcat | grep -i videowidget
        ;;
    "reinstall")
        echo "🔄 Reinstalling app..."
        adb uninstall com.videowidgetplayer 2>/dev/null
        ./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk
        ;;
    "watch")
        echo "👀 Installing and watching logs..."
        ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "Watching logs (Ctrl+C to stop)..."
        adb logcat | grep -i videowidget
        ;;
    "")
        echo "Usage: ./test-dev.sh [install|run|logs|reinstall|watch]"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Usage: ./test-dev.sh [install|run|logs|reinstall|watch]"
        ;;
esac
