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

  private fun idToKey(workflowToken: String, id: String): String? {
    val key = sync.hget(idMapKey(workflowToken), id) ?: return null
    return "($key"
  }

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
    fromId: String?,
    count: Long,
  ): Pair<List<SseEvent>, String?> {
    val fromKey = fromId
      ?.let { idToKey(workflowToken, it) }
      ?: "-"

    val range = Range.create(fromKey, "+")

    val entries = sync.xrange(redisKey(workflowToken), range, Limit.from(count))
    val events = entries.map { entry ->
      val message = entry.body
      SseEvent(
        id = message["id"].orEmpty(),
        event = message["event"].orEmpty(),
        data = message["data"].orEmpty(),
      )
    }

    val lastId = events.lastOrNull()?.id
      ?: fromId

    return events to lastId
  }
}
