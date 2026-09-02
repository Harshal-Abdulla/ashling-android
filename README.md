# Solas

An Android chat app that runs a language model on the phone itself. Once the
model file is downloaded there is no internet involved — nothing is sent to a
server, and it works in aeroplane mode.

"Solas" is the Irish word for light.

<img src="docs/screenshot.png" width="320" alt="Solas running on a Pixel 9 Pro emulator">

## Download

Grab the APK from the [Releases page](https://github.com/Harshal-Abdulla/local-ai-android/releases).

Android will warn you about installing outside the Play Store. That is expected
for a sideloaded app — you have to allow "install unknown apps" for whatever
browser or file manager you used.

Needs Android 8.0 or newer.

## Getting a model

The app does not ship with a model, because they are over a gigabyte and the
licence does not allow redistributing them. You download one from inside the
app, from the Switch Model screen.

The models are hosted on Kaggle, so you need a free Kaggle account and an API
token:

1. Sign in at [kaggle.com](https://www.kaggle.com) and go to Settings
2. Under API, click "Create New Token" — it downloads a `kaggle.json` file
3. Open that file to get your username and key
4. Accept the [Gemma terms](https://ai.google.dev/gemma/terms) on the model page
5. Enter the username and key in the app when it asks

That is a lot of steps for a first run and it is the weakest part of the app.
It is the way Google distributes these models though.

## What it does

- Chat with a Gemma model running locally on the device
- Answers stream back word by word instead of appearing all at once
- Remembers the conversation, so follow-up questions work
- Pick between four Gemma models depending on how much storage and RAM you have
- Warns you on first launch that a small model gets things wrong

## Building it yourself

```
git clone https://github.com/Harshal-Abdulla/local-ai-android.git
cd local-ai-android
./gradlew assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`.

You need JDK 21. JDK 25 does not work with this version of the Android Gradle
plugin, and neither does Gradle 9 — the wrapper pins Gradle 8.13 so that is
handled for you.

For a release build you need a `keystore.properties` in the project root
pointing at your own signing key. It is gitignored, so without it the release
build is just unsigned rather than broken.

## Built with

| | |
|---|---|
| Kotlin | the app |
| MediaPipe Tasks GenAI 0.10.22 | running the model on-device |
| Gemma 2 2B (int4) | the default model |
| View binding, RecyclerView, Material 3 | the UI |

## Known problems

- The Kaggle sign-in is a rough first-run experience, as above.
- Generation is slow on older phones. A 2B model is a lot to ask of a mid-range
  device, and the first reply after opening the app is the slowest.
- The prompt is built in Gemma's format, so a non-Gemma model loads but answers
  oddly.
- No way to delete a downloaded model from inside the app yet. You have to
  clear the app's storage in Android settings.

## Why I built it

I wanted to learn Kotlin properly, and running a model on-device seemed a more
interesting way to do that than another to-do list. The privacy side is a real
benefit too — nothing typed into it leaves the phone.
