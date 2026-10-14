package com.example.offlinegemma

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val id: Long, val text: String, val isUser: Boolean)
data class Conversation(val id: Long, val title: String, val updatedAt: Long, val messages: List<ChatMessage>)

enum class Screen { HOME, CHAT, MODELS, SETTINGS }

sealed interface ModelState {
    data object NotLoaded : ModelState
    data class Working(val label: String, val progress: Float?) : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val engine = LlmEngine(application)
    private var tts: TextToSpeech? = null

    val modelState = MutableStateFlow<ModelState>(ModelState.NotLoaded)
    val screen = MutableStateFlow(Screen.HOME)
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val generating = MutableStateFlow(false)
    val settings = MutableStateFlow(SettingsStore.load(application))
    val currentTitle = MutableStateFlow("New chat")

    private var currentId: Long? = null
    private var nextMsgId = 0L
    private var sessionReady = false
    private var genId = 0

    init {
        conversations.value = ChatStore.load(app)
        tts = TextToSpeech(app) { }
        if (ModelManager.modelFile(app).exists()) {
            modelState.value = ModelState.Working("Loading Gemma...", null)
            loadModel()
        }
    }

    fun navigate(s: Screen) { screen.value = s }

    fun modelSizeMb(): Int = (ModelManager.modelFile(app).length() / (1024 * 1024)).toInt()

    // ---------------- model loading ----------------

    fun loadModel() {
        viewModelScope.launch(Dispatchers.Default) {
            val file = ModelManager.modelFile(app)
            if (!file.exists()) {
                modelState.value = ModelState.Failed("Model file is missing. Pick it or download it.")
                return@launch
            }
            val mem = ActivityManager.MemoryInfo()
            (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mem)
            val gb = 1024L * 1024 * 1024
            if (mem.totalMem < 2.8 * gb) {
                modelState.value = ModelState.Failed(
                    "Not enough RAM: this phone has under 3 GB. The model needs about 1.5 GB free."
                )
                return@launch
            }
            if (mem.availMem < 1.2 * gb) {
                modelState.value = ModelState.Failed(
                    "Only ${"%.1f".format(mem.availMem.toDouble() / gb)} GB RAM is free. " +
                        "Close other apps and tap Retry."
                )
                return@launch
            }
            modelState.value = ModelState.Working("Loading Gemma...", null)
            try {
                engine.load(file.absolutePath, settings.value.maxTokens)
                sessionReady = false
                modelState.value = ModelState.Ready
            } catch (e: OutOfMemoryError) {
                modelState.value = ModelState.Failed("Ran out of memory while loading the model.")
            } catch (e: Throwable) {
                modelState.value = ModelState.Failed(
                    "Could not load the model: ${e.message}\n" +
                        "The file may be damaged or the wrong type (needs a .task file)."
                )
            }
        }
    }

    fun reloadModel() {
        if (generating.value) stop()
        sessionReady = false
        loadModel()
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            engine.close()
            modelState.value = ModelState.Working("Copying model into the app...", 0f)
            try {
                ModelManager.importFromUri(app, uri) {
                    modelState.value = ModelState.Working("Copying model into the app...", it)
                }
                loadModel()
            } catch (e: Exception) {
                modelState.value = ModelState.Failed(e.message ?: "Could not read the file")
            }
        }
    }

    fun downloadModel(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            modelState.value = ModelState.Working("Downloading model...", 0f)
            try {
                ModelManager.download(app, ModelManager.DEFAULT_URL, token) {
                    modelState.value = ModelState.Working("Downloading model...", it)
                }
                loadModel()
            } catch (e: Exception) {
                modelState.value = ModelState.Failed(e.message ?: "Download failed (check internet)")
            }
        }
    }

    fun deleteModel() {
        if (generating.value) stop()
        engine.close()
        ModelManager.modelFile(app).delete()
        sessionReady = false
        modelState.value = ModelState.NotLoaded
    }

    // ---------------- settings ----------------

    fun saveSettings(s: AppSettings) {
        SettingsStore.save(app, s)
        settings.value = s
        sessionReady = false   // new temperature / system prompt apply from the next message
    }

    // ---------------- chat ----------------

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || generating.value || modelState.value !is ModelState.Ready) return
        val history = messages.value
        generating.value = true
        messages.update {
            it + ChatMessage(nextMsgId++, prompt, true) + ChatMessage(nextMsgId++, "", false)
        }
        val myGen = ++genId
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (!sessionReady) {
                    val s = settings.value
                    engine.startSession(s.temperature, s.systemPrompt, history)
                    sessionReady = true
                }
                engine.generate(prompt) { piece, done ->
                    if (myGen != genId) return@generate
                    messages.update { list ->
                        val last = list.last()
                        list.dropLast(1) + last.copy(text = last.text + piece)
                    }
                    if (done) {
                        generating.value = false
                        persistChat()
                    }
                }
            } catch (e: Throwable) {
                if (myGen == genId) {
                    messages.update { list ->
                        val last = list.last()
                        list.dropLast(1) + last.copy(text = last.text + "\n[Error: ${e.message}]")
                    }
                    sessionReady = false
                    generating.value = false
                    persistChat()
                }
            }
        }
    }

    fun stop() {
        if (!generating.value) return
        genId++              // ignore any pieces that still arrive
        engine.cancel()
        generating.value = false
        sessionReady = false // rebuild the session (with the partial reply) on the next message
        persistChat()
    }

    fun newChat() {
        if (generating.value) stop()
        currentId = null
        messages.value = emptyList()
        nextMsgId = 0
        sessionReady = false
        currentTitle.value = "New chat"
    }

    fun openChat(id: Long) {
        if (generating.value) stop()
        val c = conversations.value.firstOrNull { it.id == id } ?: return
        currentId = c.id
        messages.value = c.messages
        nextMsgId = c.messages.size.toLong()
        currentTitle.value = c.title
        sessionReady = false
    }

    fun deleteChat(id: Long) {
        conversations.update { list -> list.filter { it.id != id } }
        ChatStore.save(app, conversations.value)
        if (currentId == id) newChat()
    }

    @Synchronized
    private fun persistChat() {
        val msgs = messages.value.filter { it.text.isNotBlank() }
        val first = msgs.firstOrNull { it.isUser } ?: return
        val id = currentId ?: System.currentTimeMillis().also { currentId = it }
        val title = first.text.trim().replace("\n", " ").take(40)
        val conv = Conversation(id, title, System.currentTimeMillis(), msgs)
        conversations.update { list -> listOf(conv) + list.filter { it.id != id } }
        currentTitle.value = title
        ChatStore.save(app, conversations.value)
    }

    fun speak(text: String) {
        val clean = text.replace(Regex("[*`#]"), "")
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "gemma")
    }

    override fun onCleared() {
        engine.close()
        tts?.stop()
        tts?.shutdown()
    }
}
