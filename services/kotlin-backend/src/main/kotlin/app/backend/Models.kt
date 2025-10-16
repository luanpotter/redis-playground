package app.backend

import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkflowResponse(
  val workflowToken: String,
)

@Serializable
data class SseEvent(
  val id: String,
  val event: String,
  val data: String,
) {
  fun toMap(): Map<String, String> = mapOf(
    "id" to id,
    "event" to event,
    "data" to data,
  )

  fun toSseFormat(): String = "id: $id\nevent: $event\ndata: $data\n\n"
}
