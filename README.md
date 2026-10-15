# Offline Gemma - on-device chat app

## About the model
There is no "Gemma 4 1B". Gemma 4 ships as E2B / E4B (bigger). The closest 1B model is
**Gemma 3 1B IT (int4)** - this app uses that, via MediaPipe LLM Inference.
Note: MediaPipe LLM Inference is in maintenance mode (Google now recommends LiteRT-LM),
but it still works fine for Gemma 3 1B.

## Get the model file (~550 MB)
1. Create a free Hugging Face account: https://huggingface.co
2. Open https://huggingface.co/litert-community/Gemma3-1B-IT and accept the Gemma license.
3. Download the file `gemma3-1b-it-int4.task` (Files tab). Check the exact name on that page.
4. Place it anywhere on your phone (e.g. Download folder).
   In the app tap "Pick model file from storage" - it copies the file into the app's private
   storage (you can delete the original afterwards).
   OR: Settings > Access Tokens > create a "Read" token, paste it into the app, tap "Download model".

## Build WITHOUT a PC (GitHub Actions)
1. Create a new GitHub repo (empty).
2. In Termux: unzip the project, then
   cd OfflineGemma && git init && git add . && git commit -m "init"
   git branch -M main && git remote add origin <your repo url> && git push -u origin main
3. On GitHub: Actions tab > "Build APK" > wait ~5-8 min > open the run > Artifacts >
   download OfflineGemma-debug-apk, unzip, install app-debug.apk (allow "install unknown apps").

## Build with Android Studio
File > Open > select this folder > wait for Gradle sync > Run (or Build > Build APK).

## How it works
- settings.gradle.kts / build.gradle.kts / app/build.gradle.kts: Gradle build config. The key line is
  `com.google.mediapipe:tasks-genai` - Google's library that runs the LLM.
- AndroidManifest.xml: app declaration; INTERNET permission only for the optional download;
  largeHeap asks Android for more memory.
- MainActivity.kt: entry point; shows the Compose UI.
- ChatViewModel.kt: the "brain" of the screen. Holds model state, chat history (in RAM),
  RAM check, error handling. Survives screen rotation.
- LlmEngine.kt: wraps MediaPipe. load() reads the model into RAM; a "session" remembers the
  conversation; generateResponseAsync streams text piece by piece; a new session = New chat.
- ModelManager.kt: copies the picked file or downloads it with progress.
- ChatScreen.kt: Compose UI: setup screen (pick/download/progress/errors) and chat screen
  (message bubbles, text box, Send button, New chat button). Dark mode follows the phone setting.
- .github/workflows/build.yml: tells GitHub to build the APK for you.

## Troubleshooting
- Crash while loading / "out of memory": phone needs roughly 4 GB+ RAM; close other apps.
- Won't load: file must be the MediaPipe `.task` file, not GGUF or safetensors.
- Emulators are not supported by MediaPipe LLM; use a real phone.

## v2.2 (AI Toolbox update)
- Added an AI Toolbox with **64 prompt tools** across learning, math, writing, languages, coding,
  productivity, communication, career, science, research, everyday tasks, documents, and AI helpers.
- Search tools by name/description and filter by category.
- Tap a tool to place its prompt starter in the chat composer; add your own details before sending.
- Existing features remain: offline model import/download, chat history, history search, markdown replies,
  copy/read-aloud actions, stop generation, quick prompts, temperature/max-token/system-prompt settings,
  model info, delete model, and navigation drawer.
- Project version is 2.2 (versionCode 3). Debug builds use the bundled signing key so updates can install over
  the same app when the previous build used this project's key.

Note: The 64 toolbox items are prompt-based helpers powered by the locally loaded model, not 64 separate
AI models. Answer quality and speed depend on the model file and available RAM. This source update has not
been compiled or tested on a physical Android tablet in this environment.


## v2.3 (voice, documents, export, and memory)
- Voice input through Android's speech-recognition activity; recognition availability and language depend on installed device services.
- Import plain-text, Markdown, CSV, or JSON documents into the chat as context (limited to the first 3,000 characters because the 1B model only holds about 2048 tokens). This is not a PDF text extractor.
- Export the current conversation to a Markdown file using Android's save-file picker.
- Optional private memory notes are stored in app preferences on the device and appended to the model's system instructions. Do not store passwords or sensitive details there.
- Existing read-aloud, 64 prompt tools, offline model inference, and chat history are retained.

The project source was updated but an Android build and physical-device test have not been run in this environment. Voice recognition may use an installed Android speech service and therefore is not guaranteed to work fully offline. Image understanding and PDF text extraction are not included in this version.


## v2.3.1
- Document import now trims to 3,000 characters (80,000 would overflow the model context and fail).
- Drawer labels corrected (v2.3, 64 AI Tools).
