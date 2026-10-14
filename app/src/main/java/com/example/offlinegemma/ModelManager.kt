package com.example.offlinegemma

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Handles getting the model file into the app's private storage. */
object ModelManager {

    // Gemma 3 1B instruction-tuned, 4-bit, MediaPipe format (~550 MB).
    // Gated on Hugging Face: accept the license + use a read token.
    const val DEFAULT_URL =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"

    private const val MIN_VALID_BYTES = 50L * 1024 * 1024

    fun modelFile(ctx: Context) = File(ctx.filesDir, "model.task")

    /** Copy a file the user picked (content:// Uri) into private storage. */
    fun importFromUri(ctx: Context, uri: Uri, onProgress: (Float) -> Unit) {
        val total = ctx.contentResolver
            .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L } ?: -1L
        val tmp = File(ctx.filesDir, "model.tmp")
        val input = ctx.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected file")
        input.use { i -> tmp.outputStream().use { o -> copy(i, o, total, onProgress) } }
        finish(ctx, tmp)
    }

    /** Download the model with progress. Redirects are followed by hand so the
     *  token is only sent to the first host (Hugging Face), not the CDN. */
    fun download(ctx: Context, url: String, token: String, onProgress: (Float) -> Unit) {
        var target = URL(url)
        var hops = 0
        var conn: HttpURLConnection
        while (true) {
            conn = target.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (hops == 0 && token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location") ?: throw IOException("Bad redirect")
                conn.disconnect()
                target = URL(target, loc)
                if (++hops > 5) throw IOException("Too many redirects")
                continue
            }
            if (code == 401 || code == 403) {
                throw IOException(
                    "Access denied (HTTP $code). Accept the Gemma license on Hugging Face " +
                        "and paste a read token."
                )
            }
            if (code != 200) throw IOException("Download failed (HTTP $code)")
            break
        }
        val tmp = File(ctx.filesDir, "model.tmp")
        val total = conn.contentLengthLong
        try {
            conn.inputStream.use { i -> tmp.outputStream().use { o -> copy(i, o, total, onProgress) } }
        } finally {
            conn.disconnect()
        }
        finish(ctx, tmp)
    }

    private fun copy(input: InputStream, out: OutputStream, total: Long, onProgress: (Float) -> Unit) {
        val buf = ByteArray(1024 * 1024)
        var done = 0L
        var lastPct = -1
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            done += n
            if (total > 0) {
                val pct = (done * 100 / total).toInt()
                if (pct != lastPct) {
                    lastPct = pct
                    onProgress(done.toFloat() / total)
                }
            }
        }
    }

    private fun finish(ctx: Context, tmp: File) {
        if (tmp.length() < MIN_VALID_BYTES) {
            tmp.delete()
            throw IOException("File is too small to be a model. Download may have failed.")
        }
        val dest = modelFile(ctx)
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) throw IOException("Could not save the model file")
    }
}
