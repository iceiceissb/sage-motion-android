package cn.tsinghua.sagemotion.data.agent

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import cn.tsinghua.sagemotion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Called only after the user explicitly approves this photo for image generation. */
class RemoteZineApi(private val context: Context) {
    suspend fun generate(photoUri: String, caption: String): File = withContext(Dispatchers.IO) {
        val base = BuildConfig.SAGE_AGENT_BACKEND_URL.trim().trimEnd('/')
        require(base.startsWith("https://") || (BuildConfig.DEBUG && base.startsWith("http://"))) { "请先配置云端服务" }
        val encoded = VisionImagePayloadEncoder(context).encodeDataUrl(photoUri)
            ?: throw IOException("照片无法读取，请重新拍摄")
        val connection = (URL("$base/v1/journey/zine").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            instanceFollowRedirects = false
            connectTimeout = 8_000
            readTimeout = 190_000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (BuildConfig.SAGE_AGENT_CLIENT_TOKEN.isNotBlank()) setRequestProperty("Authorization", "Bearer ${BuildConfig.SAGE_AGENT_CLIENT_TOKEN}")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(JSONObject().put("image_data_url", encoded).put("caption", caption.take(500)).toString())
            }
            when (connection.responseCode) {
                in 200..299 -> Unit
                401, 403 -> throw IOException("云端授权无效，请检查客户端令牌")
                503 -> throw IOException("服务端尚未启用纸刊生成")
                429 -> throw IOException("请求过于频繁，请稍后再试")
                else -> throw IOException("云端生成暂不可用，请稍后重试")
            }
            val response = connection.inputStream.use { it.readBytesBounded(25_000_000) }
            val json = JSONObject(String(response, Charsets.UTF_8))
            val bytes = Base64.decode(json.getString("image_base64"), Base64.DEFAULT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096 && bounds.outMimeType == "image/png") { "生成图片格式无效" }
            val dir = File(context.cacheDir, "journey_shares").apply { mkdirs() }
            File.createTempFile("zine_", ".png", dir).apply { writeBytes(bytes) }
        } finally { connection.disconnect() }
    }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        require(output.size() + read <= limit) { "生成图片过大" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
