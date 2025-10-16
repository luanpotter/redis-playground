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

    fun send(data: String) {
      val part = OutputPart(
        token = UUID.randomUUID().toString(),
        ts = System.currentTimeMillis(),
        data = data,
      )
      println("Sending $workflowToken - part: $part")
      BackendApi.post("ingest/$workflowToken", Json.encodeToString(part))
    }

    workflows[workflowToken] = scope.launch {
      var nextSeq = 1
      while (isActive) {
        send("seq[${nextSeq++}]")
        if (Random.nextDouble() < 0.05) {
          send("end")

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
