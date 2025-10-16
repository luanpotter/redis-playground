package app.source

import kotlinx.serialization.Serializable

@Serializable
data class SseEvent(
  val id: String,
  val event: String,
  val data: String,
)
