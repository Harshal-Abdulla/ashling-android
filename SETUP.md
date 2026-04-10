# Local AI Chat — Setup Guide

This guide walks you through everything, step by step.

---

## What we're building
A native Android chat app that runs Google's **Gemma 2B** AI model entirely on your phone.
No internet needed after setup. No server. Just your phone and the model.

---

## Step 1 — Install Android Studio

1. Go to: https://developer.android.com/studio
2. Download and install (click Next/Agree on everything)
3. When it asks about installing an Android Virtual Device (emulator), say **Yes**
4. Let it finish — this takes a while (~10 minutes)

---

## Step 2 — Open this project in Android Studio

1. Open Android Studio
2. Click **"Open"** (not "New Project")
3. Navigate to this folder: `Desktop/ollama in android`
4. Click **OK**
5. Android Studio will ask to download Gradle — click **OK** and wait

> If it shows errors in red, wait for Gradle sync to finish (bottom progress bar).
> Usually fixes itself after the first sync.

---

## Step 3 — Download the AI model

The app uses Google's **Gemma 2B IT** model in a special compressed format (~1.4 GB).

1. Go to: https://www.kaggle.com/models/google/gemma/frameworks/tfLite
2. Create a free Kaggle account if you don't have one
3. Find: **Gemma 2B IT CPU int4** → Download the `.bin` file
4. The file is named something like `gemma-2b-it-cpu-int4.bin`

---

## Step 4 — Enable Developer Mode on your Android phone

1. Open **Settings** on your phone
2. Go to **About Phone** → tap **Build Number** 7 times
3. You'll see "You are now a developer!"
4. Go back to Settings → **Developer Options** → enable **USB Debugging**

---

## Step 5 — Connect your phone and install the app

1. Connect your phone to your Mac with a USB cable
2. On your phone, tap **"Allow"** when it asks about USB debugging
3. In Android Studio, click the **▶ Run** button (green play button at the top)
4. Select your phone from the list
5. The app will install and open

---

## Step 6 — Push the model to your phone

The model is too big to bundle inside the app (1.4 GB!), so we copy it directly.

Open **Terminal** on your Mac and run:

```bash
# Replace the path with wherever you downloaded the model
adb push ~/Downloads/gemma-2b-it-cpu-int4.bin /data/data/com.example.localllm/files/gemma-2b-it-cpu-int4.bin
```

> `adb` (Android Debug Bridge) is a tool that comes with Android Studio.
> If `adb` isn't found, add it to your PATH:
> ```bash
> export PATH=$PATH:~/Library/Android/sdk/platform-tools
> ```

---

## Step 7 — Open the app and chat!

1. Open the app on your phone
2. Wait ~15 seconds while it loads the model into RAM
3. The Send button will become enabled when it's ready
4. Type a message and hit Send!

---

## Troubleshooting

**"Model file not found"** — Run the `adb push` command from Step 6.

**Build errors in Android Studio** — Try **File → Sync Project with Gradle Files**.

**"adb: command not found"** — See Step 6 for the PATH fix.

**App crashes on launch** — Your phone may not have enough free RAM. Close all other apps and try again.

**Very slow responses** — Normal for the first message. Subsequent messages are faster.
The Gemma 2B model generates about 5-15 tokens/second on a mid-range phone.

---

## File structure reference

```
app/src/main/
├── java/com/example/localllm/
│   ├── MainActivity.kt     ← App logic + AI engine
│   ├── ChatAdapter.kt      ← Manages the scrolling message list
│   └── ChatMessage.kt      ← Simple data container (text + isUser flag)
└── res/
    ├── layout/
    │   ├── activity_main.xml       ← Main screen layout
    │   ├── item_user_message.xml   ← Blue (user) bubble
    │   └── item_ai_message.xml     ← Gray (AI) bubble
    └── drawable/
        ├── bg_user_bubble.xml      ← Rounded rect shape (blue)
        └── bg_ai_bubble.xml        ← Rounded rect shape (gray)
```
