package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.api.dto.DepositLogDTO
import me.daegyeo.netflixchecker.crawler.BankCrawler
import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.entity.DepositLog
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class DepositService(
    private val bankCrawler: BankCrawler,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun crawlBankManually() {
        bankCrawler.openBrowser()
        val result = bankCrawler.crawl()
        bankCrawler.closeBrowser()

        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(result))
    }

    fun getDepositors(): List<DepositLogDTO> {
        val data = transaction {
            DepositLog.all().map {
                DepositLogDTO(it.id.value, it.who, it.cost, it.date, it.costMonth)
            }
        }
        return data
    }

    fun addDepositor(data: AccountData) {
        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(listOf(data)))
    }
}