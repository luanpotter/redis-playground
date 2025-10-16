package app.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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
    val part = call.receive<OutputPart>()

    println("Ingesting $workflowToken - part: $part")
    Redis.add(workflowToken, part)

    call.respond(HttpStatusCode.Accepted)
  }

  get("/workflows/{workflowToken}/output") {
    val workflowToken = call.parameters["workflowToken"]!!
    val fromToken = call.request.queryParameters["fromToken"] ?: "-"
    val count = call.request.queryParameters["count"]?.toLongOrNull() ?: 200

    val (parts, lastToken) = Redis.list(
      workflowToken = workflowToken,
      fromToken = fromToken,
      count = count,
    )

    val response = OutputPartsResponse(
      parts = parts,
      fromToken = fromToken,
      lastToken = lastToken,
    )
    call.respond(response)
  }
}
