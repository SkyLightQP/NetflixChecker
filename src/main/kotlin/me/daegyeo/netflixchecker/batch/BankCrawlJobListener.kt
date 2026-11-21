package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.enum.MetricsKey
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import me.daegyeo.netflixchecker.event.OccurredCrawlErrorEvent
import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class BankCrawlJobListener(
    private val browserSession: BankBrowserSession,
    private val bankCrawlResult: BankCrawlResult,
    private val applicationEventPublisher: ApplicationEventPublisher
) : JobExecutionListener {
    companion object {
        private val KST_ZONE = ZoneId.of("Asia/Seoul")
        private val CRAWLING_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    private val logger = LoggerFactory.getLogger(BankCrawlJobListener::class.java)

    override fun beforeJob(jobExecution: JobExecution) {
        logger.info("은행 크롤링 Job을 시작합니다. (trigger={})", jobExecution.jobParameters.getString(BankCrawlJobConfiguration.TRIGGER))
        browserSession.open()
    }

    override fun afterJob(jobExecution: JobExecution) {
        browserSession.close()

        if (jobExecution.status == BatchStatus.COMPLETED) {
            recordCrawlSuccess()
            alertDeposits(bankCrawlResult.deposits)
            return
        }

        val failure = jobExecution.allFailureExceptions.firstOrNull()
        logger.error("은행 크롤링 Job이 실패했습니다. (status={})", jobExecution.status, failure)
        alertCrawlFailure(failure?.message ?: "")
        recordCrawlFailure()
    }

    private fun recordCrawlSuccess() {
        val now = ZonedDateTime.now(KST_ZONE).format(CRAWLING_TIME_FORMATTER)
        runCatching {
            transaction {
                upsertMetric(MetricsKey.LATEST_CRAWLING_STATUS, "O")
                upsertMetric(MetricsKey.LATEST_CRAWLING_TIME, now)
            }
        }.onFailure {
            logger.error("은행 크롤링 성공 Metrics 갱신 중 오류가 발생했습니다.", it)
        }
    }

    private fun alertDeposits(deposits: List<AccountData>) {
        runCatching {
            applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(deposits))
        }.onFailure {
            logger.error("은행 입금 알림 발송 중 오류가 발생했습니다.", it)
            alertCrawlFailure("입금 알림 발송 실패: ${it.message ?: ""}")
        }
    }

    private fun alertCrawlFailure(message: String) {
        runCatching {
            applicationEventPublisher.publishEvent(OccurredCrawlErrorEvent("Bank", message))
        }.onFailure {
            logger.error("은행 크롤링 오류 알림 발행 중 오류가 발생했습니다.", it)
        }
    }

    private fun recordCrawlFailure() {
        runCatching {
            transaction {
                upsertMetric(MetricsKey.LATEST_CRAWLING_STATUS, "X")
            }
        }.onFailure {
            logger.error("은행 크롤링 실패 Metrics 갱신 중 오류가 발생했습니다.", it)
        }
    }

    private fun upsertMetric(metricsKey: MetricsKey, metricValue: String) {
        Metrics.upsert(Metrics.key, where = { Metrics.key eq metricsKey.name }) {
            it[key] = metricsKey.name
            it[value] = metricValue
        }
    }
}
