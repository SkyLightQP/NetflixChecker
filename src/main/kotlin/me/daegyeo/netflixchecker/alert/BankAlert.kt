package me.daegyeo.netflixchecker.alert

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import me.daegyeo.netflixchecker.entity.DepositLog
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import me.daegyeo.netflixchecker.table.DepositLogs
import me.daegyeo.netflixchecker.util.EmbedUtil
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


@Component
class BankAlert(
    private val gatewayDiscordClient: GatewayDiscordClient, private val discordConfiguration: DiscordConfiguration
) {
    private val logger = LoggerFactory.getLogger(BankAlert::class.java)

    @EventListener
    fun sendDepositLogAlert(event: CompletedBankCrawlEvent) {
        val channel = gatewayDiscordClient.getChannelById(Snowflake.of(discordConfiguration.channel))
            .ofType(GuildMessageChannel::class.java).block()!!

        transaction {
            var count = 0
            event.data.forEach {
                val log = DepositLog.find {
                    (DepositLogs.date eq it.date) and (DepositLogs.who eq it.who)
                }.firstOrNull()

                if (log == null) {
                    val embed = EmbedUtil.create(
                        title = "넷플릭스 입금 확인",
                        description = "(${it.who}) ${it.costMonth}달 치 확인 완료",
                        date = convertStringToInstant(it.date)
                    )
                    channel.createMessage(embed).block()

                    DepositLog.new {
                        who = it.who
                        cost = it.cost.toInt()
                        date = it.date
                    }

                    count++
                }
            }
            logger.info("새로운 입금 알림 ${count}건을 발송하였습니다.")
        }
    }

    private fun convertStringToInstant(date: String): Instant {
        val pattern = "yyyy-MM-dd"
        val dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.KOREA)
        val localDate = LocalDate.parse(date, dateTimeFormatter)
        val zoneId = ZoneId.of("Asia/Seoul")
        return localDate.atStartOfDay(zoneId).toInstant()
    }
}