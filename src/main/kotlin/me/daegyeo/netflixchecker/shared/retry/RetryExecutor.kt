package me.daegyeo.netflixchecker.shared.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

data class RetryAttemptContext(
    val operationName: String,
    val attempt: Int,
    val maxAttempts: Int,
    val nextDelayMillis: Long,
    val throwable: Throwable,
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 500,
    val multiplier: Double = 2.0,
    val maxDelayMillis: Long = 5_000,
    val jitterMillis: Long = 0,
    val shouldRetry: (Throwable) -> Boolean = { true },
    val onRetry: ((RetryAttemptContext) -> Unit)? = null,
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than 0" }
        require(initialDelayMillis >= 0) { "initialDelayMillis must be greater than or equal to 0" }
        require(multiplier >= 1.0) { "multiplier must be greater than or equal to 1.0" }
        require(maxDelayMillis >= 0) { "maxDelayMillis must be greater than or equal to 0" }
        require(jitterMillis >= 0) { "jitterMillis must be greater than or equal to 0" }
    }
}

object RetryExecutor {
    fun <T> run(
        policy: RetryPolicy,
        operationName: String,
        block: () -> T,
    ): T {
        for (attempt in 1..policy.maxAttempts) {
            try {
                return block()
            } catch (throwable: Exception) {
                if (throwable is InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw throwable
                }

                if (!shouldRetry(policy, attempt, throwable)) {
                    throw throwable
                }

                val nextDelayMillis = calculateDelayMillis(policy, attempt)
                policy.onRetry?.invoke(
                    RetryAttemptContext(
                        operationName = operationName,
                        attempt = attempt,
                        maxAttempts = policy.maxAttempts,
                        nextDelayMillis = nextDelayMillis,
                        throwable = throwable,
                    )
                )

                if (nextDelayMillis > 0) {
                    sleep(nextDelayMillis)
                }
            }
        }

        error("RetryExecutor reached an impossible state")
    }

    suspend fun <T> runSuspend(
        policy: RetryPolicy,
        operationName: String,
        block: suspend () -> T,
    ): T {
        for (attempt in 1..policy.maxAttempts) {
            try {
                return block()
            } catch (throwable: Exception) {
                if (throwable is CancellationException) {
                    throw throwable
                }

                if (throwable is InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw throwable
                }

                if (!shouldRetry(policy, attempt, throwable)) {
                    throw throwable
                }

                val nextDelayMillis = calculateDelayMillis(policy, attempt)
                policy.onRetry?.invoke(
                    RetryAttemptContext(
                        operationName = operationName,
                        attempt = attempt,
                        maxAttempts = policy.maxAttempts,
                        nextDelayMillis = nextDelayMillis,
                        throwable = throwable,
                    )
                )

                if (nextDelayMillis > 0) {
                    delay(nextDelayMillis)
                }
            }
        }

        error("RetryExecutor reached an impossible state")
    }

    private fun shouldRetry(policy: RetryPolicy, attempt: Int, throwable: Throwable): Boolean {
        val isLastAttempt = attempt >= policy.maxAttempts
        return !isLastAttempt && policy.shouldRetry(throwable)
    }

    private fun calculateDelayMillis(policy: RetryPolicy, attempt: Int): Long {
        val exponentialDelay = policy.initialDelayMillis.toDouble() * policy.multiplier.pow((attempt - 1).toDouble())
        val cappedDelay = min(exponentialDelay, policy.maxDelayMillis.toDouble()).toLong()
        val jitterRange = policy.jitterMillis

        if (jitterRange == 0L) {
            return cappedDelay
        }

        val jitter = Random.nextLong(-jitterRange, jitterRange + 1)
        val jitteredDelay = cappedDelay.toDouble() + jitter.toDouble()
        return jitteredDelay.coerceIn(0.0, policy.maxDelayMillis.toDouble()).toLong()
    }

    private fun sleep(delayMillis: Long) {
        try {
            Thread.sleep(delayMillis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        }
    }
}
