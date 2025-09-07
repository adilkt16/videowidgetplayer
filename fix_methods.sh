#!/bin/bash

# Fix the WidgetVideoManager.kt file by replacing the incorrect initializeQueue calls

FILE="/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/WidgetVideoManager.kt"

# Create a backup
cp "$FILE" "$FILE.backup"

# Use perl to perform multiline replacement
perl -i -pe '
BEGIN{undef $/;}
s/queueManager\.initializeQueue\(\s*context = context,\s*widgetId = widgetId,\s*videoUris = videoUris,\s*startIndex = [^,]*,\s*shuffleEnabled = [^,]*,\s*loopMode = [^\)]*\s*\)/queueManager.initializeQueue(context, widgetId, videoUris)/gs
' "$FILE"

echo "Fixed initializeQueue method calls"
