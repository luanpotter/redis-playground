package app.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.random.Random

object StreamGenerator {
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val workflows = mutableMapOf<String, Job>()

  fun start(workflowToken: String) {
    if (workflows[workflowToken]?.isActive == true) {
      return
    }

    fun send(
      event: String,
      data: String,
    ) {
      val sseEvent = SseEvent(
        id = UUID.randomUUID().toString(),
        event = event,
        data = data,
      )
      println("Sending event: $sseEvent")
      BackendApi.post("ingest/$workflowToken", Json.encodeToString(sseEvent))
    }

    workflows[workflowToken] = scope.launch {
      var nextSeq = 1
      while (isActive) {
        send("output", "seq[${nextSeq++}]")
        if (Random.nextDouble() < 0.05) {
          send("end", "workflow completed")

          stop(workflowToken)
          break // finish processing
        }
        delay(Random.nextLong(100, 500))
      }
    }
  }

  private fun stop(workflowToken: String) {
    workflows.remove(workflowToken)?.cancel()
  }
}
