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
