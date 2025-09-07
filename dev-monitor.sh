#!/bin/bash

# Live Development Monitor
# Monitors logs, builds, and testing in real-time

PROJECT_DIR="/home/user/Desktop/projects/videowidget/VideoWidgetPlayer"
PACKAGE_NAME="com.videowidgetplayer.debug"
ACTIVITY_NAME="com.videowidgetplayer.ui.MainActivity"

echo "🎬 VideoWidget Live Development Monitor"
echo "======================================"
echo "📱 Device: $(adb devices | grep device | cut -f1)"
echo "📦 Package: $PACKAGE_NAME"
echo "🚀 Activity: $ACTIVITY_NAME"
echo ""

show_menu() {
    echo "Choose development action:"
    echo "1. 🔄 Install & Run App"
    echo "2. 📋 Watch Live Logs" 
    echo "3. 🏗️  Build & Install"
    echo "4. 🐛 Debug Permissions"
    echo "5. 🎮 Test Widget Setup"
    echo "6. 🔍 Full System Logs"
    echo "7. 📱 Grant All Permissions"
    echo "8. 🔄 Auto-rebuild Watch Mode"
    echo "9. ❌ Exit"
    echo ""
    read -p "Enter choice (1-9): " choice
}

install_and_run() {
    echo "🔄 Building and installing app..."
    cd "$PROJECT_DIR"
    ./gradlew assembleDebug && \
    adb install -r app/build/outputs/apk/debug/app-debug.apk && \
    adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
    echo "✅ App installed and started!"
}

watch_logs() {
    echo "📋 Watching app logs (Ctrl+C to stop)..."
    adb logcat -c
    adb logcat | grep -i -E "(videowidget|VideoWidget|ERROR|FATAL|AndroidRuntime)"
}

build_install() {
    echo "🏗️ Building and installing..."
    cd "$PROJECT_DIR"
    ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
    echo "✅ Build and install complete!"
}

debug_permissions() {
    echo "🐛 Checking app permissions..."
    adb shell dumpsys package "$PACKAGE_NAME" | grep -A 10 -B 5 "runtime permissions"
    echo ""
    echo "📁 Checking storage access..."
    adb shell ls -la /storage/emulated/0/Movies/ 2>/dev/null || echo "❌ Cannot access Movies folder"
    adb shell ls -la /storage/emulated/0/DCIM/ 2>/dev/null || echo "❌ Cannot access DCIM folder"
}

test_widget() {
    echo "🎮 Testing widget setup..."
    echo "1. Try adding widget from home screen"
    echo "2. Opening widget configuration..."
    adb shell am start -n "$PACKAGE_NAME/com.videowidgetplayer.widgets.VideoWidgetConfigureActivity"
}

full_logs() {
    echo "🔍 Full system logs (Ctrl+C to stop)..."
    adb logcat -c
    adb logcat
}

grant_permissions() {
    echo "📱 Granting all permissions..."
    adb shell pm grant "$PACKAGE_NAME" android.permission.READ_MEDIA_VIDEO
    adb shell pm grant "$PACKAGE_NAME" android.permission.READ_MEDIA_AUDIO  
    adb shell pm grant "$PACKAGE_NAME" android.permission.READ_EXTERNAL_STORAGE
    adb shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS
    echo "✅ Permissions granted!"
}

auto_rebuild() {
    echo "🔄 Starting auto-rebuild watch mode..."
    cd "$PROJECT_DIR"
    ./watch-build.sh
}

# Main loop
while true; do
    show_menu
    case $choice in
        1) install_and_run ;;
        2) watch_logs ;;
        3) build_install ;;
        4) debug_permissions ;;
        5) test_widget ;;
        6) full_logs ;;
        7) grant_permissions ;;
        8) auto_rebuild ;;
        9) echo "👋 Goodbye!"; break ;;
        *) echo "❌ Invalid choice" ;;
    esac
    echo ""
    read -p "Press Enter to continue..."
    echo ""
done
