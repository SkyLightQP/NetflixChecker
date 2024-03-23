package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PingCommand : Command {
    override val name: String = "핑"

    override val description: String = "봇의 작동 상태를 확인합니다."

    override val options: List<CommandOption> = arrayListOf()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        return event.reply()
            .withEphemeral(false)
            .withContent("Pong!")
            .then()
    }
}