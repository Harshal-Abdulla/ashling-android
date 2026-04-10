# Local AI Chat — Android

A free, open source Android app that runs AI language models entirely on your device.
No internet connection. No subscription. No data collection. Just AI in your pocket.

Built for students and everyday people who need AI but can't afford cloud subscriptions.

---

## What it does

- Runs Google's Gemma AI models locally on your Android phone
- Full conversation memory — the AI remembers what you said earlier in the chat
- Multiple model support — switch between models from inside the app
- Accuracy warning shown on first launch — honest about AI limitations
- Works completely offline after the model is downloaded

## Why I built this

AI tools like ChatGPT cost $20/month. That's a lot for a student.
Every phone today has enough power to run a small AI model locally.
This app makes that possible — free, private, no strings attached.

---

## Tech Stack

| Tool | Purpose |
|---|---|
| Kotlin | Android app language |
| MediaPipe Tasks GenAI | On-device AI inference |
| Gemma 1.1 2B (LiteRT) | AI model |
| Android ViewBinding | UI |
| SharedPreferences | Storing active model selection |

---

## Models Supported

| Model | Size | License |
|---|---|---|
| Gemma 1.1 2B IT CPU int4 | ~1.3 GB | [Gemma Terms of Use](https://ai.google.dev/gemma/terms) |
| Gemma 2B IT CPU int4 | ~1.4 GB | [Gemma Terms of Use](https://ai.google.dev/gemma/terms) |

> Models are not included in this repo. Download instructions below.

---

## How to Run

### Requirements
- Android Studio (latest)
- Android phone or emulator — API 26+ (Android 8.0), 4GB+ RAM recommended
- Mac/Linux/Windows for development

### Setup

1. Clone this repo:
```bash
git clone https://github.com/Harshal-Abdulla/local-ai-android.git
cd local-ai-android
```

2. Open in Android Studio → let Gradle sync finish

3. Download a model from Kaggle:
   - Go to [kaggle.com/models/google/gemma](https://www.kaggle.com/models/google/gemma)
   - Select **LiteRT** tab → choose `gemma-1.1-2b-it-cpu-int4`
   - Download and extract the `.tar.gz` file

4. Push the model to your device/emulator:
```bash
adb push ~/Downloads/gemma-1.1-2b-it-cpu-int4.bin \
  /sdcard/Android/data/com.example.localllm/files/gemma-1.1-2b-it-cpu-int4.bin
```

5. Hit **Run ▶** in Android Studio

6. Open the app → tap **Models** → tap **Use This Model**

---

## Project Structure

```
app/src/main/
├── java/com/example/localllm/
│   ├── MainActivity.kt          # Main chat screen + AI engine
│   ├── ChatAdapter.kt           # RecyclerView adapter for messages
│   ├── ChatMessage.kt           # Data class for a single message
│   ├── ModelInfo.kt             # Model metadata + supported model list
│   └── ModelLibraryActivity.kt  # Model picker screen
└── res/
    ├── layout/
    │   ├── activity_main.xml         # Chat screen layout
    │   ├── activity_model_library.xml # Model library layout
    │   ├── item_user_message.xml     # User chat bubble
    │   ├── item_ai_message.xml       # AI chat bubble
    │   └── item_model.xml            # Model card in library
    └── values/
        ├── strings.xml   # App text
        ├── colors.xml    # Color palette
        └── themes.xml    # App theme
```

---

## Roadmap

- [ ] App icon and branding
- [ ] More models (Phi-3 Mini, TinyLlama)
- [ ] Settings screen (response length, style)
- [ ] Export conversation
- [ ] iOS version (Flutter)
- [ ] Play Store release

---

## Contributing

Pull requests are welcome. If you want to add a new model, improve the UI,
or fix a bug — open an issue first so we can discuss it.

---

## Disclaimer

AI responses can be inaccurate, biased or wrong.
Do not use this app for medical, legal, financial or safety decisions.
Always verify important information from trusted sources.

---

## Author

Built by [Harshal Abdulla](https://github.com/Harshal-Abdulla)

Grad student in Ireland. Built this because AI should be free for everyone.
