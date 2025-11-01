package me.daegyeo.netflixchecker.scheduler

import me.daegyeo.netflixchecker.crawler.BankCrawler
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class BankScheduler(
    private val bankCrawler: BankCrawler,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(BankScheduler::class.java)
    private val KST_ZONE = ZoneId.of("Asia/Seoul")

    @Scheduled(cron = "0 0 */6 * * *")
    fun schedule() {
        val now = ZonedDateTime.now(KST_ZONE)

        if (isMaintenanceTime(now)) {
            logger.info("은행 점검 시간입니다. 크롤링 요청을 무시합니다. (23:30 ~ 01:00)")
            return
        }

        logger.info("은행 크롤링을 시작합니다.")

        bankCrawler.openBrowser()
        val result = bankCrawler.crawl()
        bankCrawler.closeBrowser()

        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(result))
    }

    private fun isMaintenanceTime(now: ZonedDateTime): Boolean {
        val currentTime = now.toLocalTime()
        val maintenanceStart = LocalTime.of(23, 30)
        val maintenanceEnd = LocalTime.of(1, 0)

        return currentTime.isAfter(maintenanceStart) ||
               currentTime.isBefore(maintenanceEnd) ||
               currentTime.equals(maintenanceStart) ||
               currentTime.equals(maintenanceEnd)
    }
}