package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.api.dto.DepositLogDTO
import me.daegyeo.netflixchecker.batch.BankCrawlJobRunner
import me.daegyeo.netflixchecker.batch.BankCrawlTrigger
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
    private val bankCrawlJobRunner: BankCrawlJobRunner,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(DepositService::class.java)

    fun crawlBankManually() {
        logger.info("API를 통해 크롤링을 시작합니다.")

        val result = bankCrawlJobRunner.run(BankCrawlTrigger.API)
        logger.info("API를 통한 크롤링을 마쳤습니다. (result={})", result)
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
        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(listOf(data), true))
        logger.info("새로운 입금 내역을 수동으로 추가했습니다.")
    }
}