package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.api.dto.DepositLogDTO
import me.daegyeo.netflixchecker.crawler.BankCrawler
import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.entity.DepositLog
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import me.daegyeo.netflixchecker.table.DepositLogs
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class DepositService(
    private val bankCrawler: BankCrawler,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(DepositService::class.java)

    fun crawlBankManually() {
        logger.info("API를 통해 크롤링을 시작합니다.")

        bankCrawler.openBrowser()
        val result = bankCrawler.crawl()
        bankCrawler.closeBrowser()

        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(result))
    }

    fun getDepositors(): List<DepositLogDTO> {
        val data = transaction {
            DepositLog.all().orderBy(DepositLogs.date to SortOrder.DESC).map {
                DepositLogDTO(it.id.value, it.who, it.cost, it.date, it.costMonth)
            }
        }
        return data
    }

    fun addDepositor(data: AccountData) {
        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(listOf(data)))
        logger.info("새로운 입금 내역을 수동으로 추가했습니다.")
    }
}