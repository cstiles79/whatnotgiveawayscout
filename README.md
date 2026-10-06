# Whatnot Giveaway Scout — single-app prototype

This version combines the prototype workflow into one Android app:

- scanner framework / discovery-source field
- immediate giveaway-found notification
- open the Whatnot show
- manually mark a giveaway as entered
- track entered giveaways locally
- persistent Android alarm for exactly 15 seconds before the scheduled drawing
- high-priority sound/vibration alert

## Important limitation

The app does **not** automate giveaway entry. Whatnot's current rules require entries to be submitted manually and require the entrant to be present when the winner is selected.

The scanner is intentionally an adapter/framework rather than an undocumented Whatnot scraper. The current prototype accepts a discovery-feed URL placeholder and includes a simulated detection button so the complete notification/tracking workflow can be tested immediately. A production scanner needs a permitted, reliable data source for live giveaway events.

## Build the APK without Android Studio

This project includes a GitHub Actions workflow that automatically builds the debug APK in the cloud.

1. Create/sign in to a GitHub account.
2. Create a new repository (it can be private).
3. Upload the **contents** of this project folder to the repository.
4. Go to the repository's **Actions** tab.
5. Select **Build Debug APK** and click **Run workflow** if it has not already run from your upload.
6. When the workflow finishes successfully, open the workflow run and scroll to **Artifacts**.
7. Download **WhatnotGiveawayScout-debug-apk**.
8. Extract the ZIP and install `app-debug.apk` on your Android phone.

The workflow also runs automatically whenever you push changes to the repository.

### Android permissions

On Android 12+, allow exact alarms when prompted. On Android 13+, allow notifications.

### Installing the APK

Android may ask you to allow installation from the browser or file manager used to open the APK. Only enable that permission for a source you trust.

## Test

1. Launch the app.
2. Tap `SIMULATE GIVEAWAY DETECTED`.
3. Enter/adjust the seconds until drawing.
4. Tap `I ENTERED — START 15-SECOND ALERT`.
5. The app schedules the 15-second warning using AlarmManager, so the alert can fire even if the app is not on screen.
