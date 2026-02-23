# Egg Timer App

A professional and reliable Egg Timer application built with modern Android development practices. This app ensures you get the perfect boiled egg every time with precise timing and background support.

## 🌟 Features

*   **Three Boiling Modes**: Quick presets for Soft, Medium, and Hard-boiled eggs with detailed descriptions.
*   **Visual Countdown**: A beautiful circular progress indicator and large timer display in the app.
*   **Background Support**: Uses a Foreground Service to keep the timer running even if the app is closed or the screen is off.
*   **Smart Notifications**: A persistent notification shows the remaining time and provides quick controls (Stop/Stop Alarm).
*   **Customizable Alarm**: Choose your favorite ringtone or music for the alarm using the built-in sound picker.
*   **Haptic Feedback**: The device vibrates when your egg is ready, ensuring you don't miss the alert.
*   **Cooking Guide**: Includes step-by-step instructions for the perfect boiling process.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Design System**: Material 3
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Architecture**: Android Services (Foreground Service), ViewModel-like state management.

## 🚀 How to Build & Run

1.  Clone this repository.
2.  Open the project in **Android Studio (Ladybug or newer)**.
3.  Connect an Android device or start an emulator.
4.  Click the **Run** button.

## 📦 How to Get the APK

To generate an APK file that you can install on any Android device:

1.  In Android Studio, go to the top menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2.  Wait for the build to finish. A notification will appear in the bottom-right corner.
3.  Click the **locate** link in that notification.
4.  This will open the folder containing `app-debug.apk`. This is your app file!

## 📤 Sharing & Hosting

### How to Share
*   **Direct Share**: You can send the `.apk` file directly via WhatsApp, Telegram, or Email.
*   **Installation**: The recipient will need to allow "Install from unknown sources" in their Android settings to install it.

### Where to Host
1.  **GitHub Releases**: If you host your code on GitHub, you can create a "Release" and upload the APK there. This is the best way for open-source projects.
2.  **Google Drive / Dropbox**: Upload the APK and share the link with "Anyone with the link can view".
3.  **Firebase App Distribution**: Perfect for sending the app to a specific group of testers.
4.  **Google Play Store**: The official way to reach everyone, though it requires a developer account fee and app review.
5.  **Free File Hosting**: Services like MEGA or MediaFire are quick for temporary sharing.
