#!/bin/bash

# Quick fix for Option A: Minimal working test build
# Fix method signature mismatches in the calling files

echo "🚀 Creating Option A: Quick Test Build..."

# Fix VideoWidgetConfigureActivity method calls
sed -i 's/videoManager\.initialize(context)/videoManager.initialize(context, appWidgetId)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/VideoWidgetConfigureActivity.kt"

sed -i 's/videoManager\.initializeVideoQueue(context, selectedVideoUris)/videoManager.initializeVideoQueue(context, appWidgetId)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/VideoWidgetConfigureActivity.kt"

sed -i 's/videoManager\.setGestureSensitivity(context, sensitivity, appWidgetId)/videoManager.setGestureSensitivity(context, sensitivity.toFloat())/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/VideoWidgetConfigureActivity.kt"

# Fix VideoWidgetProvider method calls - add widgetIds parameter to all initialize() calls
sed -i 's/videoManager\.initialize(context)/videoManager.initialize(context, appWidgetIds[0])/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/VideoWidgetProvider.kt"

sed -i 's/videoManager\.release(context)/videoManager.release(context, appWidgetId)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/VideoWidgetProvider.kt"

# Fix WidgetGestureReceiver method calls
sed -i 's/videoManager\.initialize(context)/videoManager.initialize(context, 0)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/receivers/WidgetGestureReceiver.kt"

sed -i 's/videoManager\.nextVideoWithGesture(context, widgetId, gestureType, intensity)/videoManager.nextVideoWithGesture(context, widgetId)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/receivers/WidgetGestureReceiver.kt"

sed -i 's/videoManager\.previousVideoWithGesture(context, widgetId, gestureType, intensity)/videoManager.previousVideoWithGesture(context, widgetId)/g' \
    "/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/receivers/WidgetGestureReceiver.kt"

echo "✅ Fixed method signature mismatches for Option A build"
echo "🔧 Ready to build minimal test APK"
