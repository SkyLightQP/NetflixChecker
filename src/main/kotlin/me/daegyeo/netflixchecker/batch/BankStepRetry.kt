package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

@Component
class BankStepRetry(
    private val featureFlagConfiguration: FeatureFlagConfiguration
) {
    companion object {
        private const val INITIAL_INTERVAL_MILLIS = 2_000L
        private const val MULTIPLIER = 2.0
        private const val MAX_INTERVAL_MILLIS = 20_000L

        private val RETRYABLE_EXCEPTIONS: Map<Class<out Throwable>, Boolean> = mapOf(
            InterruptedException::class.java to false,
            TimeoutException::class.java to true,
            WebDriverException::class.java to true,
            SocketTimeoutException::class.java to true,
            ConnectException::class.java to true,
            IOException::class.java to true,
        )
    }

    private val logger = LoggerFactory.getLogger(BankStepRetry::class.java)

    fun <T> run(stepName: String, action: (isRetry: Boolean) -> T): T {
        val maxAttempts = featureFlagConfiguration.getInt(FeatureFlagKey.MAX_RETRY_COUNT).coerceAtLeast(1)
        val retryTemplate = RetryTemplate().apply {
            setRetryPolicy(SimpleRetryPolicy(maxAttempts, RETRYABLE_EXCEPTIONS, true))
            setBackOffPolicy(ExponentialRandomBackOffPolicy().apply {
                initialInterval = INITIAL_INTERVAL_MILLIS
                multiplier = MULTIPLIER
                maxInterval = MAX_INTERVAL_MILLIS
            })
            registerListener(retryLogListener(stepName, maxAttempts))
        }

        return retryTemplate.execute<T, Exception> { context -> action(context.retryCount > 0) }
    }

    private fun retryLogListener(stepName: String, maxAttempts: Int) = object : RetryListener {
        override fun <T, E : Throwable> onError(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable) {
            if (throwable is InterruptedException) {
                Thread.currentThread().interrupt()
            }

            logger.warn(
                "은행 크롤링 Step 재시도({}/{}) - step={}, exception={}({})",
                context.retryCount,
                maxAttempts,
                stepName,
                throwable.javaClass.simpleName,
                throwable.message ?: ""
            )
        }
    }
}
