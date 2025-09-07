#!/bin/bash

# Watch Mode Script for VideoWidget Development
# Automatically rebuilds APK when source files change

echo "🚀 Starting VideoWidget Watch Mode..."
echo "📱 APK will be rebuilt automatically when you save changes"
echo "📁 Watching: app/src/ directory"
echo "🔄 Press Ctrl+C to stop"
echo ""

# Function to build and show result
build_app() {
    echo "🔨 Building APK..."
    if ./gradlew assembleDebug --quiet; then
        echo "✅ Build successful! APK updated: $(date)"
        ls -la app/build/outputs/apk/debug/app-debug.apk
        echo ""
    else
        echo "❌ Build failed! Check errors above."
        echo ""
    fi
}

# Initial build
build_app

# Watch for changes using inotifywait
if command -v inotifywait >/dev/null 2>&1; then
    echo "👀 Watching for file changes..."
    inotifywait -m -r -e modify,create,delete --format '%w%f %e' app/src/ | while read file event; do
        if [[ $file == *.kt ]] || [[ $file == *.xml ]] || [[ $file == *.java ]]; then
            echo "📝 Changed: $file"
            sleep 2  # Brief delay to allow file write completion
            build_app
        fi
    done
else
    echo "⚠️  inotify-tools not found. Install with: sudo apt-get install inotify-tools"
    echo "🔄 Manual mode: Run './gradlew assembleDebug' after each change"
fi
