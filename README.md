# Video Widget Player

An Android application that allows users to create video widgets for their home screen, enabling quick access and playback of their favorite videos directly from widgets.

## Features

- 🎥 **Video Widget Creation**: Create customizable video widgets for your home screen
- 📱 **ExoPlayer Integration**: High-quality video playback using Google's ExoPlayer
- 🎛️ **Widget Controls**: Play, pause, and control videos directly from the widget
- 📁 **Gallery Access**: Browse and select videos from your device's gallery
- 🔧 **Widget Configuration**: Easy-to-use configuration interface for setting up widgets
- 🎨 **Modern UI**: Material Design 3 compliant user interface

## Technical Specifications

- **Target SDK**: Android API 34 (Android 14)
- **Minimum SDK**: Android API 26 (Android 8.0) - Required for proper widget support
- **Programming Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL

## Dependencies

### Core Dependencies
- AndroidX Core KTX
- AppCompat
- Material Design Components
- ConstraintLayout
- Lifecycle components

### Video Playback
- **ExoPlayer 2.19.1**: For high-quality video playback
- ExoPlayer UI components

### Widget Support
- **Glance App Widget 1.0.0**: For modern widget development

### Permissions & Storage
- Activity and Fragment KTX for permission handling
- DocumentFile for media access

### Async Processing
- Kotlin Coroutines for background operations
- ViewModel and LiveData for reactive UI

## Project Structure

```
app/src/main/java/com/videowidgetplayer/
├── widgets/           # Widget-related classes
│   ├── VideoWidgetProvider.kt
│   └── VideoWidgetConfigureActivity.kt
├── ui/                # User interface components
│   ├── MainActivity.kt
│   └── VideoPlayerManager.kt
├── utils/             # Utility classes
│   ├── MediaUtils.kt
│   └── PreferenceUtils.kt
└── data/              # Data models and repositories
    ├── VideoFile.kt
    └── VideoRepository.kt
```

## Permissions

The app requests the following permissions:

### Required Permissions
- `READ_EXTERNAL_STORAGE` (Android < 13)
- `READ_MEDIA_VIDEO` (Android 13+)
- `READ_MEDIA_AUDIO` (Android 13+)
- `INTERNET` - For streaming videos (if needed)
- `ACCESS_NETWORK_STATE` - For network status
- `WAKE_LOCK` - For video playback
- `FOREGROUND_SERVICE` - For background playback (if implemented)

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later (Recommended: Android Studio Flamingo or newer)
- Android SDK 34
- Kotlin 1.9.10 or later
- Git (for version control)
- Java 17 or later

### Setup Instructions

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd VideoWidgetPlayer
```

#### 2. Open in Android Studio
1. Launch Android Studio
2. Select "Open an existing project"
3. Navigate to the VideoWidgetPlayer directory
4. Click "OK"

#### 3. Configure the Project
1. Wait for Gradle sync to complete
2. Ensure you have Android SDK 34 installed
3. Accept any license agreements if prompted
4. Install any missing SDK components

#### 4. Build and Run
1. Connect an Android device (API 26+) or start an emulator
2. Click "Run" (green play button) or use `Ctrl+R` (Windows/Linux) / `Cmd+R` (Mac)
3. Select your target device
4. Wait for the app to install and launch

#### 5. Grant Permissions
1. When first running the app, grant media permissions
2. For Android 13+ devices, you'll be prompted for specific media permissions
3. For older devices, grant storage permissions

### Building the Project
- **Debug Build**: `./gradlew assembleDebug`
- **Release Build**: `./gradlew assembleRelease`
- **Run Tests**: `./gradlew test`
- **Clean Build**: `./gradlew clean`

### Installing Widgets
1. Install the app on your device
2. Grant required media permissions
3. Long press on home screen
4. Select "Widgets"
5. Find "Video Player" widget
6. Drag to home screen
7. Configure with your favorite video

## Widget Features

- **Thumbnail Display**: Shows video thumbnail in widget
- **Playback Controls**: Play/pause buttons directly in widget
- **Customizable Size**: Resizable widget (minimum 250x180dp)
- **Quick Access**: Direct video playback from home screen

## Development Notes

- Uses modern Android development practices
- Implements proper permission handling for Android 13+
- Follows Material Design 3 guidelines
- Uses ViewBinding for type-safe view access
- Implements proper lifecycle management

## Git Workflow & Development

This project uses a simplified Git workflow suitable for personal or small team development:

### Simple Workflow
- **`main`**: Primary development branch - all changes go here
- **Optional feature branches**: For larger features that need isolation

### Development Process

#### For Most Changes (Recommended):
1. Work directly on `main` branch
2. Make small, focused commits
3. Push changes regularly: `git push`
4. Tag releases when ready: `git tag v1.0.0`

#### For Larger Features (Optional):
1. Create feature branch: `git checkout -b feature-name`
2. Develop the feature with clear commits
3. Merge back to main: `git checkout main && git merge feature-name`
4. Delete feature branch: `git branch -d feature-name`

### Commit Message Guidelines
Use clear, descriptive commit messages:
- `Add video thumbnail preview to widget`
- `Fix ExoPlayer memory leak issue`
- `Update README with setup instructions`
- `Refactor VideoPlayerManager for better performance`

## Future Enhancements

- Multiple video widgets support
- Video thumbnail generation
- Widget customization options
- Playlist support
- Streaming video support
- Advanced playback controls

## License

This project is created for educational and development purposes.
