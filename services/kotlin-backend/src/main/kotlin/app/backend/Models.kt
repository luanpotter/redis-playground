package app.backend

import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkflowResponse(
  val workflowToken: String,
)

@Serializable
data class OutputPart(
  val token: String,
  val ts: Long,
  val data: String,
) {
  fun toMap(): Map<String, String> = mapOf(
    "token" to token,
    "ts" to ts.toString(),
    "data" to data,
  )
}

@Serializable
data class OutputPartsResponse(
  val parts: List<OutputPart>,
  val fromToken: String,
  val lastToken: String,
)
