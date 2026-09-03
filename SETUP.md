# Ashling: setup guide

Most people do not need this file. If you just want the app, download the APK
from the [Releases page](https://github.com/Harshal-Abdulla/ashling-android/releases)
and skip to "Getting a model" in the [README](README.md).

This guide is for building it from source in Android Studio.

## 1. Install Android Studio

Download it from [developer.android.com/studio](https://developer.android.com/studio)
and accept the defaults. Say yes when it offers to install an emulator, which is
useful if you do not want to plug a phone in. The install takes a while.

You need JDK 21. Android Studio ships with a suitable one, so this usually
sorts itself out.

## 2. Open the project

In Android Studio, choose **Open** rather than New Project, and point it at this
folder. It will download Gradle on first open, which takes a few minutes.

Red errors during that first sync are normal. Wait for the progress bar at the
bottom to finish before worrying about them.

If the project lives in a folder that iCloud Drive syncs, such as Desktop or
Documents, move it somewhere like `~/Projects` first. iCloud creates
"file 2.xml" duplicates inside `app/build` and the dex step then fails on them.

## 3. Run it

On an emulator, pick one from the device dropdown and press Run.

On your own phone:

1. Open **Settings**, go to **About Phone**, tap **Build Number** seven times
2. Go back, find **Developer Options**, turn on **USB Debugging**
3. Plug the phone in and accept the prompt asking you to trust the computer
4. Pick the phone in the device dropdown and press Run

From the command line instead of the IDE:

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## 4. Get a model

The app ships without one, because model files are over a gigabyte.

Open the app, go to **Switch Model**, and pick one. It downloads inside the app.
Qwen 2.5 1.5B is the one to choose. There is no need to download anything
manually or push files over adb.

Five of the models need no account. The Gemma ones are behind Kaggle and need a
free account and an API token, which the README explains.

## If something goes wrong

**Build fails after you moved or renamed the project.** Run
`./gradlew clean`, then build again.

**Gradle or JDK complaints.** This needs JDK 21 and Gradle 8.13. JDK 25 and
Gradle 9 both fail. The wrapper pins the Gradle version, so the JDK is the one
to check.

**The app crashes when a model loads.** The phone probably does not have enough
free RAM for that model. Close everything else, or pick a smaller one. SmolLM
135M runs on anything.

**First reply is very slow.** That is normal. The model is being loaded into
memory. Later messages in the same session are faster.
