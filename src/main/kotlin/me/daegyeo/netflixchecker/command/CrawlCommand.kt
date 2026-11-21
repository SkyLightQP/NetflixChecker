package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import me.daegyeo.netflixchecker.batch.BankCrawlJobRunner
import me.daegyeo.netflixchecker.batch.BankCrawlRunResult
import me.daegyeo.netflixchecker.batch.BankCrawlTrigger
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CrawlCommand(
    private val bankCrawlJobRunner: BankCrawlJobRunner,
    private val featureFlagConfiguration: FeatureFlagConfiguration,
) : Command {
    override val name: String = "크롤링"

    override val description: String = "(관리자) 수동으로 입금 정보를 가져옵니다."

    override val options: List<CommandOption> = arrayListOf()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        if (event.interaction.user.id.asString() != featureFlagConfiguration.getString(FeatureFlagKey.DISCORD_ADMIN_ID)) {
            return event.reply()
                .withEphemeral(true)
                .withContent("권한이 없습니다.")
                .then()
        }

        event.reply()
            .withEphemeral(true)
            .withContent("입금 정보를 수동으로 가져옵니다...")
            .block()

        val replyMessage = when (val result = bankCrawlJobRunner.run(BankCrawlTrigger.DISCORD)) {
            is BankCrawlRunResult.Completed ->
                if (result.depositCount > 0) "성공적으로 입금 정보를 가져왔습니다!" else "입금 정보가 없습니다."
            BankCrawlRunResult.Failed -> "크롤링을 실패했습니다."
            BankCrawlRunResult.Maintenance -> "은행 점검 시간(23:30 ~ 01:00)에는 크롤링할 수 없습니다."
            BankCrawlRunResult.AlreadyRunning, BankCrawlRunResult.DuplicateRun -> "이미 크롤링이 실행 중입니다."
        }

        return event.editReply(replyMessage).then()
    }
}
