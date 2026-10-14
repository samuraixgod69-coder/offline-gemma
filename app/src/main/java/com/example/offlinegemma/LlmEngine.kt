package com.example.offlinegemma

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

/** Thin wrapper around MediaPipe's LLM Inference API. */
class LlmEngine(private val context: Context) {

    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null

    /** Loads the model into RAM. Slow (seconds) - call off the main thread. */
    fun load(path: String) {
        close()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(2048)          // prompt + history + reply budget
            .build()
        llm = LlmInference.createFromOptions(context, options)
        resetSession()
    }

    /** A session remembers the conversation. A fresh one = a new chat. */
    fun resetSession() {
        session?.close()
        val opts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTopP(0.95f)
            .setTemperature(0.8f)
            .build()
        session = LlmInferenceSession.createFromOptions(llm ?: error("Model not loaded"), opts)
    }

    /** Streams the reply: onPartial(textPiece, isDone) is called repeatedly. */
    fun generate(userText: String, onPartial: (String, Boolean) -> Unit) {
        val s = session ?: error("Model not loaded")
        // Gemma's chat format
        s.addQueryChunk("<start_of_turn>user\n$userText<end_of_turn>\n<start_of_turn>model\n")
        s.generateResponseAsync { partial, done -> onPartial(partial ?: "", done) }
    }

    fun close() {
        session?.close(); session = null
        llm?.close(); llm = null
    }
}
