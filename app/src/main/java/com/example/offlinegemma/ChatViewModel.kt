package com.example.offlinegemma

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val id: Long, val text: String, val isUser: Boolean)

sealed interface ModelState {
    data object NotLoaded : ModelState
    data class Working(val label: String, val progress: Float?) : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val engine = LlmEngine(application)
    private var nextId = 0L

    val modelState = MutableStateFlow<ModelState>(ModelState.NotLoaded)
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())   // chat history (RAM only)
    val generating = MutableStateFlow(false)

    init {
        if (ModelManager.modelFile(app).exists()) loadModel()
    }

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
            modelState.value = ModelState.Working("Loading model... (can take up to a minute)", null)
            try {
                engine.load(file.absolutePath)
                messages.value = emptyList()
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

    fun importModel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || generating.value || modelState.value !is ModelState.Ready) return
        generating.value = true
        messages.update {
            it + ChatMessage(nextId++, prompt, true) + ChatMessage(nextId++, "", false)
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                engine.generate(prompt) { piece, done ->
                    // runs on MediaPipe's thread; StateFlow.update is thread-safe
                    messages.update { list ->
                        val last = list.last()
                        list.dropLast(1) + last.copy(text = last.text + piece)
                    }
                    if (done) generating.value = false
                }
            } catch (e: Throwable) {
                messages.update { list ->
                    val last = list.last()
                    list.dropLast(1) + last.copy(text = last.text + "\n[Error: ${e.message}]")
                }
                generating.value = false
            }
        }
    }

    fun newChat() {
        if (generating.value) return
        try { engine.resetSession() } catch (_: Throwable) {}
        messages.value = emptyList()
    }

    override fun onCleared() {
        engine.close()
    }
}
