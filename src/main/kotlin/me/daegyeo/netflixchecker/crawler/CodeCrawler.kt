package me.daegyeo.netflixchecker.crawler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.config.Pop3Configuration
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import me.daegyeo.netflixchecker.enum.MetricsKey
import me.daegyeo.netflixchecker.shared.retry.RetryExecutor
import me.daegyeo.netflixchecker.shared.retry.RetryPolicy
import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.mail.*
import javax.mail.internet.MimeBodyPart


data class VerificationCode(val text: String, val link: String)

private class VerificationCodeNotFoundException : RuntimeException("Verification code not found")

@Component
class CodeCrawler(
    private val pop3Configuration: Pop3Configuration,
    private val coroutineScope: CoroutineScope,
    private val featureFlagConfiguration: FeatureFlagConfiguration
) {
    private val logger = LoggerFactory.getLogger(CodeCrawler::class.java)

    private val EMAIL_SUBJECT_TYPE_A = "회원님의 넷플릭스 임시 접속 코드"
    private val EMAIL_SUBJECT_TYPE_B = "넷플릭스: 로그인 코드"
    private val EMAIL_FROM = "Netflix <info@account.netflix.com>"
    private val verificationCodeCrawlers = mapOf(
        EMAIL_SUBJECT_TYPE_A to ::crawlVerificationCodeByTypeA,
        EMAIL_SUBJECT_TYPE_B to ::crawlVerificationCodeByTypeB
    )

    private fun createMailClient(): Store {
        val properties = Properties()
        properties["mail.pop3.host"] = pop3Configuration.host
        properties["mail.pop3.port"] = pop3Configuration.port
        properties["mail.pop3.starttls.enable"] = "true"
        properties["mail.pop3.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        val emailSession = Session.getDefaultInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(pop3Configuration.user, pop3Configuration.password)
            }
        })
        val store = emailSession.getStore("pop3")
        return store
    }

    fun getVerificationCode(): VerificationCode {
        val retryPolicy = RetryPolicy(
            maxAttempts = featureFlagConfiguration.getInt(FeatureFlagKey.MAX_RETRY_COUNT).coerceAtLeast(1),
            initialDelayMillis = 750,
            multiplier = 2.0,
            maxDelayMillis = 5_000,
            jitterMillis = 250,
            shouldRetry = ::isRetryableException,
            onRetry = {
                logger.warn(
                    "인증코드 조회 재시도({}/{}) - nextDelay={}ms, operation={}, exception={}({})",
                    it.attempt,
                    it.maxAttempts,
                    it.nextDelayMillis,
                    it.operationName,
                    it.throwable.javaClass.simpleName,
                    it.throwable.message ?: ""
                )
            }
        )

        return try {
            RetryExecutor.run(
                policy = retryPolicy,
                operationName = "code-crawler.getVerificationCode"
            ) {
                getVerificationCodeOnce()
            }
        } catch (e: VerificationCodeNotFoundException) {
            logger.info("재시도 이후에도 최근 발송된 인증코드를 찾지 못했습니다.")
            VerificationCode("", "")
        }
    }

    private fun getVerificationCodeOnce(): VerificationCode {
        val store = createMailClient()
        var inbox: Folder? = null

        try {
            store.connect(pop3Configuration.host, pop3Configuration.user, pop3Configuration.password)
            inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val messages = inbox.messages

            for (i in messages.size - 1 downTo (messages.size - 10).coerceAtLeast(0)) {
                val result = crawlVerificationCode(messages[i]) ?: continue
                recordMetricsAsyncSafely()
                return result
            }

            throw VerificationCodeNotFoundException()
        } finally {
            closeFolder(inbox)
            closeStore(store)
        }
    }

    private fun isRetryableException(throwable: Throwable): Boolean {
        if (throwable is VerificationCodeNotFoundException) {
            return true
        }
        if (throwable is AuthenticationFailedException) {
            return false
        }
        if (throwable is MessagingException || throwable is IOException) {
            return true
        }

        val cause = throwable.cause ?: return false
        return isRetryableException(cause)
    }

    private fun crawlVerificationCode(message: Message): VerificationCode? {
        val crawler = verificationCodeCrawlers[message.subject] ?: return null
        val content = message.content

        if (message.from.firstOrNull()?.toString() != EMAIL_FROM) {
            return null
        }
        if (content !is Multipart) {
            return null
        }

        val convertedContent = convertMultipartToString(content)
        val html = Jsoup.parse(convertedContent).body()

        return crawler(html)
    }

    private fun crawlVerificationCodeByTypeA(html: Element): VerificationCode {
        val text =
            html.selectXpath("table/tbody/tr/td/table/tbody/tr[2]/td/table[5]/tbody/tr/td/table/tbody/tr/td/table[1]/tbody/tr[2]/td[3]/table/tbody/tr/td")
                .text()
        val link =
            html.selectXpath("table/tbody/tr/td/table/tbody/tr[2]/td/table[5]/tbody/tr/td/table/tbody/tr/td/table[2]/tbody/tr/td/table/tbody/tr/td/a")
                .attr("href")

        logger.info("넷플릭스 인증코드(Type A)를 발견했습니다.")

        return VerificationCode(text, link)
    }

    private fun crawlVerificationCodeByTypeB(html: Element): VerificationCode {
        val code = html.selectXpath("table/tbody/tr/td/table/tbody/tr[2]/td/table[2]/tbody/tr/td").text()
        val text = "로그인 코드를 확인하세요. 코드는 15분 뒤 만료됩니다."

        logger.info("넷플릭스 인증코드(Type B)를 발견했습니다.")

        return VerificationCode(text, code)
    }

    private fun recordMetricsAsyncSafely() {
        runCatching {
            coroutineScope.launch {
                runCatching {
                    transaction {
                        Metrics.upsert(
                            Metrics.key,
                            onUpdate = listOf(Metrics.value to Metrics.value.castTo(IntegerColumnType()).plus(1)),
                            where = { Metrics.key eq MetricsKey.CODE_GENERATED_COUNT.name }
                        ) {
                            it[key] = MetricsKey.CODE_GENERATED_COUNT.name
                            it[value] = "1"
                        }

                        val now = Instant.now().atZone(ZoneId.of("Asia/Seoul"))
                        val year = now.year.toString()
                        val month = now.monthValue.toString().padStart(2, '0')
                        val thisMonthKey = MetricsKey.CODE_GENERATED_COUNT_BY_MONTH.key.replace("{YEAR}", year)
                            .replace("{MONTH}", month)
                        Metrics.upsert(
                            Metrics.key,
                            onUpdate = listOf(Metrics.value to Metrics.value.castTo(IntegerColumnType()).plus(1)),
                            where = { Metrics.key eq thisMonthKey }
                        ) {
                            it[key] = thisMonthKey
                            it[value] = "1"
                        }
                    }
                }.onFailure {
                    logger.error("인증코드 metrics 기록 중 오류가 발생했습니다.", it)
                }
            }
        }.onFailure {
            logger.error("인증코드 metrics 작업 시작 중 오류가 발생했습니다.", it)
        }
    }

    private fun closeFolder(folder: Folder?) {
        if (folder?.isOpen != true) {
            return
        }

        runCatching {
            folder.close(false)
        }.onFailure {
            logger.warn("메일함을 닫는 중 오류가 발생했습니다.", it)
        }
    }

    private fun closeStore(store: Store) {
        if (!store.isConnected) {
            return
        }

        runCatching {
            store.close()
        }.onFailure {
            logger.warn("메일 스토어를 닫는 중 오류가 발생했습니다.", it)
        }
    }

    private fun convertMultipartToString(data: Multipart): String {
        var content = ""
        for (i in 0 until data.count) {
            val bp = data.getBodyPart(i) as MimeBodyPart
            if (bp.contentType.contains("text/html")) {
                content = bp.content.toString()
            }
        }
        return content
    }
}
