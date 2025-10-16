package app.source

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object BackendApi {
  private const val BACKEND_URL = "http://kotlin-backend:8080"

  private val client = OkHttpClient
    .Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  private val JSON = "application/json; charset=utf-8".toMediaType()

  fun post(
    path: String,
    body: String,
  ) {
    val requestBody = body.toRequestBody(JSON)

    val request = Request
      .Builder()
      .url("$BACKEND_URL/$path")
      .post(requestBody)
      .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        val errorBody = response.body?.string() ?: "No error body"
        error("HTTP ${response.code}: $errorBody")
      }
    }
  }
}
