package app.backend

import io.lettuce.core.Limit
import io.lettuce.core.Range
import io.lettuce.core.RedisClient
import io.lettuce.core.XAddArgs
import io.lettuce.core.api.sync.RedisCommands

object Redis {
  private val client = RedisClient.create("redis://${System.getenv("REDIS_HOST")}:${System.getenv("REDIS_PORT")}")
  private val conn = client.connect()
  private val sync: RedisCommands<String, String> = conn.sync()

  private const val MAX_LENGTH = 5000L
  private const val TTL_SECONDS = 3600L

  private fun redisKey(workflowToken: String) = "workflow_events:$workflowToken"

  private fun idMapKey(workflowToken: String) = "event_ids:$workflowToken"

  fun add(
    workflowToken: String,
    event: SseEvent,
  ) {
    val key = redisKey(workflowToken)
    val redisId = sync.xadd(key, XAddArgs.Builder.maxlen(MAX_LENGTH).approximateTrimming(), event.toMap())
    sync.hset(idMapKey(workflowToken), event.id, redisId)
    sync.expire(key, TTL_SECONDS)
  }

  fun list(
    workflowToken: String,
    fromId: String,
    count: Long,
  ): Pair<List<SseEvent>, String> {
    val startId = if (fromId == "-") {
      "-"
    } else {
      sync.hget(idMapKey(workflowToken), fromId) ?: return listOf<SseEvent>() to fromId
    }

    val range = if (startId == "-") {
      Range.create("-", "+")
    } else {
      Range.create("($startId", "+")
    }

    val entries = sync.xrange(redisKey(workflowToken), range, Limit.from(count))
    val events = entries.map { entry ->
      val message = entry.body
      SseEvent(
        id = message["id"].orEmpty(),
        event = message["event"].orEmpty(),
        data = message["data"].orEmpty(),
      )
    }

    val lastId = if (events.isEmpty()) {
      fromId
    } else {
      events.last().id
    }

    return events to lastId
  }
}
