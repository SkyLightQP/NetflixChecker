package me.daegyeo.netflixchecker.scheduler

import me.daegyeo.netflixchecker.batch.BankCrawlJobRunner
import me.daegyeo.netflixchecker.batch.BankCrawlTrigger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BankCrawlScheduler(
    private val bankCrawlJobRunner: BankCrawlJobRunner
) {
    private val logger = LoggerFactory.getLogger(BankCrawlScheduler::class.java)

    @Scheduled(cron = "0 0 1/4 * * *", zone = "Asia/Seoul")
    fun schedule() {
        val result = bankCrawlJobRunner.run(BankCrawlTrigger.SCHEDULED)
        logger.info("예약된 은행 크롤링을 마쳤습니다. (result={})", result)
    }
}
