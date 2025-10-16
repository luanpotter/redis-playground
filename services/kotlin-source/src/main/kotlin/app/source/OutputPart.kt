package app.source

import kotlinx.serialization.Serializable

@Serializable
data class OutputPart(
  val token: String,
  val ts: Long,
  val data: String,
)
