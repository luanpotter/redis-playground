package app.backend

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE

fun main() {
  embeddedServer(Netty, port = 8080) {
    install(ContentNegotiation) { json() }
    install(SSE)
    routing { apiRoutes() }
  }.start(wait = true)
}
