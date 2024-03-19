package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import me.daegyeo.netflixchecker.crawler.CodeCrawler
import me.daegyeo.netflixchecker.util.EmbedUtil
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono


@Component
class CodeCommand(private val codeCrawler: CodeCrawler) : Command {
    override val name: String = "code"

    override val description: String = "최근 넷플릭스 인증코드를 확인합니다."

    override val options: List<CommandOption> = arrayListOf()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        event.reply()
            .withEphemeral(true)
            .withContent("인증코드를 가져오는 중...")
            .block()

        val result = codeCrawler.getVerificationCode()

        if (result.text == "" && result.link == "") {
            return event.editReply("최근 발송된 인증코드가 없습니다.").then()
        }

        val embed = EmbedUtil.create(
            "넷플릭스 인증코드", """
            ${result.text}
            **${result.link}**
        """.trimIndent()
        )

        return event.editReply("").withEmbeds(embed).then()
    }
}