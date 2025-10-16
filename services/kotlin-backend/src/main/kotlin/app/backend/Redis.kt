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

  private fun idMapKey(workflowToken: String) = "output_part_tokens:$workflowToken"

  fun add(
    workflowToken: String,
    part: OutputPart,
  ) {
    val key = redisKey(workflowToken)
    val redisId = sync.xadd(key, XAddArgs.Builder.maxlen(MAX_LENGTH).approximateTrimming(), part.toMap())
    sync.hset(idMapKey(workflowToken), part.token, redisId)
    sync.expire(key, TTL_SECONDS)
  }

  fun list(
    workflowToken: String,
    fromToken: String,
    count: Long,
  ): Pair<List<OutputPart>, String> {
    val startId = if (fromToken == "-") {
      "-"
    } else {
      sync.hget(idMapKey(workflowToken), fromToken) ?: return listOf<OutputPart>() to fromToken
    }

    val range = if (startId == "-") {
      Range.create("-", "+")
    } else {
      Range.create("($startId", "+")
    }

    val entries = sync.xrange(redisKey(workflowToken), range, Limit.from(count))
    val parts = entries.map { entry ->
      val message = entry.body
      OutputPart(
        token = message["token"].orEmpty(),
        ts = message["ts"]?.toLongOrNull() ?: 0L,
        data = message["data"].orEmpty(),
      )
    }

    val lastToken = if (entries.isEmpty()) {
      fromToken
    } else {
      parts.last().token
    }

    return parts to lastToken
  }
}
