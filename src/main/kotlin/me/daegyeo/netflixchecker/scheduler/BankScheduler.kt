package me.daegyeo.netflixchecker.scheduler

import io.sentry.spring.jakarta.checkin.SentryCheckIn
import me.daegyeo.netflixchecker.crawler.BankCrawler
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BankScheduler(
    private val bankCrawler: BankCrawler,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(BankScheduler::class.java)
    private val BANK_MAINTENANCE_START = LocalDateTime.now().withHour(23).withMinute(0).withSecond(0)
    private val BANK_MAINTENANCE_END_HOUR = LocalDateTime.now().withHour(1).withMinute(0).withSecond(0)

    @Scheduled(cron = "0 0 */6 * * *")
    @SentryCheckIn("Bank crawl scheduler")
    fun schedule() {
        val now = LocalDateTime.now()
        if (now.isAfter(BANK_MAINTENANCE_START) && now.isBefore(BANK_MAINTENANCE_END_HOUR)) {
            logger.info("은행 점검 시간입니다. 크롤링을 하지 않습니다.")
            return
        }

        logger.info("은행 크롤링을 시작합니다.")

        bankCrawler.openBrowser()
        val result = bankCrawler.crawl()
        bankCrawler.closeBrowser()

        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(result))
    }
}