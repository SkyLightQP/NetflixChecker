package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import me.daegyeo.netflixchecker.crawler.BankCrawler
import me.daegyeo.netflixchecker.event.CompletedBankCrawlEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CrawlCommand(
    private val bankCrawler: BankCrawler,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val discordConfiguration: DiscordConfiguration
) : Command {
    override val name: String = "crawl"

    override val description: String = "(관리자) 수동으로 입금 정보를 가져옵니다."

    override val options: List<CommandOption> = arrayListOf()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        if (event.interaction.user.id.asString() != discordConfiguration.admin) {
            return event.reply()
                .withEphemeral(true)
                .withContent("권한이 없습니다.")
                .then()
        }

        event.reply()
            .withEphemeral(true)
            .withContent("입금 정보를 수동으로 가져옵니다...")
            .block()

        bankCrawler.openBrowser()
        val result = bankCrawler.crawl()
        bankCrawler.closeBrowser()

        applicationEventPublisher.publishEvent(CompletedBankCrawlEvent(result))

        return event.editReply("성공적으로 입금 정보를 가져왔습니다!").then()
    }
}