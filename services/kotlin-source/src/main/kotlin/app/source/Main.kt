package app.source

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8081) {
    install(ContentNegotiation) { json() }
    routing {
      post("/stream/{workflowToken}/start") {
        val workflowToken = call.parameters["workflowToken"]!!
        StreamGenerator.start(workflowToken)
        call.respondText("started")
      }
    }
  }.start(wait = true)
}
