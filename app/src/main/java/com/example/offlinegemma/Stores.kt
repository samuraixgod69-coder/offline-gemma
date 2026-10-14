package com.example.offlinegemma

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AppSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "You are a helpful assistant. Answer clearly and keep replies concise.",
    val userName: String = "Offline Gemma User"
)

object SettingsStore {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(ctx: Context): AppSettings {
        val d = AppSettings()
        val p = prefs(ctx)
        return AppSettings(
            temperature = p.getFloat("temperature", d.temperature),
            maxTokens = p.getInt("maxTokens", d.maxTokens),
            systemPrompt = p.getString("systemPrompt", d.systemPrompt) ?: d.systemPrompt,
            userName = p.getString("userName", d.userName) ?: d.userName
        )
    }

    fun save(ctx: Context, s: AppSettings) {
        prefs(ctx).edit()
            .putFloat("temperature", s.temperature)
            .putInt("maxTokens", s.maxTokens)
            .putString("systemPrompt", s.systemPrompt)
            .putString("userName", s.userName)
            .apply()
    }
}

/** Saves all chats to one small JSON file inside the app's private storage. */
object ChatStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "chats.json")

    fun load(ctx: Context): List<Conversation> {
        return try {
            val f = file(ctx)
            if (!f.exists()) return emptyList()
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ms = o.getJSONArray("m")
                Conversation(
                    id = o.getLong("id"),
                    title = o.getString("t"),
                    updatedAt = o.getLong("u"),
                    messages = (0 until ms.length()).map { j ->
                        val m = ms.getJSONObject(j)
                        ChatMessage(j.toLong(), m.getString("x"), m.getBoolean("user"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, list: List<Conversation>) {
        try {
            val arr = JSONArray()
            for (c in list) {
                val ms = JSONArray()
                for (m in c.messages) {
                    ms.put(JSONObject().put("x", m.text).put("user", m.isUser))
                }
                arr.put(JSONObject().put("id", c.id).put("t", c.title).put("u", c.updatedAt).put("m", ms))
            }
            val tmp = File(ctx.filesDir, "chats.tmp")
            tmp.writeText(arr.toString())
            tmp.renameTo(file(ctx))
        } catch (_: Exception) {
        }
    }
}
