package com.example.offlinegemma

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

/** Thin wrapper around MediaPipe's LLM Inference API. */
class LlmEngine(private val context: Context) {

    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var systemPrompt = ""
    private var firstTurn = true

    /** Loads the model into RAM (slow - call off the main thread). */
    fun load(path: String, maxTokens: Int) {
        close()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(maxTokens)
            .build()
        llm = LlmInference.createFromOptions(context, options)
    }

    /** New session = fresh memory. `history` is replayed so an old chat can continue. */
    fun startSession(temperature: Float, system: String, history: List<ChatMessage>) {
        session?.close()
        val opts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTopP(0.95f)
            .setTemperature(temperature)
            .build()
        val s = LlmInferenceSession.createFromOptions(llm ?: error("Model not loaded"), opts)
        session = s
        systemPrompt = system
        firstTurn = true

        var i = 0
        while (i + 1 < history.size) {
            val u = history[i]
            val a = history[i + 1]
            if (u.isUser && !a.isUser && a.text.isNotBlank() && !a.text.contains("[Error:")) {
                s.addQueryChunk(userTurn(u.text))
                firstTurn = false
                s.addQueryChunk(a.text + "<end_of_turn>\n")
                i += 2
            } else {
                i += 1
            }
        }
    }

    private fun userTurn(text: String): String {
        val sys = if (firstTurn && systemPrompt.isNotBlank()) systemPrompt + "\n\n" else ""
        return "<start_of_turn>user\n" + sys + text + "<end_of_turn>\n<start_of_turn>model\n"
    }

    /** Streams the reply: onPartial(textPiece, isDone) is called repeatedly. */
    fun generate(userText: String, onPartial: (String, Boolean) -> Unit) {
        val s = session ?: error("Model not loaded")
        s.addQueryChunk(userTurn(userText))
        firstTurn = false
        s.generateResponseAsync { partial, done -> onPartial(partial ?: "", done) }
    }

    /** Asks MediaPipe to stop generating (called by name so a missing method can't break the build). */
    fun cancel() {
        try {
            val s = session ?: return
            s.javaClass.getMethod("cancelGenerateResponseAsync").invoke(s)
        } catch (_: Throwable) {
        }
    }

    fun close() {
        try { session?.close() } catch (_: Throwable) {}
        try { llm?.close() } catch (_: Throwable) {}
        session = null
        llm = null
    }
}
