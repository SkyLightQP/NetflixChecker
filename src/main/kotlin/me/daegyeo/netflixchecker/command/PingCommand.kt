package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PingCommand : ICommand {
    override val name: String = "ping"

    override val description: String = "Ping the bot."

    override val options: List<CommandOption> = arrayListOf()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        return event.reply()
            .withEphemeral(true)
            .withContent("Pong!")
            .then()
    }
}