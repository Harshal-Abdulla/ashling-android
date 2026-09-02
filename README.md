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

The app does not ship with a model — they are too big to put in an APK. You
pick one on the Switch Model screen and it downloads there.

Four of them need no account at all. Start with SmolLM if you just want to see
it working:

| Model | Size | Needs |
|---|---|---|
| SmolLM 135M | 160 MB | any phone |
| TinyLlama 1.1B | 1.1 GB | 3 GB RAM |
| Qwen 2.5 1.5B | 1.5 GB | 4 GB RAM |
| DeepSeek R1 1.5B | 1.8 GB | 4 GB RAM |

The Gemma models are also there, but Google puts those behind Kaggle, so those
need a free Kaggle account and an API token:

1. Sign in at [kaggle.com](https://www.kaggle.com) and go to Settings
2. Under API, click "Create New Token" — it downloads a `kaggle.json` file
3. Open that file to get your username and key
4. Accept the [Gemma terms](https://ai.google.dev/gemma/terms) on the model page
5. Enter them in the app when it asks

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

- The prompt is written in Gemma's format. Other models still work, but they
  don't recognise Gemma's end-of-turn token, so they keep writing past their
  answer. The reply gets trimmed at the first stop token to hide that, which
  works but is a patch over the real problem.
- Only the Gemma models need a Kaggle account now, but that flow is still
  clumsy if you want one.
- Generation is slow on older phones. A 2B model is a lot to ask of a mid-range
  device, and the first reply after opening the app is the slowest.
- No way to delete a downloaded model from inside the app yet. You have to
  clear the app's storage in Android settings.

## Why I built it

I wanted to learn Kotlin properly, and running a model on-device seemed a more
interesting way to do that than another to-do list. The privacy side is a real
benefit too — nothing typed into it leaves the phone.
