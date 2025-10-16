package app.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import kotlinx.coroutines.delay
import java.util.UUID

fun Route.apiRoutes() {
  post("/workflows") {
    val workflowToken = UUID.randomUUID().toString()
    call.respond(CreateWorkflowResponse(workflowToken))
  }

  post("/workflows/{workflowToken}/start") {
    val workflowToken = call.parameters["workflowToken"]!!

    SourceApi.post("stream/$workflowToken/start")

    call.respond(HttpStatusCode.Accepted)
  }

  post("/ingest/{workflowToken}") {
    val workflowToken = call.parameters["workflowToken"]!!
    val event = call.receive<SseEvent>()

    println("Ingesting event for workflow $workflowToken: $event")
    Redis.add(workflowToken, event)

    call.respond(HttpStatusCode.Accepted)
  }

  sse("/workflows/{workflowToken}/stream") {
    val workflowToken = call.parameters["workflowToken"]!!
    val resumeFromId = call.request.queryParameters["lastEventId"]
    var lastId: String? = resumeFromId

    while (true) {
      val (events, newLastId) = Redis.list(
        workflowToken = workflowToken,
        fromId = lastId,
        count = 200,
      )

      events.forEach { event ->
        send(
          data = event.data,
          event = event.event,
          id = event.id,
        )
      }

      // Always update lastId when we get events
      if (events.isNotEmpty()) {
        lastId = newLastId
      }

      // Check if workflow ended
      if (events.any { it.event == "end" }) {
        break
      }

      delay(100)
    }
  }
}
